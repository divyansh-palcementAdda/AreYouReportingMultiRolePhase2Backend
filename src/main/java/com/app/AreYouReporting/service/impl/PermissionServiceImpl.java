package com.app.AreYouReporting.service.impl;

import com.app.AreYouReporting.Entities.Permission;
import com.app.AreYouReporting.Entities.Role;
import com.app.AreYouReporting.Entities.User;
import com.app.AreYouReporting.Entities.UserRoleAssignment;
import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ResourceNotFoundException;
import com.app.AreYouReporting.mapper.PermissionMapper;
import com.app.AreYouReporting.mapper.UserMapper;
import com.app.AreYouReporting.payload.response.PermissionDto;
import com.app.AreYouReporting.payload.response.ResourceGrantsDto;
import com.app.AreYouReporting.payload.response.UserPermissionResponse;
import com.app.AreYouReporting.payload.response.UserRoleAssignmentDto;
import com.app.AreYouReporting.repository.PermissionRepository;
import com.app.AreYouReporting.repository.RoleRepository;
import com.app.AreYouReporting.repository.UserRepository;
import com.app.AreYouReporting.repository.UserRoleAssignmentRepository;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.service.interfaces.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;
    private final ScopeAuthorizationService scopeSecurity;


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
    public UserPermissionResponse getUserPermissions(UUID userId, UUID roleId) {
        final UUID targetUserId = (userId != null) ? userId : scopeSecurity.getCurrentUserId();

        User user = userRepository.findByIdAndIsActiveTrue(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        List<UserRoleAssignment> assignments = assignmentRepository.findAllActiveWithDetailsByUserId(targetUserId);

        Set<Permission> userPermissions = new HashSet<>();
        UUID activeRoleId = null;
        String activeRoleName = null;

        if (roleId != null) {
            UserRoleAssignment matching = assignments.stream()
                    .filter(a -> a.getRole() != null && a.getRole().getId().equals(roleId))
                    .findFirst()
                    .orElse(null);

            if (matching != null && matching.getRole() != null) {
                activeRoleId = matching.getRole().getId();
                activeRoleName = matching.getRole().getName();
                if (matching.getRole().getPermissions() != null) {
                    userPermissions.addAll(matching.getRole().getPermissions());
                }
            }
        } else {
            for (UserRoleAssignment assignment : assignments) {
                if (assignment.getRole() != null && assignment.getRole().getPermissions() != null) {
                    userPermissions.addAll(assignment.getRole().getPermissions());
                }
            }
            if (!assignments.isEmpty() && assignments.get(0).getRole() != null) {
                activeRoleId = assignments.get(0).getRole().getId();
                activeRoleName = assignments.get(0).getRole().getName();
            }
        }

        List<Permission> allPermissions = permissionRepository.findAllByOrderByResourceAscActionAsc();
        List<ResourceGrantsDto> grants = permissionMapper.toResourceGrants(allPermissions, userPermissions);
        List<String> flatPermissions = userPermissions.stream()
                .map(Permission::getAuthority)
                .sorted()
                .collect(Collectors.toList());

        List<UserRoleAssignmentDto> assignmentDtos = userMapper.toAssignmentDtoList(assignments);

        return UserPermissionResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .activeRoleId(activeRoleId)
                .activeRoleName(activeRoleName)
                .assignments(assignmentDtos)
                .grants(grants)
                .permissions(flatPermissions)
                .build();
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
        moduleActions.put("dropdowns", List.of("departments", "sub-departments", "users", "tasks", "templates", "roles", "permissions", "options"));

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

        // Seed DROPDOWN_VIEW_* named permissions
        Map<String, String> dropdownAliases = new LinkedHashMap<>();
        dropdownAliases.put("DROPDOWN_VIEW_DEPARTMENTS", "Permission to view selectable departments in dropdowns");
        dropdownAliases.put("DROPDOWN_VIEW_SUB_DEPARTMENTS", "Permission to view selectable sub-departments in dropdowns");
        dropdownAliases.put("DROPDOWN_VIEW_USERS", "Permission to view selectable users in dropdowns");
        dropdownAliases.put("DROPDOWN_VIEW_TASKS", "Permission to view selectable tasks in dropdowns");
        dropdownAliases.put("DROPDOWN_VIEW_TASK_TEMPLATES", "Permission to view selectable task templates in dropdowns");
        dropdownAliases.put("DROPDOWN_VIEW_ROLES", "Permission to view selectable roles in dropdowns");
        dropdownAliases.put("DROPDOWN_VIEW_PERMISSION_OPTIONS", "Permission to view selectable permissions in dropdowns");
        dropdownAliases.put("DROPDOWN_VIEW_PROOF_REQUIREMENTS", "Permission to view proof requirement options in dropdowns");
        dropdownAliases.put("DROPDOWN_VIEW_TASK_STATUS_OPTIONS", "Permission to view task status options in dropdowns");
        dropdownAliases.put("DROPDOWN_VIEW_REQUEST_STATUS_OPTIONS", "Permission to view request status options in dropdowns");

        for (Map.Entry<String, String> entry : dropdownAliases.entrySet()) {
            String authority = entry.getKey();
            if (!permissionRepository.existsByAuthority(authority)) {
                Permission p = Permission.builder()
                        .resource("dropdowns")
                        .action(authority.toLowerCase())
                        .authority(authority)
                        .description(entry.getValue())
                        .isSystemPermission(true)
                        .build();
                permissionRepository.save(p);
            }
        }
        log.info("Baseline permissions seeded successfully");
    }
}
