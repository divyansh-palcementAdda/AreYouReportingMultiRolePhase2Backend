package com.app.AreYouReporting.payload.request;

import com.app.AreYouReporting.Entities.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    @NotNull(message = "Priority is required")
    private TaskPriority priority;

    private Instant startDate;

    @NotNull(message = "Due date is required")
    private Instant dueDate;

    private Set<UUID> assignedDepartmentIds;
    private Set<UUID> assignedSubDepartmentIds;
    private Set<UUID> assigneeIds;

    private UUID templateId;
    private Double targetCount;
    private Double targetPercentage;
}
