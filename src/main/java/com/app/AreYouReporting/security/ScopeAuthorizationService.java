package com.app.AreYouReporting.security;

import com.app.AreYouReporting.Entities.*;
import com.app.AreYouReporting.exceptions.ScopeViolationException;
import com.app.AreYouReporting.exceptions.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
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
        return principal.getAssignments().stream()
                .anyMatch(a -> a.isActive() && a.getRole() != null && "SUPER_ADMIN".equalsIgnoreCase(a.getRole().getName()));
    }

    public boolean hasPermission(String authority) {
        UserPrincipal principal = getCurrentPrincipal();
        if (isSuperAdmin(principal)) {
            return true;
        }
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(authority));
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
