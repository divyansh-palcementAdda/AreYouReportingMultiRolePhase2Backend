package com.app.AreYouReporting.payload.request;

import com.app.AreYouReporting.Entities.TaskPriority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateRequest {

    private String title;
    private String description;
    private TaskPriority priority;
    private Instant startDate;
    private Instant dueDate;
    private Set<UUID> assignedDepartmentIds;
    private Set<UUID> assignedSubDepartmentIds;
    private Set<UUID> assigneeIds;
    private Double targetCount;
    private Double currentCount;
    private Double targetPercentage;
    private Double currentPercentage;
}
