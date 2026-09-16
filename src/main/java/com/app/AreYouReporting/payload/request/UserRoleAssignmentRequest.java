package com.app.AreYouReporting.payload.request;

import com.app.AreYouReporting.Entities.DataScopeType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleAssignmentRequest {

    @NotNull(message = "Role ID is required")
    private UUID roleId;

    private UUID departmentId;
    private UUID subDepartmentId;
    private DataScopeType dataScopeType;
    private Set<UUID> customDepartmentIds;
    private Set<UUID> customSubDepartmentIds;
}
