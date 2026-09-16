package com.app.AreYouReporting.service.impl;

import com.app.AreYouReporting.Entities.AuditStatus;
import com.app.AreYouReporting.Entities.Permission;
import com.app.AreYouReporting.Entities.User;
import com.app.AreYouReporting.Entities.UserRoleAssignment;
import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ResourceNotFoundException;
import com.app.AreYouReporting.exceptions.UnauthorizedException;
import com.app.AreYouReporting.mapper.PermissionMapper;
import com.app.AreYouReporting.mapper.UserMapper;
import com.app.AreYouReporting.payload.request.ContextSwitchRequest;
import com.app.AreYouReporting.payload.request.LoginRequest;
import com.app.AreYouReporting.payload.request.RefreshTokenRequest;
import com.app.AreYouReporting.payload.response.AuthResponse;
import com.app.AreYouReporting.payload.response.ResourceGrantsDto;
import com.app.AreYouReporting.payload.response.UserDto;
import com.app.AreYouReporting.payload.response.UserRoleAssignmentDto;
import com.app.AreYouReporting.payload.response.UserSummaryDto;
import com.app.AreYouReporting.repository.PermissionRepository;
import com.app.AreYouReporting.repository.UserRepository;
import com.app.AreYouReporting.repository.UserRoleAssignmentRepository;
import com.app.AreYouReporting.security.CustomUserDetailsService;
import com.app.AreYouReporting.security.JwtTokenProvider;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.security.UserPrincipal;
import com.app.AreYouReporting.service.interfaces.AuditService;
import com.app.AreYouReporting.service.interfaces.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final PermissionRepository permissionRepository;
    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    private final CustomUserDetailsService customUserDetailsService;
    private final ScopeAuthorizationService scopeSecurity;
    private final AuditService auditService;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        List<UserRoleAssignment> assignments = assignmentRepository.findAllActiveWithDetailsByUserId(user.getId());

        // Resolve active role & scope
        UserRoleAssignment activeAssignment = null;
        if (request.getActiveRoleId() != null) {
            activeAssignment = assignments.stream()
                    .filter(a -> a.getRole() != null && a.getRole().getId().equals(request.getActiveRoleId()))
                    .findFirst()
                    .orElse(null);
        }

        if (activeAssignment == null && !assignments.isEmpty()) {
            activeAssignment = assignments.get(0);
        }

        UUID activeRoleId = activeAssignment != null && activeAssignment.getRole() != null ? activeAssignment.getRole().getId() : null;
        String activeRoleName = activeAssignment != null && activeAssignment.getRole() != null ? activeAssignment.getRole().getName() : null;
        UUID activeDeptId = request.getActiveDepartmentId() != null ? request.getActiveDepartmentId()
                : (activeAssignment != null && activeAssignment.getDepartment() != null ? activeAssignment.getDepartment().getId() : null);
        String activeDeptName = activeAssignment != null && activeAssignment.getDepartment() != null ? activeAssignment.getDepartment().getName() : null;
        UUID activeSubDeptId = request.getActiveSubDepartmentId() != null ? request.getActiveSubDepartmentId()
                : (activeAssignment != null && activeAssignment.getSubDepartment() != null ? activeAssignment.getSubDepartment().getId() : null);
        String activeSubDeptName = activeAssignment != null && activeAssignment.getSubDepartment() != null ? activeAssignment.getSubDepartment().getName() : null;

        String accessToken = tokenProvider.generateAccessToken(principal, activeRoleId, activeRoleName, activeDeptId, activeSubDeptId);
        String refreshToken = tokenProvider.generateRefreshToken(principal);

        List<UserRoleAssignmentDto> availableAssignments = userMapper.toAssignmentDtoList(assignments);

        auditService.log(user.getUsername(), activeRoleName, "USER_LOGIN", "USER", user.getId().toString(), null, null, "Successful login as " + activeRoleName, AuditStatus.SUCCESS, null);

        return AuthResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getExpirationMs())
                .user(userMapper.toSummaryDto(user))
                .activeRoleId(activeRoleId)
                .activeRoleName(activeRoleName)
                .activeDepartmentId(activeDeptId)
                .activeDepartmentName(activeDeptName)
                .activeSubDepartmentId(activeSubDeptId)
                .activeSubDepartmentName(activeSubDeptName)
                .availableAssignments(availableAssignments)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse switchContext(ContextSwitchRequest request) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        List<UserRoleAssignment> assignments = assignmentRepository.findAllActiveWithDetailsByUserId(user.getId());

        UserRoleAssignment targetAssignment = assignments.stream()
                .filter(a -> a.getRole() != null && a.getRole().getId().equals(request.getRoleId()))
                .findFirst()
                .orElseThrow(() -> new InvalidOperationException("User does not have role with id: " + request.getRoleId()));

        UUID activeRoleId = targetAssignment.getRole().getId();
        String activeRoleName = targetAssignment.getRole().getName();
        UUID activeDeptId = request.getDepartmentId() != null ? request.getDepartmentId()
                : (targetAssignment.getDepartment() != null ? targetAssignment.getDepartment().getId() : null);
        String activeDeptName = targetAssignment.getDepartment() != null ? targetAssignment.getDepartment().getName() : null;
        UUID activeSubDeptId = request.getSubDepartmentId() != null ? request.getSubDepartmentId()
                : (targetAssignment.getSubDepartment() != null ? targetAssignment.getSubDepartment().getId() : null);
        String activeSubDeptName = targetAssignment.getSubDepartment() != null ? targetAssignment.getSubDepartment().getName() : null;

        String accessToken = tokenProvider.generateAccessToken(principal, activeRoleId, activeRoleName, activeDeptId, activeSubDeptId);
        String refreshToken = tokenProvider.generateRefreshToken(principal);

        auditService.log(user.getUsername(), activeRoleName, "SWITCH_CONTEXT", "USER", user.getId().toString(), null, null, "Switched context to role=" + activeRoleName + " dept=" + activeDeptName, AuditStatus.SUCCESS, null);

        return AuthResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getExpirationMs())
                .user(userMapper.toSummaryDto(user))
                .activeRoleId(activeRoleId)
                .activeRoleName(activeRoleName)
                .activeDepartmentId(activeDeptId)
                .activeDepartmentName(activeDeptName)
                .activeSubDepartmentId(activeSubDeptId)
                .activeSubDepartmentName(activeSubDeptName)
                .availableAssignments(userMapper.toAssignmentDtoList(assignments))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        UUID userId = tokenProvider.getUserIdFromToken(request.getRefreshToken());
        UserPrincipal principal = (UserPrincipal) customUserDetailsService.loadUserById(userId);

        List<UserRoleAssignment> assignments = assignmentRepository.findAllActiveWithDetailsByUserId(userId);
        UserRoleAssignment first = !assignments.isEmpty() ? assignments.get(0) : null;

        UUID activeRoleId = first != null && first.getRole() != null ? first.getRole().getId() : null;
        String activeRoleName = first != null && first.getRole() != null ? first.getRole().getName() : null;
        UUID activeDeptId = first != null && first.getDepartment() != null ? first.getDepartment().getId() : null;
        UUID activeSubDeptId = first != null && first.getSubDepartment() != null ? first.getSubDepartment().getId() : null;

        String newAccessToken = tokenProvider.generateAccessToken(principal, activeRoleId, activeRoleName, activeDeptId, activeSubDeptId);
        String newRefreshToken = tokenProvider.generateRefreshToken(principal);

        return AuthResponse.builder()
                .tokenType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(tokenProvider.getExpirationMs())
                .user(UserSummaryDto.builder()
                        .id(principal.getId())
                        .username(principal.getUsername())
                        .email(principal.getEmail())
                        .fullName(principal.getFullName())
                        .build())
                .activeRoleId(activeRoleId)
                .activeRoleName(activeRoleName)
                .activeDepartmentId(activeDeptId)
                .activeSubDepartmentId(activeSubDeptId)
                .availableAssignments(userMapper.toAssignmentDtoList(assignments))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUserProfile() {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        List<Permission> allPermissions = permissionRepository.findAllByOrderByResourceAscActionAsc();
        Set<Permission> userPerms = new HashSet<>();
        if (user.getRoleAssignments() != null) {
            for (UserRoleAssignment assignment : user.getRoleAssignments()) {
                if (assignment.isActive() && assignment.getRole() != null && assignment.getRole().getPermissions() != null) {
                    userPerms.addAll(assignment.getRole().getPermissions());
                }
            }
        }
        return userMapper.toDto(user, permissionMapper.toResourceGrants(allPermissions, userPerms));
    }
}
