package com.app.AreYouReporting.service.impl;

import com.app.AreYouReporting.Entities.Permission;
import com.app.AreYouReporting.Entities.Role;
import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ResourceNotFoundException;
import com.app.AreYouReporting.mapper.PermissionMapper;
import com.app.AreYouReporting.payload.response.PermissionDto;
import com.app.AreYouReporting.payload.response.ResourceGrantsDto;
import com.app.AreYouReporting.repository.PermissionRepository;
import com.app.AreYouReporting.repository.RoleRepository;
import com.app.AreYouReporting.service.interfaces.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> getAllPermissions() {
        return permissionMapper.toDtoList(permissionRepository.findAllByOrderByResourceAscActionAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceGrantsDto> getAllPermissionsGrouped() {
        List<Permission> all = permissionRepository.findAllByOrderByResourceAscActionAsc();
        return permissionMapper.toResourceGrants(all, Collections.emptySet());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceGrantsDto> getPermissionsByRoleId(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        List<Permission> allPermissions = permissionRepository.findAllByOrderByResourceAscActionAsc();
        return permissionMapper.toResourceGrants(allPermissions, role.getPermissions());
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionDto getPermissionById(UUID id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));
        return permissionMapper.toDto(permission);
    }

    @Override
    @Transactional
    public PermissionDto createPermission(String resource, String action, String description) {
        String authority = (resource + "." + action).toLowerCase().trim();
        if (permissionRepository.existsByAuthority(authority)) {
            throw new InvalidOperationException("Permission with authority '" + authority + "' already exists");
        }

        Permission permission = Permission.builder()
                .resource(resource.toLowerCase().trim())
                .action(action.toLowerCase().trim())
                .authority(authority)
                .description(description)
                .isSystemPermission(false)
                .build();

        Permission saved = permissionRepository.save(permission);
        return permissionMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void seedBaselinePermissions() {
        Map<String, List<String>> moduleActions = new LinkedHashMap<>();
        moduleActions.put("tasks", List.of("read", "create", "update", "delete", "approve", "close", "extend", "status-update", "export"));
        moduleActions.put("departments", List.of("read", "create", "update", "delete"));
        moduleActions.put("sub-departments", List.of("read", "create", "update", "delete"));
        moduleActions.put("users", List.of("read", "create", "update", "delete", "assign-role", "manage-scope"));
        moduleActions.put("roles", List.of("read", "create", "update", "delete", "assign-permissions"));
        moduleActions.put("templates", List.of("read", "create", "update", "delete"));
        moduleActions.put("requests", List.of("read", "create", "approve", "reject", "cancel"));
        moduleActions.put("proofs", List.of("read", "create", "delete", "download"));
        moduleActions.put("audits", List.of("read", "export"));
        moduleActions.put("dashboard", List.of("read"));
        moduleActions.put("settings", List.of("read", "update"));

        for (Map.Entry<String, List<String>> entry : moduleActions.entrySet()) {
            String resource = entry.getKey();
            for (String action : entry.getValue()) {
                String authority = resource + "." + action;
                if (!permissionRepository.existsByAuthority(authority)) {
                    Permission p = Permission.builder()
                            .resource(resource)
                            .action(action)
                            .authority(authority)
                            .description("Permission to " + action + " in " + resource)
                            .isSystemPermission(true)
                            .build();
                    permissionRepository.save(p);
                }
            }
        }
        log.info("Baseline permissions seeded successfully");
    }
}
