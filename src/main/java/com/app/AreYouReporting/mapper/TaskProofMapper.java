package com.app.AreYouReporting.mapper;

import com.app.AreYouReporting.Entities.TaskProof;
import com.app.AreYouReporting.payload.response.TaskProofDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskProofMapper {

    private final UserMapper userMapper;

    public TaskProofDto toDto(TaskProof proof) {
        if (proof == null) return null;
        return TaskProofDto.builder()
                .id(proof.getId())
                .taskId(proof.getTask() != null ? proof.getTask().getId() : null)
                .taskRequestId(proof.getTaskRequest() != null ? proof.getTaskRequest().getId() : null)
                .proofRequirementId(proof.getProofRequirement() != null ? proof.getProofRequirement().getId() : null)
                .proofRequirementName(proof.getProofRequirement() != null ? proof.getProofRequirement().getName() : null)
                .proofType(proof.getProofType())
                .fileUrl(proof.getFileUrl())
                .fileName(proof.getFileName())
                .fileSize(proof.getFileSize())
                .mimeType(proof.getMimeType())
                .description(proof.getDescription())
                .uploadedBy(userMapper.toSummaryDto(proof.getUploadedBy()))
                .uploadedAt(proof.getUploadedAt())
                .build();
    }

    public List<TaskProofDto> toDtoList(Collection<TaskProof> proofs) {
        if (proofs == null) return Collections.emptyList();
        return proofs.stream().map(this::toDto).collect(Collectors.toList());
    }
}
