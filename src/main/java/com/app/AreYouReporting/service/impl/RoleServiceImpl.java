package com.app.AreYouReporting.service.impl;

import com.app.AreYouReporting.Entities.AuditStatus;
import com.app.AreYouReporting.Entities.DataScopeType;
import com.app.AreYouReporting.Entities.Permission;
import com.app.AreYouReporting.Entities.Role;
import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ResourceNotFoundException;
import com.app.AreYouReporting.mapper.RoleMapper;
import com.app.AreYouReporting.payload.request.RoleRequest;
import com.app.AreYouReporting.payload.response.RoleDto;
import com.app.AreYouReporting.repository.PermissionRepository;
import com.app.AreYouReporting.repository.RoleRepository;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.service.interfaces.AuditService;
import com.app.AreYouReporting.service.interfaces.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;
    private final AuditService auditService;
    private final ScopeAuthorizationService scopeSecurity;

    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleMapper.toDtoList(roleRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDto getRoleById(UUID id) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        return roleMapper.toDto(role);
    }

    @Override
    @Transactional
    public RoleDto createRole(RoleRequest request) {
        scopeSecurity.validatePermission("roles.create");

        String roleName = request.getName().trim().toUpperCase();
        if (roleRepository.existsByName(roleName)) {
            throw new InvalidOperationException("Role with name '" + roleName + "' already exists");
        }

        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissions.addAll(permissionRepository.findAllById(request.getPermissionIds()));
        }

        Role role = Role.builder()
                .name(roleName)
                .description(request.getDescription())
                .defaultDataScope(request.getDefaultDataScope() != null ? request.getDefaultDataScope() : DataScopeType.SELF)
                .isSystemRole(false)
                .permissions(permissions)
                .build();

        Role saved = roleRepository.save(role);
        auditService.log(scopeSecurity.getCurrentUsername(), "ROLE", "CREATE_ROLE", "ROLE", saved.getId().toString(), null, null, saved.getName(), AuditStatus.SUCCESS, null);
        return roleMapper.toDto(saved);
    }

    @Override
    @Transactional
    public RoleDto updateRole(UUID id, RoleRequest request) {
        scopeSecurity.validatePermission("roles.update");

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        String oldName = role.getName();
        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim().toUpperCase();
            if (!newName.equalsIgnoreCase(role.getName()) && roleRepository.existsByName(newName)) {
                throw new InvalidOperationException("Role with name '" + newName + "' already exists");
            }
            if (!role.isSystemRole()) {
                role.setName(newName);
            }
        }

        role.setDescription(request.getDescription());
        if (request.getDefaultDataScope() != null) {
            role.setDefaultDataScope(request.getDefaultDataScope());
        }

        if (request.getPermissionIds() != null) {
            Set<Permission> perms = new HashSet<>(permissionRepository.findAllById(request.getPermissionIds()));
            role.setPermissions(perms);
        }

        Role updated = roleRepository.save(role);
        auditService.log(scopeSecurity.getCurrentUsername(), "ROLE", "UPDATE_ROLE", "ROLE", updated.getId().toString(), null, oldName, updated.getName(), AuditStatus.SUCCESS, null);
        return roleMapper.toDto(updated);
    }

    @Override
    @Transactional
    public RoleDto updateRolePermissions(UUID id, Set<UUID> permissionIds) {
        scopeSecurity.validatePermission("roles.assign-permissions");

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        Set<Permission> permissions = new HashSet<>();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            permissions.addAll(permissionRepository.findAllById(permissionIds));
        }
        role.setPermissions(permissions);

        Role saved = roleRepository.save(role);
        auditService.log(scopeSecurity.getCurrentUsername(), "ROLE", "UPDATE_ROLE_PERMISSIONS", "ROLE", saved.getId().toString(), null, null, "Updated permissions count: " + permissions.size(), AuditStatus.SUCCESS, null);
        return roleMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteRole(UUID id) {
        scopeSecurity.validatePermission("roles.delete");

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        if (role.isSystemRole()) {
            throw new InvalidOperationException("System predefined roles cannot be deleted");
        }

        roleRepository.delete(role);
        auditService.log(scopeSecurity.getCurrentUsername(), "ROLE", "DELETE_ROLE", "ROLE", id.toString(), null, role.getName(), null, AuditStatus.SUCCESS, null);
    }

    @Override
    @Transactional
    public void seedBaselineRoles() {
        List<Permission> allPermissions = permissionRepository.findAll();

        Map<String, Set<String>> defaultRoleAuthorities = new HashMap<>();

        // SUPER_ADMIN has ALL permissions
        Set<String> superAdminAuths = allPermissions.stream().map(Permission::getAuthority).collect(Collectors.toSet());
        defaultRoleAuthorities.put("SUPER_ADMIN", superAdminAuths);

        // ADMIN has department-level management permissions
        defaultRoleAuthorities.put("ADMIN", Set.of(
                "tasks.read", "tasks.create", "tasks.update", "tasks.delete", "tasks.approve", "tasks.close", "tasks.extend", "tasks.status-update", "tasks.export",
                "departments.read", "departments.update",
                "sub-departments.read", "sub-departments.create", "sub-departments.update", "sub-departments.delete",
                "users.read", "users.create", "users.update", "users.assign-role",
                "templates.read", "templates.create", "templates.update",
                "requests.read", "requests.create", "requests.approve", "requests.reject",
                "proofs.read", "proofs.create", "proofs.delete", "proofs.download",
                "dashboard.read"
        ));

        // SUB_ADMIN has sub-department / department operations
        defaultRoleAuthorities.put("SUB_ADMIN", Set.of(
                "tasks.read", "tasks.create", "tasks.update", "tasks.approve", "tasks.close", "tasks.extend", "tasks.status-update", "tasks.export",
                "departments.read",
                "sub-departments.read",
                "users.read",
                "templates.read",
                "requests.read", "requests.create", "requests.approve", "requests.reject",
                "proofs.read", "proofs.create", "proofs.download",
                "dashboard.read"
        ));

        // HOD has subdepartment management
        defaultRoleAuthorities.put("HOD", Set.of(
                "tasks.read", "tasks.create", "tasks.update", "tasks.approve", "tasks.close", "tasks.extend", "tasks.status-update",
                "departments.read",
                "sub-departments.read",
                "users.read",
                "templates.read",
                "requests.read", "requests.create", "requests.approve", "requests.reject",
                "proofs.read", "proofs.create", "proofs.download",
                "dashboard.read"
        ));

        // TEACHER has self / assigned tasks permissions
        defaultRoleAuthorities.put("TEACHER", Set.of(
                "tasks.read", "tasks.create", "tasks.update", "tasks.status-update",
                "departments.read",
                "sub-departments.read",
                "templates.read",
                "requests.read", "requests.create", "requests.cancel",
                "proofs.read", "proofs.create", "proofs.download",
                "dashboard.read"
        ));

        Map<String, DataScopeType> scopes = Map.of(
                "SUPER_ADMIN", DataScopeType.GLOBAL,
                "ADMIN", DataScopeType.DEPARTMENT,
                "SUB_ADMIN", DataScopeType.DEPARTMENT,
                "HOD", DataScopeType.SUB_DEPARTMENT,
                "TEACHER", DataScopeType.SELF
        );

        for (Map.Entry<String, Set<String>> entry : defaultRoleAuthorities.entrySet()) {
            String roleName = entry.getKey();
            Role role = roleRepository.findByName(roleName).orElse(null);
            if (role == null) {
                Set<Permission> perms = allPermissions.stream()
                        .filter(p -> entry.getValue().contains(p.getAuthority()))
                        .collect(Collectors.toSet());

                role = Role.builder()
                        .name(roleName)
                        .description("System role for " + roleName)
                        .isSystemRole(true)
                        .defaultDataScope(scopes.getOrDefault(roleName, DataScopeType.SELF))
                        .permissions(perms)
                        .build();

                roleRepository.save(role);
            }
        }
        log.info("Baseline roles seeded successfully");
    }
}
