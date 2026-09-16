package com.app.AreYouReporting.payload.request;

import com.app.AreYouReporting.Entities.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplateCreateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    private String description;
    private TaskPriority defaultPriority;
    private Integer defaultDurationDays;
    private Double defaultTargetCount;
    private Double defaultTargetPercentage;
    private Boolean isActive;
    private Set<UUID> applicableDepartmentIds;
    private Set<UUID> applicableSubDepartmentIds;
    private List<TaskProofRequirementDto> proofRequirements;
}
