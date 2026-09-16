package com.app.AreYouReporting.controller;

import com.app.AreYouReporting.payload.response.ApiResponse;
import com.app.AreYouReporting.payload.response.PermissionDto;
import com.app.AreYouReporting.payload.response.ResourceGrantsDto;
import com.app.AreYouReporting.payload.response.UserPermissionResponse;
import com.app.AreYouReporting.service.interfaces.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/permissions"})
@RequiredArgsConstructor
@Tag(name = "Permissions Management", description = "Endpoints for dynamic permission discovery and role-permission grants")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @Operation(summary = "Get all system permissions as a flat list")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getAllPermissions() {
        List<PermissionDto> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @GetMapping("/grouped")
    @Operation(summary = "Get all system permissions grouped by resource")
    public ResponseEntity<ApiResponse<List<ResourceGrantsDto>>> getAllPermissionsGrouped() {
        List<ResourceGrantsDto> grouped = permissionService.getAllPermissionsGrouped();
        return ResponseEntity.ok(ApiResponse.success(grouped));
    }

    @GetMapping("/get-permissions-by-role")
    @Operation(summary = "Get all system permissions grouped by resource with granted flags for a specific role")
    public ResponseEntity<List<ResourceGrantsDto>> getPermissionsByRole(@RequestParam("roleId") UUID roleId) {
        List<ResourceGrantsDto> grants = permissionService.getPermissionsByRoleId(roleId);
        return ResponseEntity.ok(grants);
    }

    @GetMapping({"/user-permissions", "/get-permissions-by-user", "/me"})
    @Operation(summary = "Get permissions and custom scopes for a user (or current authenticated user if userId not provided)")
    public ResponseEntity<ApiResponse<UserPermissionResponse>> getUserPermissions(
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "roleId", required = false) UUID roleId) {
        UserPermissionResponse response = permissionService.getUserPermissions(userId, roleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get permissions and custom scopes for a specific user ID")
    public ResponseEntity<ApiResponse<UserPermissionResponse>> getPermissionsByUserId(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "roleId", required = false) UUID roleId) {
        UserPermissionResponse response = permissionService.getUserPermissions(userId, roleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get permission by ID")
    public ResponseEntity<ApiResponse<PermissionDto>> getPermissionById(@PathVariable("id") UUID id) {
        PermissionDto permission = permissionService.getPermissionById(id);
        return ResponseEntity.ok(ApiResponse.success(permission));
    }

    @PostMapping
    @Operation(summary = "Create custom permission")
    public ResponseEntity<ApiResponse<PermissionDto>> createPermission(
            @RequestParam("resource") String resource,
            @RequestParam("action") String action,
            @RequestParam(value = "description", required = false) String description) {
        PermissionDto permission = permissionService.createPermission(resource, action, description);
        return ResponseEntity.ok(ApiResponse.success("Permission created successfully", permission));
    }
}
