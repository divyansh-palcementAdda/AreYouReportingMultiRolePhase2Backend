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
public class DataScopeOverrideRequest {

    @NotNull(message = "Data scope type is required")
    private DataScopeType dataScopeType;

    private Set<UUID> customDepartmentIds;
    private Set<UUID> customSubDepartmentIds;
}
