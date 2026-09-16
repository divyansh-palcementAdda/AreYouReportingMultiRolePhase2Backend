package com.app.AreYouReporting.service.impl;

import com.app.AreYouReporting.Entities.*;
import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ResourceNotFoundException;
import com.app.AreYouReporting.exceptions.ScopeViolationException;
import com.app.AreYouReporting.mapper.TaskProofMapper;
import com.app.AreYouReporting.payload.request.TaskProofUploadRequest;
import com.app.AreYouReporting.payload.response.TaskProofDto;
import com.app.AreYouReporting.repository.*;
import com.app.AreYouReporting.security.ActiveUserContext;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.security.UserPrincipal;
import com.app.AreYouReporting.service.interfaces.AuditService;
import com.app.AreYouReporting.service.interfaces.TaskProofService;
import com.app.AreYouReporting.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskProofServiceImpl implements TaskProofService {

    private final TaskProofRepository proofRepository;
    private final TaskRepository taskRepository;
    private final TaskRequestRepository requestRepository;
    private final TaskProofRequirementRepository proofRequirementRepository;
    private final UserRepository userRepository;
    private final TaskProofMapper proofMapper;
    private final StorageService storageService;
    private final ScopeAuthorizationService scopeSecurity;
    private final AuditService auditService;

    @Override
    @Transactional
    public TaskProofDto uploadProof(UUID taskId, MultipartFile file, TaskProofUploadRequest request) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        Task task = taskRepository.findByIdWithDetails(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (!scopeSecurity.canViewTask(task, context, principal)) {
            throw new ScopeViolationException("You do not have permission to upload proofs for this task");
        }

        User uploader = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        TaskRequest taskReq = null;
        if (request.getTaskRequestId() != null) {
            taskReq = requestRepository.findById(request.getTaskRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("TaskRequest", "id", request.getTaskRequestId()));
        }

        TaskProofRequirement requirement = null;
        if (request.getProofRequirementId() != null) {
            requirement = proofRequirementRepository.findById(request.getProofRequirementId())
                    .orElseThrow(() -> new ResourceNotFoundException("TaskProofRequirement", "id", request.getProofRequirementId()));
        }

        String fileUrl;
        String fileName;
        Long fileSize = null;
        String mimeType = null;

        if (request.getProofType() == ProofType.LINK) {
            if (request.getExternalLinkUrl() == null || request.getExternalLinkUrl().isBlank()) {
                throw new InvalidOperationException("External link URL is required for LINK proof type");
            }
            fileUrl = request.getExternalLinkUrl().trim();
            fileName = "External Link";
            mimeType = "text/uri-list";
        } else {
            if (file == null || file.isEmpty()) {
                throw new InvalidOperationException("File is required for upload");
            }
            fileUrl = storageService.storeFile(file, "task-proofs/" + taskId);
            fileName = file.getOriginalFilename();
            fileSize = file.getSize();
            mimeType = file.getContentType();
        }

        TaskProof proof = TaskProof.builder()
                .task(task)
                .taskRequest(taskReq)
                .proofRequirement(requirement)
                .proofType(request.getProofType())
                .fileUrl(fileUrl)
                .fileName(fileName)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .description(request.getDescription())
                .uploadedBy(uploader)
                .uploadedAt(Instant.now())
                .build();

        TaskProof saved = proofRepository.save(proof);
        auditService.log(principal.getUsername(), context != null ? context.getRoleName() : "USER", "UPLOAD_PROOF", "TASK_PROOF", saved.getId().toString(), null, null, "Uploaded proof for task: " + task.getTitle(), AuditStatus.SUCCESS, null);
        return proofMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadProofFile(UUID proofId) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        TaskProof proof = proofRepository.findById(proofId)
                .orElseThrow(() -> new ResourceNotFoundException("TaskProof", "id", proofId));

        if (!scopeSecurity.canViewTask(proof.getTask(), context, principal)) {
            throw new ScopeViolationException("You do not have permission to download this proof");
        }

        if (proof.getProofType() == ProofType.LINK) {
            throw new InvalidOperationException("Link proofs cannot be downloaded as files");
        }

        return storageService.loadFileAsResource(proof.getFileUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskProofDto> getProofsByTaskId(UUID taskId) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (!scopeSecurity.canViewTask(task, context, principal)) {
            throw new ScopeViolationException("You do not have permission to view proofs for this task");
        }

        return proofMapper.toDtoList(proofRepository.findByTaskId(taskId));
    }

    @Override
    @Transactional
    public void deleteProof(UUID proofId) {
        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        TaskProof proof = proofRepository.findById(proofId)
                .orElseThrow(() -> new ResourceNotFoundException("TaskProof", "id", proofId));

        if (!scopeSecurity.isSuperAdmin(principal) && !proof.getUploadedBy().getId().equals(principal.getId())) {
            throw new ScopeViolationException("You can only delete proofs uploaded by yourself");
        }

        if (proof.getProofType() != ProofType.LINK) {
            storageService.deleteFile(proof.getFileUrl());
        }

        proofRepository.delete(proof);
        auditService.log(principal.getUsername(), context != null ? context.getRoleName() : "USER", "DELETE_PROOF", "TASK_PROOF", proofId.toString(), null, proof.getFileName(), null, AuditStatus.SUCCESS, null);
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
