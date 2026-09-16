package com.app.AreYouReporting.payload.response;

import com.app.AreYouReporting.Entities.TaskPriority;
import com.app.AreYouReporting.payload.request.TaskProofRequirementDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplateDto {

    private UUID id;
    private String name;
    private String description;
    private TaskPriority defaultPriority;
    private Integer defaultDurationDays;
    private Double defaultTargetCount;
    private Double defaultTargetPercentage;
    private boolean isActive;
    private List<DepartmentDto> applicableDepartments;
    private List<SubDepartmentDto> applicableSubDepartments;
    private List<TaskProofRequirementDto> proofRequirements;
}
