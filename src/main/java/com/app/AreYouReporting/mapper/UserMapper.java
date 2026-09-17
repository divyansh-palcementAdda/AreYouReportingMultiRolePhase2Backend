package com.app.AreYouReporting.mapper;

import com.app.AreYouReporting.Entities.Department;
import com.app.AreYouReporting.Entities.SubDepartment;
import com.app.AreYouReporting.Entities.User;
import com.app.AreYouReporting.Entities.UserRoleAssignment;
import com.app.AreYouReporting.payload.response.ResourceGrantsDto;
import com.app.AreYouReporting.payload.response.UserDto;
import com.app.AreYouReporting.payload.response.UserRoleAssignmentDto;
import com.app.AreYouReporting.payload.response.UserSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final DepartmentMapper departmentMapper;

    public UserSummaryDto toSummaryDto(User user) {
        if (user == null) return null;
        return UserSummaryDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    public List<UserSummaryDto> toSummaryDtoList(Collection<User> users) {
        if (users == null) return Collections.emptyList();
        return users.stream().map(this::toSummaryDto).collect(Collectors.toList());
    }

    public UserRoleAssignmentDto toAssignmentDto(UserRoleAssignment assignment) {
        if (assignment == null) return null;

        List<Department> activeCustomDepts = assignment.getCustomDepartments() != null
                ? assignment.getCustomDepartments().stream().filter(Department::isActive).collect(Collectors.toList())
                : Collections.emptyList();

        List<SubDepartment> activeCustomSubDepts = assignment.getCustomSubDepartments() != null
                ? assignment.getCustomSubDepartments().stream()
                .filter(sd -> sd.isActive() && sd.getDepartment() != null && sd.getDepartment().isActive())
                .collect(Collectors.toList())
                : Collections.emptyList();

        return UserRoleAssignmentDto.builder()
                .id(assignment.getId())
                .roleId(assignment.getRole() != null ? assignment.getRole().getId() : null)
                .roleName(assignment.getRole() != null ? assignment.getRole().getName() : null)
                .departmentId(assignment.getDepartment() != null ? assignment.getDepartment().getId() : null)
                .departmentName(assignment.getDepartment() != null ? assignment.getDepartment().getName() : null)
                .subDepartmentId(assignment.getSubDepartment() != null ? assignment.getSubDepartment().getId() : null)
                .subDepartmentName(assignment.getSubDepartment() != null ? assignment.getSubDepartment().getName() : null)
                .dataScopeType(assignment.getDataScopeType() != null ? assignment.getDataScopeType()
                        : (assignment.getRole() != null ? assignment.getRole().getDefaultDataScope() : null))
                .customDepartments(departmentMapper.toDtoList(activeCustomDepts))
                .customSubDepartments(departmentMapper.toSubDeptDtoList(activeCustomSubDepts))
                .isActive(assignment.isActive())
                .build();
    }

    public List<UserRoleAssignmentDto> toAssignmentDtoList(Collection<UserRoleAssignment> assignments) {
        if (assignments == null) return Collections.emptyList();
        return assignments.stream().map(this::toAssignmentDto).collect(Collectors.toList());
    }

    public UserDto toDto(User user, List<ResourceGrantsDto> effectiveGrants) {
        if (user == null) return null;

        List<Department> activeDepts = user.getDepartments() != null
                ? user.getDepartments().stream().filter(Department::isActive).collect(Collectors.toList())
                : Collections.emptyList();

        List<SubDepartment> activeSubDepts = user.getSubDepartments() != null
                ? user.getSubDepartments().stream()
                .filter(sd -> sd.isActive() && sd.getDepartment() != null && sd.getDepartment().isActive())
                .collect(Collectors.toList())
                : Collections.emptyList();

        List<UserRoleAssignment> activeAssignments = user.getRoleAssignments() != null
                ? user.getRoleAssignments().stream()
                .filter(UserRoleAssignment::isActive)
                .filter(a -> a.getDepartment() == null || a.getDepartment().isActive())
                .filter(a -> a.getSubDepartment() == null || (a.getSubDepartment().isActive() && a.getSubDepartment().getDepartment() != null && a.getSubDepartment().getDepartment().isActive()))
                .collect(Collectors.toList())
                : Collections.emptyList();

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.isActive())
                .departments(departmentMapper.toDtoList(activeDepts))
                .subDepartments(departmentMapper.toSubDeptDtoList(activeSubDepts))
                .roleAssignments(toAssignmentDtoList(activeAssignments))
                .effectiveGrants(effectiveGrants != null ? effectiveGrants : Collections.emptyList())
                .build();
    }
}
