package com.app.AreYouReporting.security;

import com.app.AreYouReporting.Entities.*;
import com.app.AreYouReporting.exceptions.ScopeViolationException;
import com.app.AreYouReporting.exceptions.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service("scopeSecurity")
@Slf4j
public class ScopeAuthorizationService {

    public static final String CONTEXT_ATTRIBUTE = "ACTIVE_USER_CONTEXT";

    public UserPrincipal getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return (UserPrincipal) auth.getPrincipal();
    }

    public UUID getCurrentUserId() {
        return getCurrentPrincipal().getId();
    }

    public String getCurrentUsername() {
        return getCurrentPrincipal().getUsername();
    }

    public boolean isSuperAdmin(UserPrincipal principal) {
        if (principal == null) return false;
        if ("superadmin".equalsIgnoreCase(principal.getUsername())) {
            return true;
        }
        if (principal.getAuthorities() != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_SUPER_ADMIN") || a.getAuthority().equalsIgnoreCase("SUPER_ADMIN"))) {
            return true;
        }
        if (principal.getAssignments() != null) {
            return principal.getAssignments().stream()
                    .anyMatch(a -> a.isActive() && a.getRole() != null && "SUPER_ADMIN".equalsIgnoreCase(a.getRole().getName()));
        }
        return false;
    }

    public boolean hasPermission(String authority) {
        UserPrincipal principal = getCurrentPrincipal();
        if (isSuperAdmin(principal)) {
            return true;
        }

        Set<String> aliases = resolvePermissionAliases(authority);
        return principal.getAuthorities().stream()
                .anyMatch(a -> aliases.contains(a.getAuthority().toLowerCase().trim()));
    }

    private Set<String> resolvePermissionAliases(String authority) {
        if (authority == null || authority.isBlank()) return Collections.emptySet();
        String normalized = authority.toLowerCase().trim();
        Set<String> aliases = new HashSet<>();
        aliases.add(normalized);
        aliases.add(authority.toUpperCase().trim());

        if (normalized.equals("dropdowns.departments") || normalized.equals("dropdown_view_departments")) {
            aliases.addAll(List.of("dropdowns.departments", "dropdown_view_departments", "dropdowns.read", "departments.read"));
        } else if (normalized.equals("dropdowns.sub-departments") || normalized.equals("dropdown_view_sub_departments")) {
            aliases.addAll(List.of("dropdowns.sub-departments", "dropdown_view_sub_departments", "dropdowns.read", "sub-departments.read"));
        } else if (normalized.equals("dropdowns.users") || normalized.equals("dropdown_view_users")) {
            aliases.addAll(List.of("dropdowns.users", "dropdown_view_users", "dropdowns.read", "users.read"));
        } else if (normalized.equals("dropdowns.tasks") || normalized.equals("dropdown_view_tasks")) {
            aliases.addAll(List.of("dropdowns.tasks", "dropdown_view_tasks", "dropdowns.read", "tasks.read"));
        } else if (normalized.equals("dropdowns.templates") || normalized.equals("dropdown_view_task_templates")) {
            aliases.addAll(List.of("dropdowns.templates", "dropdown_view_task_templates", "dropdowns.read", "templates.read"));
        } else if (normalized.equals("dropdowns.roles") || normalized.equals("dropdown_view_roles")) {
            aliases.addAll(List.of("dropdowns.roles", "dropdown_view_roles", "dropdowns.read", "roles.read"));
        } else if (normalized.equals("dropdowns.permissions") || normalized.equals("dropdown_view_permission_options")) {
            aliases.addAll(List.of("dropdowns.permissions", "dropdown_view_permission_options", "dropdowns.read", "roles.assign-permissions"));
        } else if (normalized.equals("dropdowns.options")) {
            aliases.addAll(List.of("dropdowns.options", "dropdown_view_task_status_options", "dropdown_view_request_status_options", "dropdown_view_proof_requirements"));
        }

        return aliases.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    public void validatePermission(String authority) {
        if (!hasPermission(authority)) {
            throw new ScopeViolationException("Access denied: missing required authority '" + authority + "'");
        }
    }

    public boolean canAccessDepartment(UUID departmentId, ActiveUserContext context, UserPrincipal principal) {
        if (departmentId == null) return true;
        if (isSuperAdmin(principal)) return true;

        if (context != null && context.getEffectiveDataScope() == DataScopeType.GLOBAL) {
            return true;
        }

        if (context != null && context.getEffectiveDataScope() == DataScopeType.DEPARTMENT) {
            if (context.getDepartmentId() != null && context.getDepartmentId().equals(departmentId)) {
                return true;
            }
            if (context.getCustomDepartmentIds() != null && context.getCustomDepartmentIds().contains(departmentId)) {
                return true;
            }
        }

        if (context != null && context.getEffectiveDataScope() == DataScopeType.CUSTOM) {
            if (context.getCustomDepartmentIds() != null && context.getCustomDepartmentIds().contains(departmentId)) {
                return true;
            }
        }

        return principal.getDepartmentIds().contains(departmentId);
    }

    public boolean canAccessSubDepartment(UUID subDepartmentId, UUID departmentId, ActiveUserContext context, UserPrincipal principal) {
        if (subDepartmentId == null) return true;
        if (isSuperAdmin(principal)) return true;

        if (context != null && context.getEffectiveDataScope() == DataScopeType.GLOBAL) {
            return true;
        }

        if (context != null && context.getEffectiveDataScope() == DataScopeType.DEPARTMENT) {
            if (departmentId != null && canAccessDepartment(departmentId, context, principal)) {
                return true;
            }
        }

        if (context != null && context.getEffectiveDataScope() == DataScopeType.SUB_DEPARTMENT) {
            if (context.getSubDepartmentId() != null && context.getSubDepartmentId().equals(subDepartmentId)) {
                return true;
            }
            if (context.getCustomSubDepartmentIds() != null && context.getCustomSubDepartmentIds().contains(subDepartmentId)) {
                return true;
            }
        }

        if (context != null && context.getEffectiveDataScope() == DataScopeType.CUSTOM) {
            if (context.getCustomSubDepartmentIds() != null && context.getCustomSubDepartmentIds().contains(subDepartmentId)) {
                return true;
            }
        }

        return principal.getSubDepartmentIds().contains(subDepartmentId);
    }

    public boolean canViewTask(Task task, ActiveUserContext context, UserPrincipal principal) {
        if (task == null) return false;
        if (isSuperAdmin(principal)) return true;

        UUID currentUserId = principal.getId();

        // Self-tasks or tasks created by user or assigned to user
        if (task.getCreator() != null && task.getCreator().getId().equals(currentUserId)) {
            return true;
        }
        if (task.getAssignees() != null && task.getAssignees().stream().anyMatch(u -> u.getId().equals(currentUserId))) {
            return true;
        }

        // Global data scope
        if (context != null && context.getEffectiveDataScope() == DataScopeType.GLOBAL) {
            return true;
        }

        // Department scope
        if (context != null && context.getEffectiveDataScope() == DataScopeType.DEPARTMENT) {
            if (task.getAssignedDepartments() != null && !task.getAssignedDepartments().isEmpty()) {
                boolean deptMatch = task.getAssignedDepartments().stream()
                        .anyMatch(d -> canAccessDepartment(d.getId(), context, principal));
                if (deptMatch) return true;
            }
        }

        // SubDepartment scope
        if (context != null && context.getEffectiveDataScope() == DataScopeType.SUB_DEPARTMENT) {
            if (task.getAssignedSubDepartments() != null && !task.getAssignedSubDepartments().isEmpty()) {
                boolean subDeptMatch = task.getAssignedSubDepartments().stream()
                        .anyMatch(sd -> canAccessSubDepartment(sd.getId(), sd.getDepartment() != null ? sd.getDepartment().getId() : null, context, principal));
                if (subDeptMatch) return true;
            }
        }

        return false;
    }

    public boolean canManageTask(Task task, ActiveUserContext context, UserPrincipal principal) {
        if (task == null) return false;
        if (isSuperAdmin(principal)) return true;

        // Self-tasks can be managed by their creator
        if (task.isSelfTask() && task.getCreator() != null && task.getCreator().getId().equals(principal.getId())) {
            return true;
        }

        if (task.getCreator() != null && task.getCreator().getId().equals(principal.getId())) {
            return true;
        }

        if (context != null && (context.getEffectiveDataScope() == DataScopeType.GLOBAL ||
                context.getEffectiveDataScope() == DataScopeType.DEPARTMENT ||
                context.getEffectiveDataScope() == DataScopeType.SUB_DEPARTMENT)) {
            return canViewTask(task, context, principal);
        }

        return false;
    }

    public boolean canApproveRequest(TaskRequest request, ActiveUserContext context, UserPrincipal principal) {
        if (request == null) return false;
        if (isSuperAdmin(principal)) return true;

        Task task = request.getTask();
        if (task == null) return false;

        // A user cannot approve their own request unless they are Super Admin
        if (request.getCreatedBy() != null && request.getCreatedBy().equalsIgnoreCase(principal.getUsername())) {
            return false;
        }

        return canManageTask(task, context, principal);
    }
}
