package com.app.AreYouReporting.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_role_assignments", indexes = {
        @Index(name = "idx_ura_user", columnList = "user_id"),
        @Index(name = "idx_ura_role", columnList = "role_id"),
        @Index(name = "idx_ura_dept", columnList = "department_id"),
        @Index(name = "idx_ura_subdept", columnList = "sub_department_id")
})
public class UserRoleAssignment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_department_id")
    private SubDepartment subDepartment;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_scope_type", length = 30)
    private DataScopeType dataScopeType;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_assignment_custom_departments",
            joinColumns = @JoinColumn(name = "assignment_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    @Builder.Default
    private Set<Department> customDepartments = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_assignment_custom_sub_departments",
            joinColumns = @JoinColumn(name = "assignment_id"),
            inverseJoinColumns = @JoinColumn(name = "sub_department_id")
    )
    @Builder.Default
    private Set<SubDepartment> customSubDepartments = new HashSet<>();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
