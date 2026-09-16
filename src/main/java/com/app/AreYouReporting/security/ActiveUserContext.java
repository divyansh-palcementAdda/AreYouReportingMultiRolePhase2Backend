package com.app.AreYouReporting.security;

import com.app.AreYouReporting.Entities.DataScopeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveUserContext {

    private UUID userId;
    private String username;
    private UUID roleId;
    private String roleName;
    private UUID departmentId;
    private UUID subDepartmentId;
    private DataScopeType effectiveDataScope;
    private Set<UUID> customDepartmentIds;
    private Set<UUID> customSubDepartmentIds;
    private Set<String> grantedAuthorities;
}
