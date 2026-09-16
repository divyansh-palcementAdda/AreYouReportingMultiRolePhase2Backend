package com.app.AreYouReporting.payload.response;

import com.app.AreYouReporting.Entities.TaskCompletionOutcome;
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
public class TaskSummaryDto {

    private UUID id;
    private String title;
    private TaskPriority priority;
    private TaskStatus status;
    private Instant startDate;
    private Instant dueDate;
    private boolean isSelfTask;
    private UserSummaryDto creator;
    private boolean isCompletedAfterDelay;
    private boolean isCompletedAfterExtension;
    private TaskCompletionOutcome completionOutcome;
    private Instant createdAt;
}
