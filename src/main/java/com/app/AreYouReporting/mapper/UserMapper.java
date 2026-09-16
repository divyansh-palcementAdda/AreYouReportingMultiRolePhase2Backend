package com.app.AreYouReporting.mapper;

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
                .customDepartments(assignment.getCustomDepartments() != null ? departmentMapper.toDtoList(assignment.getCustomDepartments()) : Collections.emptyList())
                .customSubDepartments(assignment.getCustomSubDepartments() != null ? departmentMapper.toSubDeptDtoList(assignment.getCustomSubDepartments()) : Collections.emptyList())
                .isActive(assignment.isActive())
                .build();
    }

    public List<UserRoleAssignmentDto> toAssignmentDtoList(Collection<UserRoleAssignment> assignments) {
        if (assignments == null) return Collections.emptyList();
        return assignments.stream().map(this::toAssignmentDto).collect(Collectors.toList());
    }

    public UserDto toDto(User user, List<ResourceGrantsDto> effectiveGrants) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.isActive())
                .departments(user.getDepartments() != null ? departmentMapper.toDtoList(user.getDepartments()) : Collections.emptyList())
                .subDepartments(user.getSubDepartments() != null ? departmentMapper.toSubDeptDtoList(user.getSubDepartments()) : Collections.emptyList())
                .roleAssignments(user.getRoleAssignments() != null ? toAssignmentDtoList(user.getRoleAssignments()) : Collections.emptyList())
                .effectiveGrants(effectiveGrants != null ? effectiveGrants : Collections.emptyList())
                .build();
    }
}
