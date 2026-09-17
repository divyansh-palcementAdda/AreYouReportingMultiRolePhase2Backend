package com.app.AreYouReporting.controller;

import com.app.AreYouReporting.payload.request.DepartmentRequest;
import com.app.AreYouReporting.payload.request.SubDepartmentRequest;
import com.app.AreYouReporting.payload.response.ApiResponse;
import com.app.AreYouReporting.payload.response.DepartmentDto;
import com.app.AreYouReporting.payload.response.SubDepartmentDto;
import com.app.AreYouReporting.service.interfaces.DepartmentService;
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
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Department Hierarchy Management", description = "Endpoints for managing parent Departments and child SubDepartments")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @Operation(summary = "Get all departments (with child sub-departments)")
    public ResponseEntity<ApiResponse<List<DepartmentDto>>> getAllDepartments(
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        List<DepartmentDto> departments = departmentService.getAllDepartments(activeOnly);
        return ResponseEntity.ok(ApiResponse.success(departments));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID")
    public ResponseEntity<ApiResponse<DepartmentDto>> getDepartmentById(@PathVariable("id") UUID id) {
        DepartmentDto department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.success(department));
    }

    @PostMapping
    @Operation(summary = "Create parent department")
    public ResponseEntity<ApiResponse<DepartmentDto>> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        DepartmentDto department = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Department created successfully", department));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update department details")
    public ResponseEntity<ApiResponse<DepartmentDto>> updateDepartment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody DepartmentRequest request) {
        DepartmentDto department = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Department updated successfully", department));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate department")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable("id") UUID id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.success("Department deactivated successfully", null));
    }

    // Sub-Department Endpoints
    @GetMapping("/{deptId}/sub-departments")
    @Operation(summary = "Get all sub-departments belonging to a department")
    public ResponseEntity<ApiResponse<List<SubDepartmentDto>>> getSubDepartmentsByDeptId(
            @PathVariable("deptId") UUID deptId,
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        List<SubDepartmentDto> subDepartments = departmentService.getSubDepartmentsByDeptId(deptId, activeOnly);
        return ResponseEntity.ok(ApiResponse.success(subDepartments));
    }

    @GetMapping("/sub-departments/{subDeptId}")
    @Operation(summary = "Get sub-department by ID")
    public ResponseEntity<ApiResponse<SubDepartmentDto>> getSubDepartmentById(@PathVariable("subDeptId") UUID subDeptId) {
        SubDepartmentDto subDepartment = departmentService.getSubDepartmentById(subDeptId);
        return ResponseEntity.ok(ApiResponse.success(subDepartment));
    }

    @PostMapping("/{deptId}/sub-departments")
    @Operation(summary = "Create child sub-department under parent department")
    public ResponseEntity<ApiResponse<SubDepartmentDto>> createSubDepartment(
            @PathVariable("deptId") UUID deptId,
            @Valid @RequestBody SubDepartmentRequest request) {
        SubDepartmentDto subDepartment = departmentService.createSubDepartment(deptId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("SubDepartment created successfully", subDepartment));
    }

    @PutMapping("/sub-departments/{subDeptId}")
    @Operation(summary = "Update sub-department details")
    public ResponseEntity<ApiResponse<SubDepartmentDto>> updateSubDepartment(
            @PathVariable("subDeptId") UUID subDeptId,
            @Valid @RequestBody SubDepartmentRequest request) {
        SubDepartmentDto subDepartment = departmentService.updateSubDepartment(subDeptId, request);
        return ResponseEntity.ok(ApiResponse.success("SubDepartment updated successfully", subDepartment));
    }

    @DeleteMapping("/sub-departments/{subDeptId}")
    @Operation(summary = "Deactivate sub-department")
    public ResponseEntity<ApiResponse<Void>> deleteSubDepartment(@PathVariable("subDeptId") UUID subDeptId) {
        departmentService.deleteSubDepartment(subDeptId);
        return ResponseEntity.ok(ApiResponse.success("SubDepartment deactivated successfully", null));
    }
}
