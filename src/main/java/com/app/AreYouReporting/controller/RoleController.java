package com.app.AreYouReporting.controller;

import com.app.AreYouReporting.payload.request.RolePermissionsUpdateRequest;
import com.app.AreYouReporting.payload.request.RoleRequest;
import com.app.AreYouReporting.payload.response.ApiResponse;
import com.app.AreYouReporting.payload.response.RoleDto;
import com.app.AreYouReporting.service.interfaces.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Dynamic Role Management", description = "Endpoints for creating, modifying, and assigning permissions to dynamic roles")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "Get all dynamic roles with their default data scopes and permissions")
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles() {
        List<RoleDto> roles = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    public ResponseEntity<ApiResponse<RoleDto>> getRoleById(@PathVariable("id") UUID id) {
        RoleDto role = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    @PostMapping
    @Operation(summary = "Create dynamic role with custom default data scope and permissions")
    public ResponseEntity<ApiResponse<RoleDto>> createRole(@Valid @RequestBody RoleRequest request) {
        RoleDto role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Role created successfully", role));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update role details and permissions")
    public ResponseEntity<ApiResponse<RoleDto>> updateRole(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RoleRequest request) {
        RoleDto role = roleService.updateRole(id, request);
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", role));
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Update granted permissions list for a role dynamically")
    public ResponseEntity<ApiResponse<RoleDto>> updateRolePermissions(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RolePermissionsUpdateRequest request) {
        RoleDto role = roleService.updateRolePermissions(id, request.getPermissionIds());
        return ResponseEntity.ok(ApiResponse.success("Role permissions updated successfully", role));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete custom dynamic role")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable("id") UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("Role deleted successfully", null));
    }
}
