package com.app.AreYouReporting.service.impl;

import com.app.AreYouReporting.Entities.*;
import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ResourceNotFoundException;
import com.app.AreYouReporting.mapper.PermissionMapper;
import com.app.AreYouReporting.mapper.UserMapper;
import com.app.AreYouReporting.payload.request.DataScopeOverrideRequest;
import com.app.AreYouReporting.payload.request.UserCreateRequest;
import com.app.AreYouReporting.payload.request.UserRoleAssignmentRequest;
import com.app.AreYouReporting.payload.request.UserUpdateRequest;
import com.app.AreYouReporting.payload.response.PageResponse;
import com.app.AreYouReporting.payload.response.ResourceGrantsDto;
import com.app.AreYouReporting.payload.response.UserDto;
import com.app.AreYouReporting.payload.response.UserSummaryDto;
import com.app.AreYouReporting.repository.*;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.service.interfaces.AuditService;
import com.app.AreYouReporting.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final PermissionRepository permissionRepository;
    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final ScopeAuthorizationService scopeSecurity;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDto> getAllUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        List<Permission> allPermissions = permissionRepository.findAllByOrderByResourceAscActionAsc();

        List<UserDto> dtos = page.getContent().stream().map(user -> {
            Set<Permission> userPerms = extractUserPermissions(user);
            List<ResourceGrantsDto> grants = permissionMapper.toResourceGrants(allPermissions, userPerms);
            return userMapper.toDto(user, grants);
        }).collect(Collectors.toList());

        return PageResponse.of(page, dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        List<Permission> allPermissions = permissionRepository.findAllByOrderByResourceAscActionAsc();
        Set<Permission> userPerms = extractUserPermissions(user);
        List<ResourceGrantsDto> grants = permissionMapper.toResourceGrants(allPermissions, userPerms);

        return userMapper.toDto(user, grants);
    }

    @Override
    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        scopeSecurity.validatePermission("users.create");

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new InvalidOperationException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new InvalidOperationException("Email '" + request.getEmail() + "' is already registered");
        }

        Set<Department> depts = new HashSet<>();
        if (request.getDepartmentIds() != null && !request.getDepartmentIds().isEmpty()) {
            depts.addAll(departmentRepository.findAllById(request.getDepartmentIds()));
        }

        Set<SubDepartment> subDepts = new HashSet<>();
        if (request.getSubDepartmentIds() != null && !request.getSubDepartmentIds().isEmpty()) {
            subDepts.addAll(subDepartmentRepository.findAllById(request.getSubDepartmentIds()));
        }

        User user = User.builder()
                .username(request.getUsername().trim().toLowerCase())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phoneNumber(request.getPhoneNumber())
                .isActive(true)
                .departments(depts)
                .subDepartments(subDepts)
                .build();

        User savedUser = userRepository.save(user);

        // Process role assignments
        if (request.getRoleAssignments() != null && !request.getRoleAssignments().isEmpty()) {
            for (UserRoleAssignmentRequest rar : request.getRoleAssignments()) {
                assignRoleInternal(savedUser, rar);
            }
        }

        User fullySaved = userRepository.findById(savedUser.getId()).orElse(savedUser);
        auditService.log(scopeSecurity.getCurrentUsername(), "USER", "CREATE_USER", "USER", fullySaved.getId().toString(), null, null, fullySaved.getUsername(), AuditStatus.SUCCESS, null);

        List<Permission> allPermissions = permissionRepository.findAllByOrderByResourceAscActionAsc();
        Set<Permission> userPerms = extractUserPermissions(fullySaved);
        return userMapper.toDto(fullySaved, permissionMapper.toResourceGrants(allPermissions, userPerms));
    }

    @Override
    @Transactional
    public UserDto updateUser(UUID id, UserUpdateRequest request) {
        scopeSecurity.validatePermission("users.update");

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (request.getEmail() != null && !request.getEmail().isBlank() && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
                throw new InvalidOperationException("Email '" + request.getEmail() + "' is already registered");
            }
            user.setEmail(request.getEmail().trim().toLowerCase());
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getIsActive() != null) {
            user.setActive(request.getIsActive());
        }

        if (request.getDepartmentIds() != null) {
            user.setDepartments(new HashSet<>(departmentRepository.findAllById(request.getDepartmentIds())));
        }
        if (request.getSubDepartmentIds() != null) {
            user.setSubDepartments(new HashSet<>(subDepartmentRepository.findAllById(request.getSubDepartmentIds())));
        }

        User updated = userRepository.save(user);
        auditService.log(scopeSecurity.getCurrentUsername(), "USER", "UPDATE_USER", "USER", updated.getId().toString(), null, null, updated.getUsername(), AuditStatus.SUCCESS, null);

        List<Permission> allPermissions = permissionRepository.findAllByOrderByResourceAscActionAsc();
        Set<Permission> userPerms = extractUserPermissions(updated);
        return userMapper.toDto(updated, permissionMapper.toResourceGrants(allPermissions, userPerms));
    }

    @Override
    @Transactional
    public UserDto assignRoleToUser(UUID userId, UserRoleAssignmentRequest request) {
        scopeSecurity.validatePermission("users.assign-role");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        assignRoleInternal(user, request);

        User refreshed = userRepository.findById(userId).orElse(user);
        auditService.log(scopeSecurity.getCurrentUsername(), "USER", "ASSIGN_ROLE", "USER", userId.toString(), null, null, "Assigned roleId=" + request.getRoleId(), AuditStatus.SUCCESS, null);

        List<Permission> allPermissions = permissionRepository.findAllByOrderByResourceAscActionAsc();
        Set<Permission> userPerms = extractUserPermissions(refreshed);
        return userMapper.toDto(refreshed, permissionMapper.toResourceGrants(allPermissions, userPerms));
    }

    private void assignRoleInternal(User user, UserRoleAssignmentRequest request) {
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", request.getRoleId()));

        Department dept = null;
        if (request.getDepartmentId() != null) {
            dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            user.getDepartments().add(dept);
        }

        SubDepartment subDept = null;
        if (request.getSubDepartmentId() != null) {
            subDept = subDepartmentRepository.findById(request.getSubDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("SubDepartment", "id", request.getSubDepartmentId()));
            user.getSubDepartments().add(subDept);
        }

        Set<Department> customDepts = new HashSet<>();
        if (request.getCustomDepartmentIds() != null && !request.getCustomDepartmentIds().isEmpty()) {
            customDepts.addAll(departmentRepository.findAllById(request.getCustomDepartmentIds()));
        }

        Set<SubDepartment> customSubDepts = new HashSet<>();
        if (request.getCustomSubDepartmentIds() != null && !request.getCustomSubDepartmentIds().isEmpty()) {
            customSubDepts.addAll(subDepartmentRepository.findAllById(request.getCustomSubDepartmentIds()));
        }

        UserRoleAssignment assignment = UserRoleAssignment.builder()
                .user(user)
                .role(role)
                .department(dept)
                .subDepartment(subDept)
                .dataScopeType(request.getDataScopeType() != null ? request.getDataScopeType() : role.getDefaultDataScope())
                .customDepartments(customDepts)
                .customSubDepartments(customSubDepts)
                .isActive(true)
                .build();

        userRoleAssignmentRepository.save(assignment);
        user.getRoleAssignments().add(assignment);
    }

    @Override
    @Transactional
    public void removeRoleAssignment(UUID assignmentId) {
        scopeSecurity.validatePermission("users.assign-role");

        UserRoleAssignment assignment = userRoleAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("UserRoleAssignment", "id", assignmentId));

        assignment.setActive(false);
        userRoleAssignmentRepository.save(assignment);
        auditService.log(scopeSecurity.getCurrentUsername(), "USER", "DEACTIVATE_ROLE_ASSIGNMENT", "USER_ROLE_ASSIGNMENT", assignmentId.toString(), null, null, null, AuditStatus.SUCCESS, null);
    }

    @Override
    @Transactional
    public UserDto overrideUserDataScope(UUID assignmentId, DataScopeOverrideRequest request) {
        scopeSecurity.validatePermission("users.manage-scope");

        UserRoleAssignment assignment = userRoleAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("UserRoleAssignment", "id", assignmentId));

        assignment.setDataScopeType(request.getDataScopeType());

        if (request.getCustomDepartmentIds() != null) {
            assignment.setCustomDepartments(new HashSet<>(departmentRepository.findAllById(request.getCustomDepartmentIds())));
        }
        if (request.getCustomSubDepartmentIds() != null) {
            assignment.setCustomSubDepartments(new HashSet<>(subDepartmentRepository.findAllById(request.getCustomSubDepartmentIds())));
        }

        userRoleAssignmentRepository.save(assignment);
        auditService.log(scopeSecurity.getCurrentUsername(), "USER", "OVERRIDE_DATA_SCOPE", "USER_ROLE_ASSIGNMENT", assignmentId.toString(), null, null, request.getDataScopeType().name(), AuditStatus.SUCCESS, null);

        return getUserById(assignment.getUser().getId());
    }

    @Override
    @Transactional
    public void deactivateUser(UUID id) {
        scopeSecurity.validatePermission("users.delete");

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setActive(false);
        userRepository.save(user);
        auditService.log(scopeSecurity.getCurrentUsername(), "USER", "DEACTIVATE_USER", "USER", id.toString(), null, user.getUsername(), null, AuditStatus.SUCCESS, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getUsersByDepartment(UUID departmentId) {
        return userMapper.toSummaryDtoList(userRepository.findByDepartmentId(departmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getUsersBySubDepartment(UUID subDepartmentId) {
        return userMapper.toSummaryDtoList(userRepository.findBySubDepartmentId(subDepartmentId));
    }

    private Set<Permission> extractUserPermissions(User user) {
        Set<Permission> permissions = new HashSet<>();
        if (user.getRoleAssignments() != null) {
            for (UserRoleAssignment assignment : user.getRoleAssignments()) {
                if (assignment.isActive() && assignment.getRole() != null && assignment.getRole().getPermissions() != null) {
                    permissions.addAll(assignment.getRole().getPermissions());
                }
            }
        }
        return permissions;
    }
}
