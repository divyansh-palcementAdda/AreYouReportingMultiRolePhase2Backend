package com.app.AreYouReporting.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContextSwitchRequest {

    @NotNull(message = "Role ID is required")
    private UUID roleId;

    private UUID departmentId;
    private UUID subDepartmentId;
}
