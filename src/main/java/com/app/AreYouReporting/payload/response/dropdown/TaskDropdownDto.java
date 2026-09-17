package com.app.AreYouReporting.payload.response.dropdown;

import com.app.AreYouReporting.Entities.TaskPriority;
import com.app.AreYouReporting.Entities.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDropdownDto {

    private UUID id;
    private String label;
    private String title;
    private TaskStatus status;
    private TaskPriority priority;
    private Instant dueDate;
    private boolean isSelfTask;
}
