package com.app.AreYouReporting.service.interfaces;

import com.app.AreYouReporting.payload.response.PermissionDto;
import com.app.AreYouReporting.payload.response.ResourceGrantsDto;
import com.app.AreYouReporting.payload.response.UserPermissionResponse;

import java.util.List;
import java.util.UUID;

public interface PermissionService {

    List<PermissionDto> getAllPermissions();

    List<ResourceGrantsDto> getAllPermissionsGrouped();

    List<ResourceGrantsDto> getPermissionsByRoleId(UUID roleId);

    UserPermissionResponse getUserPermissions(UUID userId, UUID roleId);

    PermissionDto getPermissionById(UUID id);

    PermissionDto createPermission(String resource, String action, String description);

    void seedBaselinePermissions();
}

