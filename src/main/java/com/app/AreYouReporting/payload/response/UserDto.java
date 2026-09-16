package com.app.AreYouReporting.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private boolean isActive;
    private List<DepartmentDto> departments;
    private List<SubDepartmentDto> subDepartments;
    private List<UserRoleAssignmentDto> roleAssignments;
    private List<ResourceGrantsDto> effectiveGrants;
}
