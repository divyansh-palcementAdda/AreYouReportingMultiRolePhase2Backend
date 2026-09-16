package com.app.AreYouReporting.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskProofRequirementDto {

    private UUID id;

    @NotBlank(message = "Requirement name is required")
    private String name;

    private String description;
    private Boolean isRequired;
    private String acceptedProofTypes;
    private Integer minCount;
    private Integer displayOrder;
}
