package com.app.AreYouReporting.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "task_proofs", indexes = {
        @Index(name = "idx_tp_task", columnList = "task_id"),
        @Index(name = "idx_tp_request", columnList = "task_request_id"),
        @Index(name = "idx_tp_uploader", columnList = "uploaded_by_id")
})
public class TaskProof extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_request_id")
    private TaskRequest taskRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proof_requirement_id")
    private TaskProofRequirement proofRequirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "proof_type", nullable = false, length = 30)
    private ProofType proofType;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    @Builder.Default
    private Instant uploadedAt = Instant.now();
}
