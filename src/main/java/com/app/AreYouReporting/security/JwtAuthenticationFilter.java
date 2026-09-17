package com.app.AreYouReporting.security;

import com.app.AreYouReporting.Entities.DataScopeType;
import com.app.AreYouReporting.Entities.UserRoleAssignment;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Claims claims = tokenProvider.getClaims(jwt);
                UUID userId = tokenProvider.getUserIdFromToken(jwt);
                Optional<UserPrincipal> userPrincipalOpt = Optional.empty();

                if (userId != null) {
                    userPrincipalOpt = customUserDetailsService.findUserPrincipalById(userId);
                }

                // Fallback: If not matched by UUID, attempt lookup by signed username claim
                if (userPrincipalOpt.isEmpty() && claims != null && claims.get("username") != null) {
                    String usernameClaim = claims.get("username").toString();
                    userPrincipalOpt = customUserDetailsService.findUserPrincipalByUsername(usernameClaim);
                }

                if (userPrincipalOpt.isPresent()) {
                    UserPrincipal userPrincipal = userPrincipalOpt.get();

                    if (userPrincipal.isEnabled()) {
                        // Check active context from headers or token claims
                        UUID activeRoleId = getUuidFromHeaderOrClaims(request, "X-Active-Role", claims, "activeRoleId");
                        UUID activeDeptId = getUuidFromHeaderOrClaims(request, "X-Active-Department-Id", claims, "activeDepartmentId");
                        UUID activeSubDeptId = getUuidFromHeaderOrClaims(request, "X-Active-SubDepartment-Id", claims, "activeSubDepartmentId");

                        // Resolve matching assignment
                        ActiveUserContext activeContext = buildActiveContext(userPrincipal, activeRoleId, activeDeptId, activeSubDeptId);
                        request.setAttribute(ScopeAuthorizationService.CONTEXT_ATTRIBUTE, activeContext);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        log.warn("Authentication failed: User account is inactive for user: {}", userPrincipal.getUsername());
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    log.warn("Authentication failed: User not found in active database for token subject ID: {}", userId);
                    SecurityContextHolder.clearContext();
                }
            }
        } catch (Exception ex) {
            log.warn("Could not set user authentication in security context: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private ActiveUserContext buildActiveContext(UserPrincipal principal, UUID activeRoleId, UUID activeDeptId, UUID activeSubDeptId) {
        if (principal == null) return null;

        UserRoleAssignment matched = null;
        if (activeRoleId != null && principal.getAssignments() != null) {
            matched = principal.getAssignments().stream()
                    .filter(a -> a.isActive() && a.getRole() != null && a.getRole().getId().equals(activeRoleId))
                    .findFirst()
                    .orElse(null);
        }

        if (matched == null && principal.getAssignments() != null && !principal.getAssignments().isEmpty()) {
            matched = principal.getAssignments().stream().filter(UserRoleAssignment::isActive).findFirst().orElse(null);
        }

        DataScopeType effectiveScope = DataScopeType.SELF;
        Set<UUID> customDeptIds = Collections.emptySet();
        Set<UUID> customSubDeptIds = Collections.emptySet();
        UUID roleId = null;
        String roleName = null;
        UUID deptId = activeDeptId;
        UUID subDeptId = activeSubDeptId;

        if (matched != null) {
            roleId = matched.getRole() != null ? matched.getRole().getId() : null;
            roleName = matched.getRole() != null ? matched.getRole().getName() : null;
            if (deptId == null && matched.getDepartment() != null) {
                deptId = matched.getDepartment().getId();
            }
            if (subDeptId == null && matched.getSubDepartment() != null) {
                subDeptId = matched.getSubDepartment().getId();
            }
            effectiveScope = matched.getDataScopeType() != null ? matched.getDataScopeType()
                    : (matched.getRole() != null ? matched.getRole().getDefaultDataScope() : DataScopeType.SELF);

            if (matched.getCustomDepartments() != null) {
                customDeptIds = matched.getCustomDepartments().stream().map(d -> d.getId()).collect(Collectors.toSet());
            }
            if (matched.getCustomSubDepartments() != null) {
                customSubDeptIds = matched.getCustomSubDepartments().stream().map(sd -> sd.getId()).collect(Collectors.toSet());
            }
        } else if ("superadmin".equalsIgnoreCase(principal.getUsername()) || (principal.getAuthorities() != null && principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_SUPER_ADMIN")))) {
            effectiveScope = DataScopeType.GLOBAL;
            roleName = "SUPER_ADMIN";
        }

        Set<String> authorities = principal.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        return ActiveUserContext.builder()
                .userId(principal.getId())
                .username(principal.getUsername())
                .roleId(roleId)
                .roleName(roleName)
                .departmentId(deptId)
                .subDepartmentId(subDeptId)
                .effectiveDataScope(effectiveScope)
                .customDepartmentIds(customDeptIds)
                .customSubDepartmentIds(customSubDeptIds)
                .grantedAuthorities(authorities)
                .build();
    }

    private UUID getUuidFromHeaderOrClaims(HttpServletRequest request, String headerName, Claims claims, String claimKey) {
        String headerVal = request.getHeader(headerName);
        if (StringUtils.hasText(headerVal)) {
            try {
                return UUID.fromString(headerVal.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        if (claims != null && claims.get(claimKey) != null) {
            try {
                return UUID.fromString(claims.get(claimKey).toString());
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
