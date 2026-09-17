package com.app.AreYouReporting.payload.response.dropdown;

import com.app.AreYouReporting.Entities.TaskPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplateDropdownDto {

    private UUID id;
    private String label;
    private String name;
    private TaskPriority defaultPriority;
    private Integer defaultDurationDays;
    private Double defaultTargetCount;
    private Double defaultTargetPercentage;
    private boolean active;
    private Set<UUID> applicableDepartmentIds;
    private Set<UUID> applicableSubDepartmentIds;
}
