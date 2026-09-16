package com.app.AreYouReporting.service.interfaces;

import com.app.AreYouReporting.payload.request.RoleRequest;
import com.app.AreYouReporting.payload.response.RoleDto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RoleService {

    List<RoleDto> getAllRoles();

    RoleDto getRoleById(UUID id);

    RoleDto createRole(RoleRequest request);

    RoleDto updateRole(UUID id, RoleRequest request);

    RoleDto updateRolePermissions(UUID id, Set<UUID> permissionIds);

    void deleteRole(UUID id);

    void seedBaselineRoles();
}
