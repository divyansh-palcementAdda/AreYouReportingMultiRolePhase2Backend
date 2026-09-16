package com.app.AreYouReporting.payload.request;

import com.app.AreYouReporting.Entities.ProofType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskProofUploadRequest {

    @NotNull(message = "Proof type is required")
    private ProofType proofType;

    private String description;
    private UUID proofRequirementId;
    private UUID taskRequestId;
    private String externalLinkUrl;
}
