package com.app.AreYouReporting.controller;

import com.app.AreYouReporting.Entities.ProofType;
import com.app.AreYouReporting.payload.request.TaskProofUploadRequest;
import com.app.AreYouReporting.payload.response.ApiResponse;
import com.app.AreYouReporting.payload.response.TaskProofDto;
import com.app.AreYouReporting.service.interfaces.TaskProofService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/task-proofs")
@RequiredArgsConstructor
@Tag(name = "Task Proofs & Attachments", description = "Endpoints for uploading, downloading, and managing task completion proofs and requirement fulfillment")
public class TaskProofController {

    private final TaskProofService proofService;

    @PostMapping(value = "/tasks/{taskId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload proof file or submit link for a task")
    public ResponseEntity<ApiResponse<TaskProofDto>> uploadProof(
            @PathVariable("taskId") UUID taskId,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam("proofType") ProofType proofType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "proofRequirementId", required = false) UUID proofRequirementId,
            @RequestParam(value = "taskRequestId", required = false) UUID taskRequestId,
            @RequestParam(value = "externalLinkUrl", required = false) String externalLinkUrl) {

        TaskProofUploadRequest request = new TaskProofUploadRequest(proofType, description, proofRequirementId, taskRequestId, externalLinkUrl);
        TaskProofDto proof = proofService.uploadProof(taskId, file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Proof uploaded successfully", proof));
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "Get all proofs submitted for a task")
    public ResponseEntity<ApiResponse<List<TaskProofDto>>> getProofsByTaskId(@PathVariable("taskId") UUID taskId) {
        List<TaskProofDto> proofs = proofService.getProofsByTaskId(taskId);
        return ResponseEntity.ok(ApiResponse.success(proofs));
    }

    @GetMapping("/{proofId}/download")
    @Operation(summary = "Download proof file")
    public ResponseEntity<Resource> downloadProofFile(@PathVariable("proofId") UUID proofId) {
        Resource resource = proofService.downloadProofFile(proofId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{proofId}")
    @Operation(summary = "Delete an uploaded proof")
    public ResponseEntity<ApiResponse<Void>> deleteProof(@PathVariable("proofId") UUID proofId) {
        proofService.deleteProof(proofId);
        return ResponseEntity.ok(ApiResponse.success("Proof deleted successfully", null));
    }
}
