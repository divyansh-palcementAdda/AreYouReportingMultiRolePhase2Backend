package com.app.AreYouReporting.service.interfaces;

import com.app.AreYouReporting.Entities.TaskCompletionOutcome;
import com.app.AreYouReporting.Entities.TaskPriority;
import com.app.AreYouReporting.Entities.TaskStatus;
import com.app.AreYouReporting.payload.request.SelfTaskCreateRequest;
import com.app.AreYouReporting.payload.request.TaskCreateRequest;
import com.app.AreYouReporting.payload.request.TaskStatusUpdateRequest;
import com.app.AreYouReporting.payload.request.TaskUpdateRequest;
import com.app.AreYouReporting.payload.response.PageResponse;
import com.app.AreYouReporting.payload.response.TaskDto;
import com.app.AreYouReporting.payload.response.TaskSummaryDto;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TaskService {

    PageResponse<TaskSummaryDto> getTasks(
            String search,
            List<TaskStatus> statuses,
            TaskPriority priority,
            Boolean isSelfTask,
            TaskCompletionOutcome completionOutcome,
            Boolean isCompletedAfterDelay,
            Boolean isCompletedAfterExtension,
            UUID departmentId,
            UUID subDepartmentId,
            Instant fromDate,
            Instant toDate,
            Pageable pageable
    );

    TaskDto getTaskById(UUID id);

    TaskDto createTask(TaskCreateRequest request);

    TaskDto createSelfTask(SelfTaskCreateRequest request);

    TaskDto updateTask(UUID id, TaskUpdateRequest request);

    TaskDto updateTaskStatus(UUID id, TaskStatusUpdateRequest request);

    TaskDto startTask(UUID id);

    TaskDto closeTask(UUID id);

    void deleteTask(UUID id);
}
