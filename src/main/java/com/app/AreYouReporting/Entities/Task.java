package com.app.AreYouReporting.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_task_title", columnList = "title"),
        @Index(name = "idx_task_status_due", columnList = "status, due_date"),
        @Index(name = "idx_task_dates", columnList = "start_date, due_date"),
        @Index(name = "idx_task_self_status", columnList = "is_self_task, status"),
        @Index(name = "idx_task_creator_status", columnList = "creator_id, status"),
        @Index(name = "idx_task_priority_status", columnList = "priority, status"),
        @Index(name = "idx_task_completion_outcome", columnList = "completion_outcome"),
        @Index(name = "idx_task_created_at", columnList = "created_at")
})
public class Task extends AuditableEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Lob
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "original_due_date", nullable = false)
    private Instant originalDueDate;

    @Column(name = "is_self_task", nullable = false)
    @Builder.Default
    private boolean isSelfTask = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_assigned_departments",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    @Builder.Default
    private Set<Department> assignedDepartments = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_assigned_sub_departments",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "sub_department_id")
    )
    @Builder.Default
    private Set<SubDepartment> assignedSubDepartments = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_assignees",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> assignees = new HashSet<>();

    // Lifecycle Auditing & Timestamp Tracking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by_id")
    private User startedBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_id")
    private User closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extended_by_id")
    private User extendedBy;

    @Column(name = "extended_at")
    private Instant extendedAt;

    @Column(name = "last_extended_due_date")
    private Instant lastExtendedDueDate;

    @Column(name = "extension_count", nullable = false)
    @Builder.Default
    private int extensionCount = 0;

    // Delayed & Extension Completion Metrics
    @Column(name = "is_completed_after_delay", nullable = false)
    @Builder.Default
    private boolean isCompletedAfterDelay = false;

    @Column(name = "is_completed_after_extension", nullable = false)
    @Builder.Default
    private boolean isCompletedAfterExtension = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_outcome", length = 30)
    private TaskCompletionOutcome completionOutcome;

    // Request Rejection Metrics
    @Column(name = "extension_rejection_count", nullable = false)
    @Builder.Default
    private int extensionRejectionCount = 0;

    @Column(name = "closure_rejection_count", nullable = false)
    @Builder.Default
    private int closureRejectionCount = 0;

    @Column(name = "total_rejection_count", nullable = false)
    @Builder.Default
    private int totalRejectionCount = 0;

    @Column(name = "last_rejection_reason", length = 1000)
    private String lastRejectionReason;

    @Column(name = "last_rejected_at")
    private Instant lastRejectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_rejected_by_id")
    private User lastRejectedBy;

    // Template and Target Metrics
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private TaskTemplate template;

    @Column(name = "target_count")
    private Double targetCount;

    @Column(name = "current_count")
    private Double currentCount;

    @Column(name = "target_percentage")
    private Double targetPercentage;

    @Column(name = "current_percentage")
    private Double currentPercentage;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<TaskProof> proofs = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<TaskRequest> requests = new ArrayList<>();
}
