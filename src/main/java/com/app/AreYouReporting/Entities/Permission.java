package com.app.AreYouReporting.Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "permissions", indexes = {
        @Index(name = "idx_perm_authority", columnList = "authority", unique = true),
        @Index(name = "idx_perm_resource", columnList = "resource")
})
public class Permission extends AuditableEntity {

    @Column(name = "resource", nullable = false, length = 100)
    private String resource;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "authority", nullable = false, unique = true, length = 150)
    private String authority;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_system_permission", nullable = false)
    @Builder.Default
    private boolean isSystemPermission = true;
}
