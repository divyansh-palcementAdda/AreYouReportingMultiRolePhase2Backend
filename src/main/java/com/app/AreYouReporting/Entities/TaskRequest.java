package com.app.AreYouReporting.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "task_requests", indexes = {
        @Index(name = "idx_tr_task", columnList = "task_id"),
        @Index(name = "idx_tr_type", columnList = "request_type"),
        @Index(name = "idx_tr_status", columnList = "status"),
        @Index(name = "idx_tr_reviewer", columnList = "reviewer_id")
})
public class TaskRequest extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    private TaskRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TaskRequestStatus status = TaskRequestStatus.PENDING;

    @Column(name = "requested_due_date")
    private Instant requestedDueDate;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_remarks", length = 1000)
    private String reviewRemarks;

    @OneToMany(mappedBy = "taskRequest", cascade = CascadeType.ALL)
    @JsonIgnore
    @Builder.Default
    private List<TaskProof> proofs = new ArrayList<>();
}
