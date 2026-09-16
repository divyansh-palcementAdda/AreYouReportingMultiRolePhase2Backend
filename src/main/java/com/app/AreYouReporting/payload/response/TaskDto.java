package com.app.AreYouReporting.payload.response;

import com.app.AreYouReporting.Entities.TaskCompletionOutcome;
import com.app.AreYouReporting.Entities.TaskPriority;
import com.app.AreYouReporting.Entities.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {

    private UUID id;
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private Instant startDate;
    private Instant dueDate;
    private Instant originalDueDate;
    private boolean isSelfTask;
    private UserSummaryDto creator;
    private List<DepartmentDto> assignedDepartments;
    private List<SubDepartmentDto> assignedSubDepartments;
    private List<UserSummaryDto> assignees;

    // Lifecycle Auditing & Timestamp Tracking
    private UserSummaryDto startedBy;
    private Instant startedAt;
    private UserSummaryDto closedBy;
    private Instant closedAt;
    private UserSummaryDto extendedBy;
    private Instant extendedAt;
    private Instant lastExtendedDueDate;
    private int extensionCount;

    // Delayed & Extension Completion Metrics
    private boolean isCompletedAfterDelay;
    private boolean isCompletedAfterExtension;
    private TaskCompletionOutcome completionOutcome;

    // Request Rejection Metrics
    private int extensionRejectionCount;
    private int closureRejectionCount;
    private int totalRejectionCount;
    private String lastRejectionReason;
    private Instant lastRejectedAt;
    private UserSummaryDto lastRejectedBy;

    // Template and Target Metrics
    private TaskTemplateDto template;
    private Double targetCount;
    private Double currentCount;
    private Double targetPercentage;
    private Double currentPercentage;

    private List<TaskProofDto> proofs;
    private List<TaskRequestDto> requests;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
}
