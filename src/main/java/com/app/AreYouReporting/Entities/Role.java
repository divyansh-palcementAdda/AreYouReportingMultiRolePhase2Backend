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
@Table(name = "roles", indexes = {
        @Index(name = "idx_role_name", columnList = "name", unique = true)
})
public class Role extends AuditableEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_system_role", nullable = false)
    @Builder.Default
    private boolean isSystemRole = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_data_scope", nullable = false, length = 30)
    @Builder.Default
    private DataScopeType defaultDataScope = DataScopeType.SELF;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id", referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_role_permission", columnNames = {"role_id", "permission_id"})
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
