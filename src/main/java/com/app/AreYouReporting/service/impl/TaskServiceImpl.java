package com.app.AreYouReporting.service.impl;

import com.app.AreYouReporting.Entities.*;
import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ProofValidationException;
import com.app.AreYouReporting.exceptions.ResourceNotFoundException;
import com.app.AreYouReporting.exceptions.ScopeViolationException;
import com.app.AreYouReporting.kafka.DomainEventPublisher;
import com.app.AreYouReporting.mapper.TaskMapper;
import com.app.AreYouReporting.payload.request.SelfTaskCreateRequest;
import com.app.AreYouReporting.payload.request.TaskCreateRequest;
import com.app.AreYouReporting.payload.request.TaskStatusUpdateRequest;
import com.app.AreYouReporting.payload.request.TaskUpdateRequest;
import com.app.AreYouReporting.payload.response.PageResponse;
import com.app.AreYouReporting.payload.response.TaskDto;
import com.app.AreYouReporting.payload.response.TaskSummaryDto;
import com.app.AreYouReporting.repository.*;
import com.app.AreYouReporting.security.ActiveUserContext;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.security.UserPrincipal;
import com.app.AreYouReporting.service.interfaces.AuditService;
import com.app.AreYouReporting.service.interfaces.TaskService;
import com.app.AreYouReporting.specification.TaskSpecification;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final TaskTemplateRepository templateRepository;
    private final TaskProofRepository proofRepository;
    private final TaskMapper taskMapper;
    private final DomainEventPublisher eventPublisher;
    private final ScopeAuthorizationService scopeSecurity;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskSummaryDto> getTasks(
            String search,
            List<TaskStatus> statuses,
            TaskPriority priority,
            Boolean isSelfTask,
            TaskCompletionOutcome completionOutcome,
            Boolean isCompletedAfterDelay,
            Boolean isCompletedAfterExtension,
            UUID departmentId,
            UUID subDepartmentId,
            Instant fromDate,
            Instant toDate,
            Pageable pageable
    ) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();
        boolean isSuperAdmin = scopeSecurity.isSuperAdmin(principal);

        Specification<Task> spec = TaskSpecification.filter(
                search, statuses, priority, isSelfTask, completionOutcome,
                isCompletedAfterDelay, isCompletedAfterExtension,
                departmentId, subDepartmentId, fromDate, toDate,
                context, principal, isSuperAdmin
        );

        Page<Task> page = taskRepository.findAll(spec, pageable);
        return PageResponse.of(page, taskMapper.toSummaryDtoList(page.getContent()));
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTaskById(UUID id) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        Task task = taskRepository.findActiveByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        if (!scopeSecurity.canViewTask(task, context, principal)) {
            throw new ScopeViolationException("You do not have permission to view this task");
        }

        return taskMapper.toDto(task);
    }

    @Override
    @Transactional
    public TaskDto createTask(TaskCreateRequest request) {
        scopeSecurity.validatePermission("tasks.create");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        Set<Department> depts = new HashSet<>();
        if (request.getAssignedDepartmentIds() != null && !request.getAssignedDepartmentIds().isEmpty()) {
            depts.addAll(departmentRepository.findAllById(request.getAssignedDepartmentIds()));
            for (Department dept : depts) {
                if (!scopeSecurity.canAccessDepartment(dept.getId(), context, principal)) {
                    throw new ScopeViolationException("You cannot assign tasks to department: " + dept.getName());
                }
            }
        }

        Set<SubDepartment> subDepts = new HashSet<>();
        if (request.getAssignedSubDepartmentIds() != null && !request.getAssignedSubDepartmentIds().isEmpty()) {
            subDepts.addAll(subDepartmentRepository.findAllById(request.getAssignedSubDepartmentIds()));
            for (SubDepartment subDept : subDepts) {
                if (!scopeSecurity.canAccessSubDepartment(subDept.getId(), subDept.getDepartment() != null ? subDept.getDepartment().getId() : null, context, principal)) {
                    throw new ScopeViolationException("You cannot assign tasks to sub-department: " + subDept.getName());
                }
            }
        }

        Set<User> assignees = new HashSet<>();
        if (request.getAssigneeIds() != null && !request.getAssigneeIds().isEmpty()) {
            assignees.addAll(userRepository.findAllById(request.getAssigneeIds()));
        }

        TaskTemplate template = null;
        if (request.getTemplateId() != null) {
            template = templateRepository.findByIdWithDetails(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("TaskTemplate", "id", request.getTemplateId()));
        }

        Instant startDate = request.getStartDate() != null ? request.getStartDate() : Instant.now();
        Instant dueDate = request.getDueDate();
        if (dueDate.isBefore(startDate)) {
            throw new InvalidOperationException("Due date must be after start date");
        }

        Task task = Task.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(TaskStatus.PENDING)
                .startDate(startDate)
                .dueDate(dueDate)
                .originalDueDate(dueDate)
                .isSelfTask(false)
                .creator(creator)
                .assignedDepartments(depts)
                .assignedSubDepartments(subDepts)
                .assignees(assignees)
                .template(template)
                .targetCount(request.getTargetCount() != null ? request.getTargetCount() : (template != null ? template.getDefaultTargetCount() : null))
                .targetPercentage(request.getTargetPercentage() != null ? request.getTargetPercentage() : (template != null ? template.getDefaultTargetPercentage() : null))
                .currentCount(0.0)
                .currentPercentage(0.0)
                .build();

        Task saved = taskRepository.save(task);

        eventPublisher.publishTaskEvent("TASK_CREATED", saved.getId().toString(), principal.getUsername(), context != null ? context.getRoleName() : "USER",
                Map.of("title", saved.getTitle(), "priority", saved.getPriority().name(), "dueDate", saved.getDueDate().toString()));

        auditService.log(principal.getUsername(), context != null ? context.getRoleName() : "USER", "CREATE_TASK", "TASK", saved.getId().toString(), null, null, saved.getTitle(), AuditStatus.SUCCESS, null);
        return taskMapper.toDto(saved);
    }

    @Override
    @Transactional
    public TaskDto createSelfTask(SelfTaskCreateRequest request) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        Instant startDate = request.getStartDate() != null ? request.getStartDate() : Instant.now();
        Instant dueDate = request.getDueDate();
        if (dueDate.isBefore(startDate)) {
            throw new InvalidOperationException("Due date must be after start date");
        }

        Set<User> assignees = new HashSet<>();
        assignees.add(creator);

        Task task = Task.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(TaskStatus.PENDING)
                .startDate(startDate)
                .dueDate(dueDate)
                .originalDueDate(dueDate)
                .isSelfTask(true)
                .creator(creator)
                .assignees(assignees)
                .build();

        Task saved = taskRepository.save(task);

        auditService.log(principal.getUsername(), context != null ? context.getRoleName() : "USER", "CREATE_SELF_TASK", "TASK", saved.getId().toString(), null, null, saved.getTitle(), AuditStatus.SUCCESS, null);
        return taskMapper.toDto(saved);
    }

    @Override
    @Transactional
    public TaskDto updateTask(UUID id, TaskUpdateRequest request) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        if (!scopeSecurity.canManageTask(task, context, principal)) {
            throw new ScopeViolationException("You do not have permission to update this task");
        }

        if (task.getStatus() == TaskStatus.CLOSED || task.getStatus() == TaskStatus.DELETED) {
            throw new InvalidOperationException("Cannot modify a closed or deleted task");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) task.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getStartDate() != null) task.setStartDate(request.getStartDate());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());

        if (request.getAssignedDepartmentIds() != null) {
            task.setAssignedDepartments(new HashSet<>(departmentRepository.findAllById(request.getAssignedDepartmentIds())));
        }
        if (request.getAssignedSubDepartmentIds() != null) {
            task.setAssignedSubDepartments(new HashSet<>(subDepartmentRepository.findAllById(request.getAssignedSubDepartmentIds())));
        }
        if (request.getAssigneeIds() != null) {
            task.setAssignees(new HashSet<>(userRepository.findAllById(request.getAssigneeIds())));
        }

        if (request.getTargetCount() != null) task.setTargetCount(request.getTargetCount());
        if (request.getCurrentCount() != null) task.setCurrentCount(request.getCurrentCount());
        if (request.getTargetPercentage() != null) task.setTargetPercentage(request.getTargetPercentage());
        if (request.getCurrentPercentage() != null) task.setCurrentPercentage(request.getCurrentPercentage());

        Task updated = taskRepository.save(task);
        auditService.log(principal.getUsername(), context != null ? context.getRoleName() : "USER", "UPDATE_TASK", "TASK", updated.getId().toString(), null, null, updated.getTitle(), AuditStatus.SUCCESS, null);
        return taskMapper.toDto(updated);
    }

    @Override
    @Transactional
    public TaskDto updateTaskStatus(UUID id, TaskStatusUpdateRequest request) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        if (!scopeSecurity.canManageTask(task, context, principal)) {
            throw new ScopeViolationException("You do not have permission to change status of this task");
        }

        TaskStatus oldStatus = task.getStatus();
        TaskStatus newStatus = request.getStatus();

        if (newStatus == TaskStatus.CLOSED) {
            return closeTask(id);
        }

        if (newStatus == TaskStatus.IN_PROGRESS && task.getStartedAt() == null) {
            task.setStartedBy(userRepository.findById(principal.getId()).orElse(null));
            task.setStartedAt(Instant.now());
        }

        task.setStatus(newStatus);
        Task updated = taskRepository.save(task);

        eventPublisher.publishTaskEvent("TASK_STATUS_CHANGED", updated.getId().toString(), principal.getUsername(), context != null ? context.getRoleName() : "USER",
                Map.of("oldStatus", oldStatus.name(), "newStatus", newStatus.name()));

        auditService.log(principal.getUsername(), context != null ? context.getRoleName() : "USER", "CHANGE_TASK_STATUS", "TASK", updated.getId().toString(), null, oldStatus.name(), newStatus.name(), AuditStatus.SUCCESS, null);
        return taskMapper.toDto(updated);
    }

    @Override
    @Transactional
    public TaskDto startTask(UUID id) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        if (!scopeSecurity.canViewTask(task, context, principal)) {
            throw new ScopeViolationException("You do not have access to start this task");
        }

        if (task.getStatus() == TaskStatus.CLOSED || task.getStatus() == TaskStatus.DELETED) {
            throw new InvalidOperationException("Cannot start a closed or deleted task");
        }

        task.setStartedBy(userRepository.findById(principal.getId()).orElse(null));
        task.setStartedAt(Instant.now());
        task.setStatus(TaskStatus.IN_PROGRESS);

        Task saved = taskRepository.save(task);
        eventPublisher.publishTaskEvent("TASK_STARTED", saved.getId().toString(), principal.getUsername(), context != null ? context.getRoleName() : "USER", Map.of("taskId", id.toString()));
        auditService.log(principal.getUsername(), context != null ? context.getRoleName() : "USER", "START_TASK", "TASK", saved.getId().toString(), null, null, saved.getTitle(), AuditStatus.SUCCESS, null);
        return taskMapper.toDto(saved);
    }

    @Override
    @Transactional
    public TaskDto closeTask(UUID id) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        if (!scopeSecurity.canManageTask(task, context, principal)) {
            throw new ScopeViolationException("You do not have permission to close this task");
        }

        // Validate proof requirements if template exists
        if (task.getTemplate() != null && task.getTemplate().getProofRequirements() != null) {
            List<String> missing = new ArrayList<>();
            List<TaskProof> uploadedProofs = proofRepository.findByTaskId(task.getId());

            for (TaskProofRequirement req : task.getTemplate().getProofRequirements()) {
                if (req.isRequired()) {
                    long count = uploadedProofs.stream()
                            .filter(p -> p.getProofRequirement() != null && p.getProofRequirement().getId().equals(req.getId()))
                            .count();
                    if (count < req.getMinCount()) {
                        missing.add(String.format("Proof Requirement '%s' requires at least %d proof(s), found %d",
                                req.getName(), req.getMinCount(), count));
                    }
                }
            }

            if (!missing.isEmpty()) {
                throw new ProofValidationException("Cannot close task: required proofs are missing", missing);
            }
        }

        Instant now = Instant.now();
        task.setClosedBy(userRepository.findById(principal.getId()).orElse(null));
        task.setClosedAt(now);

        boolean wasDelayed = task.getStatus() == TaskStatus.DELAYED || now.isAfter(task.getDueDate());
        boolean wasExtended = task.getExtensionCount() > 0;

        task.setCompletedAfterDelay(wasDelayed);
        task.setCompletedAfterExtension(wasExtended);

        if (wasDelayed && wasExtended) {
            task.setCompletionOutcome(TaskCompletionOutcome.AFTER_DELAY_AND_EXTENSION);
        } else if (wasDelayed) {
            task.setCompletionOutcome(TaskCompletionOutcome.AFTER_DELAY);
        } else if (wasExtended) {
            task.setCompletionOutcome(TaskCompletionOutcome.AFTER_EXTENSION);
        } else {
            task.setCompletionOutcome(TaskCompletionOutcome.ON_TIME);
        }

        task.setStatus(TaskStatus.CLOSED);
        Task closed = taskRepository.save(task);

        eventPublisher.publishTaskEvent("TASK_CLOSED", closed.getId().toString(), principal.getUsername(), context != null ? context.getRoleName() : "USER",
                Map.of("outcome", closed.getCompletionOutcome() != null ? closed.getCompletionOutcome().name() : "CLOSED"));

        auditService.log(principal.getUsername(), context != null ? context.getRoleName() : "USER", "CLOSE_TASK", "TASK", closed.getId().toString(), null, null, closed.getTitle(), AuditStatus.SUCCESS, null);
        return taskMapper.toDto(closed);
    }

    @Override
    @Transactional
    public void deleteTask(UUID id) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        if (!scopeSecurity.canManageTask(task, context, principal)) {
            throw new ScopeViolationException("You do not have permission to delete this task");
        }

        task.setStatus(TaskStatus.DELETED);
        taskRepository.save(task);

        eventPublisher.publishTaskEvent("TASK_DELETED", id.toString(), principal.getUsername(), context != null ? context.getRoleName() : "USER", Map.of("taskId", id.toString()));
        auditService.log(principal.getUsername(), context != null ? context.getRoleName() : "USER", "DELETE_TASK", "TASK", id.toString(), null, task.getTitle(), null, AuditStatus.SUCCESS, null);
    }

    private ActiveUserContext getActiveContext() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            return (ActiveUserContext) req.getAttribute(ScopeAuthorizationService.CONTEXT_ATTRIBUTE);
        }
        return null;
    }
}
