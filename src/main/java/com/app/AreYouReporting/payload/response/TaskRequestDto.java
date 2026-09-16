package com.app.AreYouReporting.payload.response;

import com.app.AreYouReporting.Entities.TaskRequestStatus;
import com.app.AreYouReporting.Entities.TaskRequestType;
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
public class TaskRequestDto {

    private UUID id;
    private UUID taskId;
    private String taskTitle;
    private TaskRequestType requestType;
    private TaskRequestStatus status;
    private Instant requestedDueDate;
    private String reason;
    private UserSummaryDto reviewer;
    private Instant reviewedAt;
    private String reviewRemarks;
    private List<TaskProofDto> proofs;
    private Instant createdAt;
    private String createdBy;
}
