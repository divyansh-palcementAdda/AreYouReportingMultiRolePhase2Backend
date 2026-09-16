package com.app.AreYouReporting.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

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
@Table(name = "task_templates", indexes = {
        @Index(name = "idx_template_name", columnList = "name", unique = true)
})
public class TaskTemplate extends AuditableEntity {

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_priority", nullable = false, length = 20)
    @Builder.Default
    private TaskPriority defaultPriority = TaskPriority.MEDIUM;

    @Column(name = "default_duration_days")
    private Integer defaultDurationDays;

    @Column(name = "default_target_count")
    private Double defaultTargetCount;

    @Column(name = "default_target_percentage")
    private Double defaultTargetPercentage;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "template_applicable_departments",
            joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    @Builder.Default
    private Set<Department> applicableDepartments = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "template_applicable_sub_departments",
            joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "sub_department_id")
    )
    @Builder.Default
    private Set<SubDepartment> applicableSubDepartments = new HashSet<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<TaskProofRequirement> proofRequirements = new ArrayList<>();
}
