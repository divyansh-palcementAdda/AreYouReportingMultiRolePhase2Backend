package com.app.AreYouReporting.controller;

import com.app.AreYouReporting.Entities.TaskCompletionOutcome;
import com.app.AreYouReporting.Entities.TaskPriority;
import com.app.AreYouReporting.Entities.TaskStatus;
import com.app.AreYouReporting.payload.request.SelfTaskCreateRequest;
import com.app.AreYouReporting.payload.request.TaskCreateRequest;
import com.app.AreYouReporting.payload.request.TaskStatusUpdateRequest;
import com.app.AreYouReporting.payload.request.TaskUpdateRequest;
import com.app.AreYouReporting.payload.response.ApiResponse;
import com.app.AreYouReporting.payload.response.PageResponse;
import com.app.AreYouReporting.payload.response.TaskDto;
import com.app.AreYouReporting.payload.response.TaskSummaryDto;
import com.app.AreYouReporting.service.interfaces.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Management & Lifecycle", description = "Endpoints for creating, managing, searching, filtering, and executing task lifecycle transitions")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Search and filter tasks with dynamic role & data scope authorization")
    public ResponseEntity<ApiResponse<PageResponse<TaskSummaryDto>>> getTasks(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "statuses", required = false) List<TaskStatus> statuses,
            @RequestParam(value = "priority", required = false) TaskPriority priority,
            @RequestParam(value = "isSelfTask", required = false) Boolean isSelfTask,
            @RequestParam(value = "completionOutcome", required = false) TaskCompletionOutcome completionOutcome,
            @RequestParam(value = "isCompletedAfterDelay", required = false) Boolean isCompletedAfterDelay,
            @RequestParam(value = "isCompletedAfterExtension", required = false) Boolean isCompletedAfterExtension,
            @RequestParam(value = "departmentId", required = false) UUID departmentId,
            @RequestParam(value = "subDepartmentId", required = false) UUID subDepartmentId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<TaskSummaryDto> tasks = taskService.getTasks(
                search, statuses, priority, isSelfTask, completionOutcome,
                isCompletedAfterDelay, isCompletedAfterExtension,
                departmentId, subDepartmentId, fromDate, toDate, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed task by ID including proofs, requests, and target metrics")
    public ResponseEntity<ApiResponse<TaskDto>> getTaskById(@PathVariable("id") UUID id) {
        TaskDto task = taskService.getTaskById(id);
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @PostMapping
    @Operation(summary = "Create task with multi-department, multi-subdepartment, and multi-user assignments")
    public ResponseEntity<ApiResponse<TaskDto>> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskDto task = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Task created successfully", task));
    }

    @PostMapping("/self-task")
    @Operation(summary = "Create personal self-task for tracking and reporting by any authenticated user")
    public ResponseEntity<ApiResponse<TaskDto>> createSelfTask(@Valid @RequestBody SelfTaskCreateRequest request) {
        TaskDto task = taskService.createSelfTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Self-task created successfully", task));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task details, target metrics, or assignees")
    public ResponseEntity<ApiResponse<TaskDto>> updateTask(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TaskUpdateRequest request) {
        TaskDto task = taskService.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", task));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update task status")
    public ResponseEntity<ApiResponse<TaskDto>> updateTaskStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TaskStatusUpdateRequest request) {
        TaskDto task = taskService.updateTaskStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Task status updated successfully", task));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start working on a task (sets startedBy, startedAt, and transitions status to IN_PROGRESS)")
    public ResponseEntity<ApiResponse<TaskDto>> startTask(@PathVariable("id") UUID id) {
        TaskDto task = taskService.startTask(id);
        return ResponseEntity.ok(ApiResponse.success("Task started successfully", task));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Directly close task (validates required proofs, records closedBy, closedAt, and sets completion metrics)")
    public ResponseEntity<ApiResponse<TaskDto>> closeTask(@PathVariable("id") UUID id) {
        TaskDto task = taskService.closeTask(id);
        return ResponseEntity.ok(ApiResponse.success("Task closed successfully", task));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete task (transitions status to DELETED and logs audit event)")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable("id") UUID id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }
}
