package com.app.AreYouReporting.mapper;

import com.app.AreYouReporting.Entities.*;
import com.app.AreYouReporting.payload.response.dropdown.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DropdownMapper {

    public DepartmentDropdownDto toDepartmentDropdownDto(Department dept) {
        if (dept == null) return null;
        return DepartmentDropdownDto.builder()
                .id(dept.getId())
                .label(dept.getName())
                .name(dept.getName())
                .code(dept.getCode())
                .active(dept.isActive())
                .build();
    }

    public List<DepartmentDropdownDto> toDepartmentDropdownDtoList(Collection<Department> departments) {
        if (departments == null) return Collections.emptyList();
        return departments.stream()
                .filter(Objects::nonNull)
                .map(this::toDepartmentDropdownDto)
                .collect(Collectors.toList());
    }

    public SubDepartmentDropdownDto toSubDepartmentDropdownDto(SubDepartment subDept) {
        if (subDept == null) return null;
        return SubDepartmentDropdownDto.builder()
                .id(subDept.getId())
                .label(subDept.getName())
                .name(subDept.getName())
                .code(subDept.getCode())
                .departmentId(subDept.getDepartment() != null ? subDept.getDepartment().getId() : null)
                .departmentName(subDept.getDepartment() != null ? subDept.getDepartment().getName() : null)
                .active(subDept.isActive())
                .build();
    }

    public List<SubDepartmentDropdownDto> toSubDepartmentDropdownDtoList(Collection<SubDepartment> subDepartments) {
        if (subDepartments == null) return Collections.emptyList();
        return subDepartments.stream()
                .filter(Objects::nonNull)
                .map(this::toSubDepartmentDropdownDto)
                .collect(Collectors.toList());
    }

    public UserDropdownDto toUserDropdownDto(User user) {
        if (user == null) return null;

        List<String> roles = Collections.emptyList();
        if (user.getRoleAssignments() != null) {
            roles = user.getRoleAssignments().stream()
                    .filter(a -> a.isActive() && a.getRole() != null)
                    .map(a -> a.getRole().getName())
                    .distinct()
                    .collect(Collectors.toList());
        }

        List<String> deptNames = Collections.emptyList();
        if (user.getDepartments() != null) {
            deptNames = user.getDepartments().stream()
                    .map(Department::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        List<String> subDeptNames = Collections.emptyList();
        if (user.getSubDepartments() != null) {
            subDeptNames = user.getSubDepartments().stream()
                    .map(SubDepartment::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return UserDropdownDto.builder()
                .id(user.getId())
                .label(user.getFullName() != null ? user.getFullName() : user.getUsername())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .departmentNames(deptNames)
                .subDepartmentNames(subDeptNames)
                .active(user.isActive())
                .build();
    }

    public List<UserDropdownDto> toUserDropdownDtoList(Collection<User> users) {
        if (users == null) return Collections.emptyList();
        return users.stream()
                .filter(Objects::nonNull)
                .map(this::toUserDropdownDto)
                .collect(Collectors.toList());
    }

    public TaskTemplateDropdownDto toTaskTemplateDropdownDto(TaskTemplate template) {
        if (template == null) return null;

        Set<UUID> deptIds = template.getApplicableDepartments() != null
                ? template.getApplicableDepartments().stream().map(Department::getId).collect(Collectors.toSet())
                : Collections.emptySet();

        Set<UUID> subDeptIds = template.getApplicableSubDepartments() != null
                ? template.getApplicableSubDepartments().stream().map(SubDepartment::getId).collect(Collectors.toSet())
                : Collections.emptySet();

        return TaskTemplateDropdownDto.builder()
                .id(template.getId())
                .label(template.getName())
                .name(template.getName())
                .defaultPriority(template.getDefaultPriority())
                .defaultDurationDays(template.getDefaultDurationDays())
                .defaultTargetCount(template.getDefaultTargetCount())
                .defaultTargetPercentage(template.getDefaultTargetPercentage())
                .active(template.isActive())
                .applicableDepartmentIds(deptIds)
                .applicableSubDepartmentIds(subDeptIds)
                .build();
    }

    public List<TaskTemplateDropdownDto> toTaskTemplateDropdownDtoList(Collection<TaskTemplate> templates) {
        if (templates == null) return Collections.emptyList();
        return templates.stream()
                .filter(Objects::nonNull)
                .map(this::toTaskTemplateDropdownDto)
                .collect(Collectors.toList());
    }

    public TaskDropdownDto toTaskDropdownDto(Task task) {
        if (task == null) return null;
        return TaskDropdownDto.builder()
                .id(task.getId())
                .label(task.getTitle())
                .title(task.getTitle())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .isSelfTask(task.isSelfTask())
                .build();
    }

    public List<TaskDropdownDto> toTaskDropdownDtoList(Collection<Task> tasks) {
        if (tasks == null) return Collections.emptyList();
        return tasks.stream()
                .filter(Objects::nonNull)
                .map(this::toTaskDropdownDto)
                .collect(Collectors.toList());
    }

    public RoleDropdownDto toRoleDropdownDto(Role role) {
        if (role == null) return null;
        return RoleDropdownDto.builder()
                .id(role.getId())
                .label(role.getName())
                .name(role.getName())
                .description(role.getDescription())
                .defaultDataScope(role.getDefaultDataScope())
                .isSystemRole(role.isSystemRole())
                .build();
    }

    public List<RoleDropdownDto> toRoleDropdownDtoList(Collection<Role> roles) {
        if (roles == null) return Collections.emptyList();
        return roles.stream()
                .filter(Objects::nonNull)
                .map(this::toRoleDropdownDto)
                .collect(Collectors.toList());
    }

    public PermissionDropdownDto toPermissionDropdownDto(Permission permission) {
        if (permission == null) return null;
        return PermissionDropdownDto.builder()
                .id(permission.getId())
                .label(permission.getAuthority())
                .authority(permission.getAuthority())
                .resource(permission.getResource())
                .action(permission.getAction())
                .description(permission.getDescription())
                .build();
    }

    public List<PermissionDropdownDto> toPermissionDropdownDtoList(Collection<Permission> permissions) {
        if (permissions == null) return Collections.emptyList();
        return permissions.stream()
                .filter(Objects::nonNull)
                .map(this::toPermissionDropdownDto)
                .collect(Collectors.toList());
    }
}
