package com.app.AreYouReporting.payload.request;

import com.app.AreYouReporting.Entities.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusUpdateRequest {

    @NotNull(message = "Task status is required")
    private TaskStatus status;

    private String remarks;
}
