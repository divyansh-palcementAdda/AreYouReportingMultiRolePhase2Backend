package com.app.AreYouReporting.controller;

import com.app.AreYouReporting.Entities.TaskPriority;
import com.app.AreYouReporting.Entities.TaskStatus;
import com.app.AreYouReporting.payload.response.ApiResponse;
import com.app.AreYouReporting.payload.response.PageResponse;
import com.app.AreYouReporting.payload.response.dropdown.*;
import com.app.AreYouReporting.service.interfaces.DropdownService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dropdowns")
@RequiredArgsConstructor
@Tag(name = "Lightweight Dropdown & Selection APIs", description = "High-performance, scope-aware selection APIs for forms, filters, assignments, and modals")
public class DropdownController {

    private final DropdownService dropdownService;

    @GetMapping("/departments")
    @Operation(summary = "Get selectable departments within user scope", description = "Returns lightweight department options with optional search, sorting, and pagination")
    public ResponseEntity<ApiResponse<PageResponse<DepartmentDropdownDto>>> getDepartments(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "activeOnly", defaultValue = "true") Boolean activeOnly,
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<DepartmentDropdownDto> response = dropdownService.getDepartments(search, activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sub-departments")
    @Operation(summary = "Get selectable sub-departments within user scope", description = "Returns lightweight sub-departments filtered by departmentId and scoped to user's permissions")
    public ResponseEntity<ApiResponse<PageResponse<SubDepartmentDropdownDto>>> getSubDepartments(
            @RequestParam(value = "departmentId", required = false) UUID departmentId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "activeOnly", defaultValue = "true") Boolean activeOnly,
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<SubDepartmentDropdownDto> response = dropdownService.getSubDepartments(departmentId, search, activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/users")
    @Operation(summary = "Get selectable users within user scope", description = "Returns lightweight user options filtered by department, sub-department, role, or search keyword")
    public ResponseEntity<ApiResponse<PageResponse<UserDropdownDto>>> getUsers(
            @RequestParam(value = "departmentId", required = false) UUID departmentId,
            @RequestParam(value = "subDepartmentId", required = false) UUID subDepartmentId,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "activeOnly", defaultValue = "true") Boolean activeOnly,
            @PageableDefault(size = 50, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<UserDropdownDto> response = dropdownService.getUsers(departmentId, subDepartmentId, role, search, activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/users/eligible-assignees")
    @Operation(summary = "Get eligible task assignees", description = "Returns selectable users who can be assigned tasks in the given department or sub-department context")
    public ResponseEntity<ApiResponse<PageResponse<UserDropdownDto>>> getEligibleAssignees(
            @RequestParam(value = "departmentId", required = false) UUID departmentId,
            @RequestParam(value = "subDepartmentId", required = false) UUID subDepartmentId,
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 50, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<UserDropdownDto> response = dropdownService.getEligibleAssignees(departmentId, subDepartmentId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/users/eligible-reporting-managers")
    @Operation(summary = "Get eligible reporting managers", description = "Returns selectable managers (HOD, Admin, Super Admin) whom users can report to or request approvals from")
    public ResponseEntity<ApiResponse<PageResponse<UserDropdownDto>>> getEligibleReportingManagers(
            @RequestParam(value = "departmentId", required = false) UUID departmentId,
            @RequestParam(value = "subDepartmentId", required = false) UUID subDepartmentId,
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 50, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<UserDropdownDto> response = dropdownService.getEligibleReportingManagers(departmentId, subDepartmentId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/task-templates")
    @Operation(summary = "Get selectable task templates within user scope", description = "Returns lightweight task templates applicable to caller's scope")
    public ResponseEntity<ApiResponse<PageResponse<TaskTemplateDropdownDto>>> getTaskTemplates(
            @RequestParam(value = "departmentId", required = false) UUID departmentId,
            @RequestParam(value = "subDepartmentId", required = false) UUID subDepartmentId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "activeOnly", defaultValue = "true") Boolean activeOnly,
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<TaskTemplateDropdownDto> response = dropdownService.getTaskTemplates(departmentId, subDepartmentId, search, activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/tasks")
    @Operation(summary = "Get selectable tasks within user scope", description = "Returns lightweight task items for task linking, parent selection, or filtering")
    public ResponseEntity<ApiResponse<PageResponse<TaskDropdownDto>>> getTasks(
            @RequestParam(value = "departmentId", required = false) UUID departmentId,
            @RequestParam(value = "subDepartmentId", required = false) UUID subDepartmentId,
            @RequestParam(value = "status", required = false) TaskStatus status,
            @RequestParam(value = "priority", required = false) TaskPriority priority,
            @RequestParam(value = "isSelfTask", required = false) Boolean isSelfTask,
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 50, sort = "dueDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<TaskDropdownDto> response = dropdownService.getTasks(departmentId, subDepartmentId, status, priority, isSelfTask, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/roles")
    @Operation(summary = "Get selectable roles", description = "Returns selectable roles governed by caller's privilege level")
    public ResponseEntity<ApiResponse<PageResponse<RoleDropdownDto>>> getRoles(
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<RoleDropdownDto> response = dropdownService.getRoles(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/permissions")
    @Operation(summary = "Get selectable permissions", description = "Returns selectable system permissions grouped by resource or action")
    public ResponseEntity<ApiResponse<PageResponse<PermissionDropdownDto>>> getPermissions(
            @RequestParam(value = "resource", required = false) String resource,
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 100, sort = "authority", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<PermissionDropdownDto> response = dropdownService.getPermissions(resource, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/task-statuses")
    @Operation(summary = "Get static task status options", description = "Returns allowable lifecycle task statuses")
    public ResponseEntity<ApiResponse<List<StaticOptionDto>>> getTaskStatuses() {
        return ResponseEntity.ok(ApiResponse.success(dropdownService.getTaskStatuses()));
    }

    @GetMapping("/request-statuses")
    @Operation(summary = "Get static request status options", description = "Returns allowable task request statuses")
    public ResponseEntity<ApiResponse<List<StaticOptionDto>>> getRequestStatuses() {
        return ResponseEntity.ok(ApiResponse.success(dropdownService.getRequestStatuses()));
    }

    @GetMapping("/task-priorities")
    @Operation(summary = "Get static task priority options", description = "Returns allowable task priority values")
    public ResponseEntity<ApiResponse<List<StaticOptionDto>>> getTaskPriorities() {
        return ResponseEntity.ok(ApiResponse.success(dropdownService.getTaskPriorities()));
    }

    @GetMapping("/proof-requirement-types")
    @Operation(summary = "Get static proof requirement attachment types", description = "Returns allowable proof attachment format types")
    public ResponseEntity<ApiResponse<List<StaticOptionDto>>> getProofRequirementTypes() {
        return ResponseEntity.ok(ApiResponse.success(dropdownService.getProofRequirementTypes()));
    }

    @GetMapping("/data-scopes")
    @Operation(summary = "Get static data scope types", description = "Returns allowable RBAC data scope types")
    public ResponseEntity<ApiResponse<List<StaticOptionDto>>> getDataScopes() {
        return ResponseEntity.ok(ApiResponse.success(dropdownService.getDataScopeTypes()));
    }
}
