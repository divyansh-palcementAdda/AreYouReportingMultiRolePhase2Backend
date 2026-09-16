package com.app.AreYouReporting.Entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sub_departments", indexes = {
        @Index(name = "idx_subdept_dept", columnList = "department_id"),
        @Index(name = "idx_subdept_name", columnList = "name"),
        @Index(name = "idx_subdept_code", columnList = "code")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_dept_subdept_name", columnNames = {"department_id", "name"}),
        @UniqueConstraint(name = "uk_dept_subdept_code", columnNames = {"department_id", "code"})
})
public class SubDepartment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
