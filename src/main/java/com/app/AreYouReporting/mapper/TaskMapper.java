package com.app.AreYouReporting.mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.app.AreYouReporting.Entities.Task;
import com.app.AreYouReporting.payload.response.TaskDto;
import com.app.AreYouReporting.payload.response.TaskSummaryDto;
import com.app.AreYouReporting.payload.response.TaskTemplateDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TaskMapper {

    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final TaskTemplateMapper taskTemplateMapper;
    private final TaskProofMapper taskProofMapper;
    private final TaskRequestMapper taskRequestMapper;

    public TaskSummaryDto toSummaryDto(Task task) {
        if (task == null) return null;
        return TaskSummaryDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .priority(task.getPriority())
                .status(task.getStatus())
                .startDate(task.getStartDate())
                .dueDate(task.getDueDate())
                .isSelfTask(task.isSelfTask())
                .creator(userMapper.toSummaryDto(task.getCreator()))
                .isCompletedAfterDelay(task.isCompletedAfterDelay())
                .isCompletedAfterExtension(task.isCompletedAfterExtension())
                .completionOutcome(task.getCompletionOutcome())
                .createdAt(task.getCreatedAt())
                .build();
    }

    public List<TaskSummaryDto> toSummaryDtoList(Collection<Task> tasks) {
        if (tasks == null) return Collections.emptyList();
        return tasks.stream().map(this::toSummaryDto).collect(Collectors.toList());
    }

    public TaskDto toDto(Task task) {
        if (task == null) return null;

        List<com.app.AreYouReporting.Entities.Department> activeDepts = task.getAssignedDepartments() != null
                ? task.getAssignedDepartments().stream().filter(com.app.AreYouReporting.Entities.Department::isActive).collect(Collectors.toList())
                : Collections.emptyList();

        List<com.app.AreYouReporting.Entities.SubDepartment> activeSubDepts = task.getAssignedSubDepartments() != null
                ? task.getAssignedSubDepartments().stream()
                .filter(sd -> sd.isActive() && sd.getDepartment() != null && sd.getDepartment().isActive())
                .collect(Collectors.toList())
                : Collections.emptyList();

        List<com.app.AreYouReporting.Entities.User> activeAssignees = task.getAssignees() != null
                ? task.getAssignees().stream().filter(com.app.AreYouReporting.Entities.User::isActive).collect(Collectors.toList())
                : Collections.emptyList();

        TaskTemplateDto activeTemplate = (task.getTemplate() != null && task.getTemplate().isActive())
                ? taskTemplateMapper.toDto(task.getTemplate())
                : null;

        return TaskDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .status(task.getStatus())
                .startDate(task.getStartDate())
                .dueDate(task.getDueDate())
                .originalDueDate(task.getOriginalDueDate())
                .isSelfTask(task.isSelfTask())
                .creator(userMapper.toSummaryDto(task.getCreator()))
                .assignedDepartments(departmentMapper.toDtoList(activeDepts))
                .assignedSubDepartments(departmentMapper.toSubDeptDtoList(activeSubDepts))
                .assignees(userMapper.toSummaryDtoList(activeAssignees))
                .startedBy(userMapper.toSummaryDto(task.getStartedBy()))
                .startedAt(task.getStartedAt())
                .closedBy(userMapper.toSummaryDto(task.getClosedBy()))
                .closedAt(task.getClosedAt())
                .extendedBy(userMapper.toSummaryDto(task.getExtendedBy()))
                .extendedAt(task.getExtendedAt())
                .lastExtendedDueDate(task.getLastExtendedDueDate())
                .extensionCount(task.getExtensionCount())
                .isCompletedAfterDelay(task.isCompletedAfterDelay())
                .isCompletedAfterExtension(task.isCompletedAfterExtension())
                .completionOutcome(task.getCompletionOutcome())
                .extensionRejectionCount(task.getExtensionRejectionCount())
                .closureRejectionCount(task.getClosureRejectionCount())
                .totalRejectionCount(task.getTotalRejectionCount())
                .lastRejectionReason(task.getLastRejectionReason())
                .lastRejectedAt(task.getLastRejectedAt())
                .lastRejectedBy(userMapper.toSummaryDto(task.getLastRejectedBy()))
                .template(activeTemplate)
                .targetCount(task.getTargetCount())
                .currentCount(task.getCurrentCount())
                .targetPercentage(task.getTargetPercentage())
                .currentPercentage(task.getCurrentPercentage())
                .proofs(task.getProofs() != null ? taskProofMapper.toDtoList(task.getProofs()) : Collections.emptyList())
                .requests(task.getRequests() != null ? taskRequestMapper.toDtoList(task.getRequests()) : Collections.emptyList())
                .createdAt(task.getCreatedAt())
                .createdBy(task.getCreatedBy())
                .updatedAt(task.getUpdatedAt())
                .updatedBy(task.getUpdatedBy())
                .build();
    }

    public List<TaskDto> toDtoList(Collection<Task> tasks) {
        if (tasks == null) return Collections.emptyList();
        return tasks.stream().map(this::toDto).collect(Collectors.toList());
    }
}
