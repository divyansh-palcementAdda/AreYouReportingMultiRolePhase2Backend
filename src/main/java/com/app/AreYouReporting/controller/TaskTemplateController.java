package com.app.AreYouReporting.controller;

import com.app.AreYouReporting.payload.request.TaskProofRequirementDto;
import com.app.AreYouReporting.payload.request.TaskTemplateCreateRequest;
import com.app.AreYouReporting.payload.response.ApiResponse;
import com.app.AreYouReporting.payload.response.TaskTemplateDto;
import com.app.AreYouReporting.service.interfaces.TaskTemplateService;
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
@RequestMapping("/api/v1/task-templates")
@RequiredArgsConstructor
@Tag(name = "Dynamic Task Templates & Proof Requirements", description = "Endpoints for managing reusable task templates, target metrics, and dynamic proof requirements")
public class TaskTemplateController {

    private final TaskTemplateService templateService;

    @GetMapping
    @Operation(summary = "Get all task templates")
    public ResponseEntity<ApiResponse<List<TaskTemplateDto>>> getAllTemplates(
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        List<TaskTemplateDto> templates = templateService.getAllTemplates(activeOnly);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task template by ID with proof requirements")
    public ResponseEntity<ApiResponse<TaskTemplateDto>> getTemplateById(@PathVariable("id") UUID id) {
        TaskTemplateDto template = templateService.getTemplateById(id);
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    @PostMapping
    @Operation(summary = "Create task template with dynamic proof requirements and target metrics")
    public ResponseEntity<ApiResponse<TaskTemplateDto>> createTemplate(@Valid @RequestBody TaskTemplateCreateRequest request) {
        TaskTemplateDto template = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Template created successfully", template));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task template")
    public ResponseEntity<ApiResponse<TaskTemplateDto>> updateTemplate(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TaskTemplateCreateRequest request) {
        TaskTemplateDto template = templateService.updateTemplate(id, request);
        return ResponseEntity.ok(ApiResponse.success("Template updated successfully", template));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate task template")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable("id") UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deactivated successfully", null));
    }

    @PostMapping("/{templateId}/proof-requirements")
    @Operation(summary = "Add dynamic proof requirement to a template")
    public ResponseEntity<ApiResponse<TaskTemplateDto>> addProofRequirement(
            @PathVariable("templateId") UUID templateId,
            @Valid @RequestBody TaskProofRequirementDto dto) {
        TaskTemplateDto template = templateService.addProofRequirement(templateId, dto);
        return ResponseEntity.ok(ApiResponse.success("Proof requirement added successfully", template));
    }

    @DeleteMapping("/proof-requirements/{requirementId}")
    @Operation(summary = "Delete dynamic proof requirement")
    public ResponseEntity<ApiResponse<Void>> deleteProofRequirement(@PathVariable("requirementId") UUID requirementId) {
        templateService.deleteProofRequirement(requirementId);
        return ResponseEntity.ok(ApiResponse.success("Proof requirement deleted successfully", null));
    }
}
