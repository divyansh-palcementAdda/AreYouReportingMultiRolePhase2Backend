package com.app.AreYouReporting.payload.response;

import com.app.AreYouReporting.Entities.ProofType;
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
public class TaskProofDto {

    private UUID id;
    private UUID taskId;
    private UUID taskRequestId;
    private UUID proofRequirementId;
    private String proofRequirementName;
    private ProofType proofType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String description;
    private UserSummaryDto uploadedBy;
    private Instant uploadedAt;
}
