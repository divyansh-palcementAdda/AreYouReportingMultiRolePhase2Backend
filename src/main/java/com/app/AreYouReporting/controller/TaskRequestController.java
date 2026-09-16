package com.app.AreYouReporting.controller;

import com.app.AreYouReporting.payload.request.TaskRequestReviewRequest;
import com.app.AreYouReporting.payload.request.TaskRequestSubmitRequest;
import com.app.AreYouReporting.payload.response.ApiResponse;
import com.app.AreYouReporting.payload.response.TaskRequestDto;
import com.app.AreYouReporting.service.interfaces.TaskRequestService;
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
@RequestMapping("/api/v1/task-requests")
@RequiredArgsConstructor
@Tag(name = "Task Request Workflows", description = "Endpoints for submitting and reviewing due date extension and task closure requests")
public class TaskRequestController {

    private final TaskRequestService requestService;

    @PostMapping("/tasks/{taskId}/submit")
    @Operation(summary = "Submit a due date extension or task closure request with reasons and optional proofs")
    public ResponseEntity<ApiResponse<TaskRequestDto>> submitRequest(
            @PathVariable("taskId") UUID taskId,
            @Valid @RequestBody TaskRequestSubmitRequest request) {
        TaskRequestDto result = requestService.submitRequest(taskId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Request submitted successfully", result));
    }

    @PostMapping("/{requestId}/review")
    @Operation(summary = "Approve or reject a task request (applies status updates, extension counts, or closure proofs validation)")
    public ResponseEntity<ApiResponse<TaskRequestDto>> reviewRequest(
            @PathVariable("requestId") UUID requestId,
            @Valid @RequestBody TaskRequestReviewRequest request) {
        TaskRequestDto result = requestService.reviewRequest(requestId, request);
        return ResponseEntity.ok(ApiResponse.success("Request reviewed successfully", result));
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "Get all requests for a specific task")
    public ResponseEntity<ApiResponse<List<TaskRequestDto>>> getRequestsByTaskId(@PathVariable("taskId") UUID taskId) {
        List<TaskRequestDto> requests = requestService.getRequestsByTaskId(taskId);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Get task request by ID")
    public ResponseEntity<ApiResponse<TaskRequestDto>> getRequestById(@PathVariable("requestId") UUID requestId) {
        TaskRequestDto request = requestService.getRequestById(requestId);
        return ResponseEntity.ok(ApiResponse.success(request));
    }

    @PostMapping("/{requestId}/cancel")
    @Operation(summary = "Cancel a pending task request")
    public ResponseEntity<ApiResponse<Void>> cancelRequest(@PathVariable("requestId") UUID requestId) {
        requestService.cancelRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success("Request cancelled successfully", null));
    }
}
