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
public class RoleDto {

    private UUID id;
    private String name;
    private String description;
    private boolean isSystemRole;
    private DataScopeType defaultDataScope;
    private List<PermissionDto> permissions;
}
