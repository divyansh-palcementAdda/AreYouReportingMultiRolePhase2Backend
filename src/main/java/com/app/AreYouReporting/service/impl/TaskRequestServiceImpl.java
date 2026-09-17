package com.app.AreYouReporting.service.impl;

import com.app.AreYouReporting.Entities.*;
import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ProofValidationException;
import com.app.AreYouReporting.exceptions.ResourceNotFoundException;
import com.app.AreYouReporting.exceptions.ScopeViolationException;
import com.app.AreYouReporting.kafka.DomainEventPublisher;
import com.app.AreYouReporting.mapper.TaskRequestMapper;
import com.app.AreYouReporting.payload.request.TaskRequestReviewRequest;
import com.app.AreYouReporting.payload.request.TaskRequestSubmitRequest;
import com.app.AreYouReporting.payload.response.TaskRequestDto;
import com.app.AreYouReporting.repository.TaskProofRepository;
import com.app.AreYouReporting.repository.TaskRepository;
import com.app.AreYouReporting.repository.TaskRequestRepository;
import com.app.AreYouReporting.repository.UserRepository;
import com.app.AreYouReporting.security.ActiveUserContext;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.security.UserPrincipal;
import com.app.AreYouReporting.service.interfaces.AuditService;
import com.app.AreYouReporting.service.interfaces.TaskRequestService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskRequestServiceImpl implements TaskRequestService {

    private final TaskRequestRepository requestRepository;
    private final TaskRepository taskRepository;
    private final TaskProofRepository proofRepository;
    private final UserRepository userRepository;
    private final TaskRequestMapper requestMapper;
    private final DomainEventPublisher eventPublisher;
    private final ScopeAuthorizationService scopeSecurity;
    private final AuditService auditService;

    @Override
    @Transactional
    public TaskRequestDto submitRequest(UUID taskId, TaskRequestSubmitRequest request) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        Task task = taskRepository.findByIdWithDetails(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (!scopeSecurity.canViewTask(task, context, principal)) {
            throw new ScopeViolationException("You do not have permission to submit requests for this task");
        }

        if (task.getStatus() == TaskStatus.CLOSED || task.getStatus() == TaskStatus.DELETED) {
            throw new InvalidOperationException("Cannot submit requests for a closed or deleted task");
        }

        // Check if pending request of same type already exists
        if (requestRepository.existsByTaskIdAndRequestTypeAndStatus(taskId, request.getRequestType(), TaskRequestStatus.PENDING)) {
            throw new InvalidOperationException("A pending request of type " + request.getRequestType() + " already exists for this task");
        }

        if (request.getRequestType() == TaskRequestType.DUE_DATE_EXTENSION) {
            if (request.getRequestedDueDate() == null) {
                throw new InvalidOperationException("Requested due date is required for extension requests");
            }
            if (!request.getRequestedDueDate().isAfter(task.getDueDate())) {
                throw new InvalidOperationException("Requested due date must be after current task due date");
            }
        }

        TaskRequest taskRequest = TaskRequest.builder()
                .task(task)
                .requestType(request.getRequestType())
                .status(TaskRequestStatus.PENDING)
                .requestedDueDate(request.getRequestedDueDate())
                .reason(request.getReason())
                .build();

        TaskRequest savedRequest = requestRepository.save(taskRequest);

        // Associate proofs if provided
        if (request.getProofIds() != null && !request.getProofIds().isEmpty()) {
            List<TaskProof> proofs = proofRepository.findAllById(request.getProofIds());
            for (TaskProof proof : proofs) {
                proof.setTaskRequest(savedRequest);
                proofRepository.save(proof);
                savedRequest.getProofs().add(proof);
            }
        }

        // Update task status according to request type
        if (request.getRequestType() == TaskRequestType.DUE_DATE_EXTENSION) {
            task.setStatus(TaskStatus.REQUEST_FOR_EXTENSION);
        } else if (request.getRequestType() == TaskRequestType.TASK_CLOSURE) {
            task.setStatus(TaskStatus.REQUEST_FOR_CLOSURE);
        }
        taskRepository.save(task);

        eventPublisher.publishRequestEvent("REQUEST_SUBMITTED", savedRequest.getId().toString(), principal.getUsername(), context != null ? context.getRoleName() : "USER",
                Map.of("taskId", taskId.toString(), "requestType", request.getRequestType().name(), "reason", request.getReason()));

        auditService.log(principal.getUsername(), context != null ? context.getRoleName() : "USER", "SUBMIT_TASK_REQUEST", "TASK_REQUEST", savedRequest.getId().toString(), null, null, request.getRequestType().name() + " for task: " + task.getTitle(), AuditStatus.SUCCESS, null);

        return requestMapper.toDto(savedRequest);
    }

    @Override
    @Transactional
    public TaskRequestDto reviewRequest(UUID requestId, TaskRequestReviewRequest reviewRequest) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        TaskRequest request = requestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("TaskRequest", "id", requestId));

        if (request.getStatus() != TaskRequestStatus.PENDING) {
            throw new InvalidOperationException("Request has already been reviewed");
        }

        if (!scopeSecurity.canApproveRequest(request, context, principal)) {
            throw new ScopeViolationException("You do not have permission to approve/reject requests for this task");
        }

        User reviewer = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        Task task = request.getTask();
        Instant now = Instant.now();

        if (Boolean.TRUE.equals(reviewRequest.getApprove())) {
            request.setStatus(TaskRequestStatus.APPROVED);
            request.setReviewer(reviewer);
            request.setReviewedAt(now);
            request.setReviewRemarks(reviewRequest.getRemarks());

            if (request.getRequestType() == TaskRequestType.DUE_DATE_EXTENSION) {
                task.setLastExtendedDueDate(request.getRequestedDueDate());
                task.setDueDate(request.getRequestedDueDate());
                task.setExtendedBy(reviewer);
                task.setExtendedAt(now);
                task.setExtensionCount(task.getExtensionCount() + 1);
                task.setStatus(TaskStatus.EXTENDED);
            } else if (request.getRequestType() == TaskRequestType.TASK_CLOSURE) {
                // Validate proof requirements if template is present
                validateClosureProofs(task);

                task.setClosedBy(reviewer);
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
            }
        } else {
            request.setStatus(TaskRequestStatus.REJECTED);
            request.setReviewer(reviewer);
            request.setReviewedAt(now);
            request.setReviewRemarks(reviewRequest.getRemarks());

            // Track rejection metrics
            task.setTotalRejectionCount(task.getTotalRejectionCount() + 1);
            task.setLastRejectionReason(reviewRequest.getRemarks());
            task.setLastRejectedAt(now);
            task.setLastRejectedBy(reviewer);

            if (request.getRequestType() == TaskRequestType.DUE_DATE_EXTENSION) {
                task.setExtensionRejectionCount(task.getExtensionRejectionCount() + 1);
            } else if (request.getRequestType() == TaskRequestType.TASK_CLOSURE) {
                task.setClosureRejectionCount(task.getClosureRejectionCount() + 1);
            }

            // Revert task status
            if (now.isAfter(task.getDueDate())) {
                task.setStatus(TaskStatus.DELAYED);
            } else if (task.getExtensionCount() > 0) {
                task.setStatus(TaskStatus.EXTENDED);
            } else if (task.getStartedAt() != null) {
                task.setStatus(TaskStatus.IN_PROGRESS);
            } else {
                task.setStatus(TaskStatus.PENDING);
            }
        }

        taskRepository.save(task);
        TaskRequest saved = requestRepository.save(request);

        eventPublisher.publishRequestEvent(
                Boolean.TRUE.equals(reviewRequest.getApprove()) ? "REQUEST_APPROVED" : "REQUEST_REJECTED",
                saved.getId().toString(), principal.getUsername(), context != null ? context.getRoleName() : "USER",
                Map.of("taskId", task.getId().toString(), "requestType", saved.getRequestType().name(), "status", saved.getStatus().name())
        );

        auditService.log(principal.getUsername(), context != null ? context.getRoleName() : "USER", "REVIEW_TASK_REQUEST", "TASK_REQUEST", saved.getId().toString(), null, null, saved.getStatus().name() + " for task: " + task.getTitle(), AuditStatus.SUCCESS, null);

        return requestMapper.toDto(saved);
    }

    private void validateClosureProofs(Task task) {
        if (task.getTemplate() != null && task.getTemplate().getProofRequirements() != null) {
            List<String> missing = new ArrayList<>();
            List<TaskProof> uploadedProofs = proofRepository.findByTaskId(task.getId());

            for (TaskProofRequirement req : task.getTemplate().getProofRequirements()) {
                if (req.isRequired()) {
                    long count = uploadedProofs.stream()
                            .filter(p -> p.getProofRequirement() != null && p.getProofRequirement().getId().equals(req.getId()))
                            .count();
                    if (count < req.getMinCount()) {
                        missing.add(String.format("Proof Requirement '%s' requires at least %d proof(s), but found %d",
                                req.getName(), req.getMinCount(), count));
                    }
                }
            }

            if (!missing.isEmpty()) {
                throw new ProofValidationException("Cannot close task: required proofs are missing", missing);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRequestDto> getRequestsByTaskId(UUID taskId) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (task.getStatus() == TaskStatus.DELETED) {
            throw new ResourceNotFoundException("Task", "id", taskId);
        }

        if (!scopeSecurity.canViewTask(task, context, principal)) {
            throw new ScopeViolationException("You do not have permission to view requests for this task");
        }

        return requestMapper.toDtoList(requestRepository.findByTaskId(taskId));
    }

    @Override
    @Transactional(readOnly = true)
    public TaskRequestDto getRequestById(UUID requestId) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        TaskRequest request = requestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("TaskRequest", "id", requestId));

        if (request.getTask() == null || request.getTask().getStatus() == TaskStatus.DELETED) {
            throw new ResourceNotFoundException("TaskRequest", "id", requestId);
        }

        if (!scopeSecurity.canViewTask(request.getTask(), context, principal)) {
            throw new ScopeViolationException("You do not have permission to view this request");
        }

        return requestMapper.toDto(request);
    }

    @Override
    @Transactional
    public void cancelRequest(UUID requestId) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        TaskRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("TaskRequest", "id", requestId));

        if (!scopeSecurity.isSuperAdmin(principal) && !request.getCreatedBy().equalsIgnoreCase(principal.getUsername())) {
            throw new ScopeViolationException("You can only cancel your own pending requests");
        }

        if (request.getStatus() != TaskRequestStatus.PENDING) {
            throw new InvalidOperationException("Only pending requests can be cancelled");
        }

        request.setStatus(TaskRequestStatus.CANCELLED);
        requestRepository.save(request);

        Task task = request.getTask();
        if (Instant.now().isAfter(task.getDueDate())) {
            task.setStatus(TaskStatus.DELAYED);
        } else if (task.getExtensionCount() > 0) {
            task.setStatus(TaskStatus.EXTENDED);
        } else if (task.getStartedAt() != null) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        } else {
            task.setStatus(TaskStatus.PENDING);
        }
        taskRepository.save(task);

        auditService.log(principal.getUsername(), context != null ? context.getRoleName() : "USER", "CANCEL_TASK_REQUEST", "TASK_REQUEST", requestId.toString(), null, null, null, AuditStatus.SUCCESS, null);
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
