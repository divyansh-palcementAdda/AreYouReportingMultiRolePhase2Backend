package com.app.AreYouReporting.payload.response;

import com.app.AreYouReporting.Entities.DataScopeType;
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
public class UserRoleAssignmentDto {

    private UUID id;
    private UUID roleId;
    private String roleName;
    private UUID departmentId;
    private String departmentName;
    private UUID subDepartmentId;
    private String subDepartmentName;
    private DataScopeType dataScopeType;
    private List<DepartmentDto> customDepartments;
    private List<SubDepartmentDto> customSubDepartments;
    private boolean isActive;
}
