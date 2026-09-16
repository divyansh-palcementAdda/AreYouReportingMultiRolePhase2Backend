package com.app.AreYouReporting.Entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "task_proof_requirements", indexes = {
        @Index(name = "idx_tpr_template", columnList = "template_id")
})
public class TaskProofRequirement extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private TaskTemplate template;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private boolean isRequired = true;

    @Column(name = "accepted_proof_types", nullable = false, length = 255)
    @Builder.Default
    private String acceptedProofTypes = "DOCUMENT,IMAGE,LINK,SPREADSHEET,OTHER";

    @Column(name = "min_count", nullable = false)
    @Builder.Default
    private int minCount = 1;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;
}
