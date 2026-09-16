package com.app.AreYouReporting.service.interfaces;

import com.app.AreYouReporting.payload.request.TaskProofRequirementDto;
import com.app.AreYouReporting.payload.request.TaskTemplateCreateRequest;
import com.app.AreYouReporting.payload.response.TaskTemplateDto;

import java.util.List;
import java.util.UUID;

public interface TaskTemplateService {

    List<TaskTemplateDto> getAllTemplates(boolean activeOnly);

    TaskTemplateDto getTemplateById(UUID id);

    TaskTemplateDto createTemplate(TaskTemplateCreateRequest request);

    TaskTemplateDto updateTemplate(UUID id, TaskTemplateCreateRequest request);

    void deleteTemplate(UUID id);

    TaskTemplateDto addProofRequirement(UUID templateId, TaskProofRequirementDto dto);

    void deleteProofRequirement(UUID requirementId);
}
