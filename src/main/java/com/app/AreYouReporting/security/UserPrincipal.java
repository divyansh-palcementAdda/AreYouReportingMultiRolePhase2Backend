package com.app.AreYouReporting.security;

import com.app.AreYouReporting.Entities.User;
import com.app.AreYouReporting.Entities.UserRoleAssignment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String email;
    private final String password;
    private final String fullName;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;
    private final List<UserRoleAssignment> assignments;
    private final Set<UUID> departmentIds;
    private final Set<UUID> subDepartmentIds;

    public static UserPrincipal create(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (user.getRoleAssignments() != null) {
            for (UserRoleAssignment assignment : user.getRoleAssignments()) {
                if (assignment.isActive() && assignment.getRole() != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + assignment.getRole().getName()));
                    if (assignment.getRole().getPermissions() != null) {
                        assignment.getRole().getPermissions().forEach(permission ->
                                authorities.add(new SimpleGrantedAuthority(permission.getAuthority())));
                    }
                }
                if (assignment.getDepartment() != null) {
                    assignment.getDepartment().getId();
                }
                if (assignment.getSubDepartment() != null) {
                    assignment.getSubDepartment().getId();
                }
                if (assignment.getCustomDepartments() != null) {
                    assignment.getCustomDepartments().size();
                }
                if (assignment.getCustomSubDepartments() != null) {
                    assignment.getCustomSubDepartments().size();
                }
            }
        }

        if ("superadmin".equalsIgnoreCase(user.getUsername())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }

        Set<UUID> deptIds = user.getDepartments() != null
                ? user.getDepartments().stream().map(d -> d.getId()).collect(Collectors.toSet())
                : Collections.emptySet();

        Set<UUID> subDeptIds = user.getSubDepartments() != null
                ? user.getSubDepartments().stream().map(sd -> sd.getId()).collect(Collectors.toSet())
                : Collections.emptySet();

        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .fullName(user.getFullName())
                .active(user.isActive())
                .authorities(authorities)
                .assignments(user.getRoleAssignments() != null ? new ArrayList<>(user.getRoleAssignments()) : Collections.emptyList())
                .departmentIds(deptIds)
                .subDepartmentIds(subDeptIds)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
