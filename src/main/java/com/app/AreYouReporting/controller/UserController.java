package com.app.AreYouReporting.controller;

import com.app.AreYouReporting.payload.request.DataScopeOverrideRequest;
import com.app.AreYouReporting.payload.request.UserCreateRequest;
import com.app.AreYouReporting.payload.request.UserRoleAssignmentRequest;
import com.app.AreYouReporting.payload.request.UserUpdateRequest;
import com.app.AreYouReporting.payload.response.ApiResponse;
import com.app.AreYouReporting.payload.response.PageResponse;
import com.app.AreYouReporting.payload.response.UserDto;
import com.app.AreYouReporting.payload.response.UserSummaryDto;
import com.app.AreYouReporting.service.interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User & Scope Management", description = "Endpoints for managing users, multi-department/subdepartment mappings, dynamic role assignments, and custom data scope overrides")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get paginated list of users with mappings and effective grants")
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<UserDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable("id") UUID id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping
    @Operation(summary = "Create user with department/sub-department mappings and role assignments")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserDto user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User created successfully", user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user details, status, or departmental mappings")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UserUpdateRequest request) {
        UserDto user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
    }

    @PostMapping("/{userId}/assignments")
    @Operation(summary = "Assign a new dynamic role & department/sub-department scope to user")
    public ResponseEntity<ApiResponse<UserDto>> assignRole(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody UserRoleAssignmentRequest request) {
        UserDto user = userService.assignRoleToUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Role assigned successfully", user));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @Operation(summary = "Deactivate/revoke a user role assignment")
    public ResponseEntity<ApiResponse<Void>> removeRoleAssignment(@PathVariable("assignmentId") UUID assignmentId) {
        userService.removeRoleAssignment(assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Role assignment revoked successfully", null));
    }

    @PutMapping("/assignments/{assignmentId}/data-scope")
    @Operation(summary = "Override/customize data scope for a specific user assignment (e.g. grant department-wide access to a teacher)")
    public ResponseEntity<ApiResponse<UserDto>> overrideUserDataScope(
            @PathVariable("assignmentId") UUID assignmentId,
            @Valid @RequestBody DataScopeOverrideRequest request) {
        UserDto user = userService.overrideUserDataScope(assignmentId, request);
        return ResponseEntity.ok(ApiResponse.success("User data scope overridden successfully", user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate user")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable("id") UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", null));
    }

    @GetMapping("/by-department/{departmentId}")
    @Operation(summary = "Get users mapped to a department")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> getUsersByDepartment(@PathVariable("departmentId") UUID departmentId) {
        List<UserSummaryDto> users = userService.getUsersByDepartment(departmentId);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/by-sub-department/{subDepartmentId}")
    @Operation(summary = "Get users mapped to a sub-department")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> getUsersBySubDepartment(@PathVariable("subDepartmentId") UUID subDepartmentId) {
        List<UserSummaryDto> users = userService.getUsersBySubDepartment(subDepartmentId);
        return ResponseEntity.ok(ApiResponse.success(users));
    }
}
