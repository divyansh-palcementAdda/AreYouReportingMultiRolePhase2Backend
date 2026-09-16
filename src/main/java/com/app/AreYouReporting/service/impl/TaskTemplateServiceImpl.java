package com.app.AreYouReporting.service.impl;

import com.app.AreYouReporting.Entities.AuditStatus;
import com.app.AreYouReporting.Entities.Department;
import com.app.AreYouReporting.Entities.SubDepartment;
import com.app.AreYouReporting.Entities.TaskProofRequirement;
import com.app.AreYouReporting.Entities.TaskTemplate;
import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ResourceNotFoundException;
import com.app.AreYouReporting.mapper.TaskTemplateMapper;
import com.app.AreYouReporting.payload.request.TaskProofRequirementDto;
import com.app.AreYouReporting.payload.request.TaskTemplateCreateRequest;
import com.app.AreYouReporting.payload.response.TaskTemplateDto;
import com.app.AreYouReporting.repository.DepartmentRepository;
import com.app.AreYouReporting.repository.SubDepartmentRepository;
import com.app.AreYouReporting.repository.TaskProofRequirementRepository;
import com.app.AreYouReporting.repository.TaskTemplateRepository;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.service.interfaces.AuditService;
import com.app.AreYouReporting.service.interfaces.TaskTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskTemplateServiceImpl implements TaskTemplateService {

    private final TaskTemplateRepository taskTemplateRepository;
    private final TaskProofRequirementRepository proofRequirementRepository;
    private final DepartmentRepository departmentRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final TaskTemplateMapper taskTemplateMapper;
    private final AuditService auditService;
    private final ScopeAuthorizationService scopeSecurity;

    @Override
    @Transactional(readOnly = true)
    public List<TaskTemplateDto> getAllTemplates(boolean activeOnly) {
        List<TaskTemplate> list = activeOnly ? taskTemplateRepository.findByIsActiveTrue() : taskTemplateRepository.findAll();
        return taskTemplateMapper.toDtoList(list);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskTemplateDto getTemplateById(UUID id) {
        TaskTemplate template = taskTemplateRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaskTemplate", "id", id));
        return taskTemplateMapper.toDto(template);
    }

    @Override
    @Transactional
    public TaskTemplateDto createTemplate(TaskTemplateCreateRequest request) {
        scopeSecurity.validatePermission("templates.create");

        if (taskTemplateRepository.existsByName(request.getName().trim())) {
            throw new InvalidOperationException("Template with name '" + request.getName() + "' already exists");
        }

        Set<Department> depts = new HashSet<>();
        if (request.getApplicableDepartmentIds() != null && !request.getApplicableDepartmentIds().isEmpty()) {
            depts.addAll(departmentRepository.findAllById(request.getApplicableDepartmentIds()));
        }

        Set<SubDepartment> subDepts = new HashSet<>();
        if (request.getApplicableSubDepartmentIds() != null && !request.getApplicableSubDepartmentIds().isEmpty()) {
            subDepts.addAll(subDepartmentRepository.findAllById(request.getApplicableSubDepartmentIds()));
        }

        TaskTemplate template = TaskTemplate.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .defaultPriority(request.getDefaultPriority())
                .defaultDurationDays(request.getDefaultDurationDays())
                .defaultTargetCount(request.getDefaultTargetCount())
                .defaultTargetPercentage(request.getDefaultTargetPercentage())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .applicableDepartments(depts)
                .applicableSubDepartments(subDepts)
                .build();

        TaskTemplate saved = taskTemplateRepository.save(template);

        if (request.getProofRequirements() != null && !request.getProofRequirements().isEmpty()) {
            for (TaskProofRequirementDto reqDto : request.getProofRequirements()) {
                TaskProofRequirement pr = TaskProofRequirement.builder()
                        .template(saved)
                        .name(reqDto.getName().trim())
                        .description(reqDto.getDescription())
                        .isRequired(reqDto.getIsRequired() != null ? reqDto.getIsRequired() : true)
                        .acceptedProofTypes(reqDto.getAcceptedProofTypes() != null ? reqDto.getAcceptedProofTypes() : "DOCUMENT,IMAGE,LINK,SPREADSHEET,OTHER")
                        .minCount(reqDto.getMinCount() != null ? reqDto.getMinCount() : 1)
                        .displayOrder(reqDto.getDisplayOrder() != null ? reqDto.getDisplayOrder() : 0)
                        .build();
                proofRequirementRepository.save(pr);
                saved.getProofRequirements().add(pr);
            }
        }

        auditService.log(scopeSecurity.getCurrentUsername(), "TEMPLATE", "CREATE_TEMPLATE", "TEMPLATE", saved.getId().toString(), null, null, saved.getName(), AuditStatus.SUCCESS, null);
        return taskTemplateMapper.toDto(saved);
    }

    @Override
    @Transactional
    public TaskTemplateDto updateTemplate(UUID id, TaskTemplateCreateRequest request) {
        scopeSecurity.validatePermission("templates.update");

        TaskTemplate template = taskTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaskTemplate", "id", id));

        if (request.getName() != null && !request.getName().isBlank() && !request.getName().equalsIgnoreCase(template.getName())) {
            if (taskTemplateRepository.existsByName(request.getName().trim())) {
                throw new InvalidOperationException("Template with name '" + request.getName() + "' already exists");
            }
            template.setName(request.getName().trim());
        }

        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getDefaultPriority() != null) template.setDefaultPriority(request.getDefaultPriority());
        if (request.getDefaultDurationDays() != null) template.setDefaultDurationDays(request.getDefaultDurationDays());
        if (request.getDefaultTargetCount() != null) template.setDefaultTargetCount(request.getDefaultTargetCount());
        if (request.getDefaultTargetPercentage() != null) template.setDefaultTargetPercentage(request.getDefaultTargetPercentage());
        if (request.getIsActive() != null) template.setActive(request.getIsActive());

        if (request.getApplicableDepartmentIds() != null) {
            template.setApplicableDepartments(new HashSet<>(departmentRepository.findAllById(request.getApplicableDepartmentIds())));
        }
        if (request.getApplicableSubDepartmentIds() != null) {
            template.setApplicableSubDepartments(new HashSet<>(subDepartmentRepository.findAllById(request.getApplicableSubDepartmentIds())));
        }

        TaskTemplate updated = taskTemplateRepository.save(template);
        auditService.log(scopeSecurity.getCurrentUsername(), "TEMPLATE", "UPDATE_TEMPLATE", "TEMPLATE", updated.getId().toString(), null, null, updated.getName(), AuditStatus.SUCCESS, null);
        return taskTemplateMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID id) {
        scopeSecurity.validatePermission("templates.delete");

        TaskTemplate template = taskTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaskTemplate", "id", id));

        template.setActive(false);
        taskTemplateRepository.save(template);
        auditService.log(scopeSecurity.getCurrentUsername(), "TEMPLATE", "DEACTIVATE_TEMPLATE", "TEMPLATE", id.toString(), null, template.getName(), null, AuditStatus.SUCCESS, null);
    }

    @Override
    @Transactional
    public TaskTemplateDto addProofRequirement(UUID templateId, TaskProofRequirementDto dto) {
        scopeSecurity.validatePermission("templates.update");

        TaskTemplate template = taskTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("TaskTemplate", "id", templateId));

        TaskProofRequirement pr = TaskProofRequirement.builder()
                .template(template)
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .isRequired(dto.getIsRequired() != null ? dto.getIsRequired() : true)
                .acceptedProofTypes(dto.getAcceptedProofTypes() != null ? dto.getAcceptedProofTypes() : "DOCUMENT,IMAGE,LINK,SPREADSHEET,OTHER")
                .minCount(dto.getMinCount() != null ? dto.getMinCount() : 1)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .build();

        proofRequirementRepository.save(pr);
        return getTemplateById(templateId);
    }

    @Override
    @Transactional
    public void deleteProofRequirement(UUID requirementId) {
        scopeSecurity.validatePermission("templates.update");

        TaskProofRequirement pr = proofRequirementRepository.findById(requirementId)
                .orElseThrow(() -> new ResourceNotFoundException("TaskProofRequirement", "id", requirementId));

        proofRequirementRepository.delete(pr);
    }
}
