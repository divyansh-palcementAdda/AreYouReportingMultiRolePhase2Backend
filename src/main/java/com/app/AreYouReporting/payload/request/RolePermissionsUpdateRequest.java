package com.app.AreYouReporting.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionsUpdateRequest {

    @NotNull(message = "Permission IDs set cannot be null")
    private Set<UUID> permissionIds;
}
