package com.app.AreYouReporting.mapper;

import com.app.AreYouReporting.Entities.TaskProofRequirement;
import com.app.AreYouReporting.Entities.TaskTemplate;
import com.app.AreYouReporting.payload.request.TaskProofRequirementDto;
import com.app.AreYouReporting.payload.response.TaskTemplateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskTemplateMapper {

    private final DepartmentMapper departmentMapper;

    public TaskProofRequirementDto toRequirementDto(TaskProofRequirement req) {
        if (req == null) return null;
        return TaskProofRequirementDto.builder()
                .id(req.getId())
                .name(req.getName())
                .description(req.getDescription())
                .isRequired(req.isRequired())
                .acceptedProofTypes(req.getAcceptedProofTypes())
                .minCount(req.getMinCount())
                .displayOrder(req.getDisplayOrder())
                .build();
    }

    public List<TaskProofRequirementDto> toRequirementDtoList(Collection<TaskProofRequirement> requirements) {
        if (requirements == null) return Collections.emptyList();
        return requirements.stream().map(this::toRequirementDto).collect(Collectors.toList());
    }

    public TaskTemplateDto toDto(TaskTemplate template) {
        if (template == null) return null;
        return TaskTemplateDto.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .defaultPriority(template.getDefaultPriority())
                .defaultDurationDays(template.getDefaultDurationDays())
                .defaultTargetCount(template.getDefaultTargetCount())
                .defaultTargetPercentage(template.getDefaultTargetPercentage())
                .isActive(template.isActive())
                .applicableDepartments(template.getApplicableDepartments() != null ? departmentMapper.toDtoList(template.getApplicableDepartments()) : Collections.emptyList())
                .applicableSubDepartments(template.getApplicableSubDepartments() != null ? departmentMapper.toSubDeptDtoList(template.getApplicableSubDepartments()) : Collections.emptyList())
                .proofRequirements(template.getProofRequirements() != null ? toRequirementDtoList(template.getProofRequirements()) : Collections.emptyList())
                .build();
    }

    public List<TaskTemplateDto> toDtoList(Collection<TaskTemplate> templates) {
        if (templates == null) return Collections.emptyList();
        return templates.stream().map(this::toDto).collect(Collectors.toList());
    }
}
