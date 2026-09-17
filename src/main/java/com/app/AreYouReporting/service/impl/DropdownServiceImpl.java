package com.app.AreYouReporting.service.impl;

import com.app.AreYouReporting.Entities.*;
import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ScopeViolationException;
import com.app.AreYouReporting.mapper.DropdownMapper;
import com.app.AreYouReporting.payload.response.PageResponse;
import com.app.AreYouReporting.payload.response.dropdown.*;
import com.app.AreYouReporting.repository.*;
import com.app.AreYouReporting.security.ActiveUserContext;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.security.UserPrincipal;
import com.app.AreYouReporting.service.interfaces.DropdownService;
import com.app.AreYouReporting.specification.DropdownSpecification;
import com.app.AreYouReporting.specification.TaskSpecification;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DropdownServiceImpl implements DropdownService {

    private final DepartmentRepository departmentRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final UserRepository userRepository;
    private final TaskTemplateRepository templateRepository;
    private final TaskRepository taskRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final DropdownMapper dropdownMapper;
    private final ScopeAuthorizationService scopeSecurity;

    private static final Set<String> ALLOWED_DEPT_SORT = Set.of("id", "name", "code", "createdAt", "updatedAt", "isActive");
    private static final Set<String> ALLOWED_SUBDEPT_SORT = Set.of("id", "name", "code", "createdAt", "updatedAt", "isActive");
    private static final Set<String> ALLOWED_USER_SORT = Set.of("id", "fullName", "username", "email", "createdAt", "updatedAt", "isActive");
    private static final Set<String> ALLOWED_TEMPLATE_SORT = Set.of("id", "name", "defaultPriority", "defaultDurationDays", "createdAt", "updatedAt", "isActive");
    private static final Set<String> ALLOWED_TASK_SORT = Set.of("id", "title", "status", "priority", "dueDate", "createdAt", "updatedAt");
    private static final Set<String> ALLOWED_ROLE_SORT = Set.of("id", "name", "description", "defaultDataScope", "createdAt", "updatedAt");
    private static final Set<String> ALLOWED_PERMISSION_SORT = Set.of("id", "authority", "resource", "action", "description", "createdAt");

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DepartmentDropdownDto> getDepartments(String search, Boolean activeOnly, Pageable pageable) {
        scopeSecurity.validatePermission("dropdowns.departments");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();
        boolean isSuperAdmin = scopeSecurity.isSuperAdmin(principal);

        Set<UUID> allowedDeptIds = resolveAllowedDepartmentIds(principal, context, isSuperAdmin);

        Pageable safePageable = sanitizePageable(pageable, ALLOWED_DEPT_SORT, "name");
        Specification<Department> spec = DropdownSpecification.departmentFilter(
                search,
                activeOnly != null ? activeOnly : true,
                allowedDeptIds,
                isSuperAdmin
        );

        Page<Department> page = departmentRepository.findAll(spec, safePageable);
        return PageResponse.of(page, dropdownMapper.toDepartmentDropdownDtoList(page.getContent()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubDepartmentDropdownDto> getSubDepartments(UUID departmentId, String search, Boolean activeOnly, Pageable pageable) {
        scopeSecurity.validatePermission("dropdowns.sub-departments");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();
        boolean isSuperAdmin = scopeSecurity.isSuperAdmin(principal);

        if (departmentId != null && !scopeSecurity.canAccessDepartment(departmentId, context, principal)) {
            log.warn("Unauthorized access attempt to sub-departments for departmentId: {} by user: {}", departmentId, principal.getUsername());
            throw new ScopeViolationException("Access denied: You do not have permission to view sub-departments for department ID: " + departmentId);
        }

        Set<UUID> allowedSubDeptIds = resolveAllowedSubDepartmentIds(principal, context, isSuperAdmin);
        Set<UUID> allowedDeptIds = resolveAllowedDepartmentIds(principal, context, isSuperAdmin);

        Pageable safePageable = sanitizePageable(pageable, ALLOWED_SUBDEPT_SORT, "name");
        Specification<SubDepartment> spec = DropdownSpecification.subDepartmentFilter(
                departmentId,
                search,
                activeOnly != null ? activeOnly : true,
                allowedSubDeptIds,
                allowedDeptIds,
                isSuperAdmin
        );

        Page<SubDepartment> page = subDepartmentRepository.findAll(spec, safePageable);
        return PageResponse.of(page, dropdownMapper.toSubDepartmentDropdownDtoList(page.getContent()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDropdownDto> getUsers(UUID departmentId, UUID subDepartmentId, String role, String search, Boolean activeOnly, Pageable pageable) {
        scopeSecurity.validatePermission("dropdowns.users");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();
        boolean isSuperAdmin = scopeSecurity.isSuperAdmin(principal);

        validateScopeIds(departmentId, subDepartmentId, context, principal);

        Set<UUID> allowedDeptIds = resolveAllowedDepartmentIds(principal, context, isSuperAdmin);
        Set<UUID> allowedSubDeptIds = resolveAllowedSubDepartmentIds(principal, context, isSuperAdmin);

        Pageable safePageable = sanitizePageable(pageable, ALLOWED_USER_SORT, "fullName");
        Specification<User> spec = DropdownSpecification.userFilter(
                departmentId,
                subDepartmentId,
                role,
                search,
                activeOnly != null ? activeOnly : true,
                allowedDeptIds,
                allowedSubDeptIds,
                isSuperAdmin
        );

        Page<User> page = userRepository.findAll(spec, safePageable);
        return PageResponse.of(page, dropdownMapper.toUserDropdownDtoList(page.getContent()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDropdownDto> getEligibleAssignees(UUID departmentId, UUID subDepartmentId, String search, Pageable pageable) {
        scopeSecurity.validatePermission("dropdowns.users");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();
        boolean isSuperAdmin = scopeSecurity.isSuperAdmin(principal);

        validateScopeIds(departmentId, subDepartmentId, context, principal);

        Set<UUID> allowedDeptIds = resolveAllowedDepartmentIds(principal, context, isSuperAdmin);
        Set<UUID> allowedSubDeptIds = resolveAllowedSubDepartmentIds(principal, context, isSuperAdmin);

        Pageable safePageable = sanitizePageable(pageable, ALLOWED_USER_SORT, "fullName");
        Specification<User> spec = DropdownSpecification.userFilter(
                departmentId,
                subDepartmentId,
                null,
                search,
                true,
                allowedDeptIds,
                allowedSubDeptIds,
                isSuperAdmin
        );

        Page<User> page = userRepository.findAll(spec, safePageable);
        return PageResponse.of(page, dropdownMapper.toUserDropdownDtoList(page.getContent()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDropdownDto> getEligibleReportingManagers(UUID departmentId, UUID subDepartmentId, String search, Pageable pageable) {
        scopeSecurity.validatePermission("dropdowns.users");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();
        boolean isSuperAdmin = scopeSecurity.isSuperAdmin(principal);

        validateScopeIds(departmentId, subDepartmentId, context, principal);

        Set<UUID> allowedDeptIds = resolveAllowedDepartmentIds(principal, context, isSuperAdmin);
        Set<UUID> allowedSubDeptIds = resolveAllowedSubDepartmentIds(principal, context, isSuperAdmin);

        Pageable safePageable = sanitizePageable(pageable, ALLOWED_USER_SORT, "fullName");

        // Reporting managers include users holding leadership roles: HOD, SUB_ADMIN, ADMIN, SUPER_ADMIN
        Specification<User> spec = (root, query, cb) -> {
            query.distinct(true);
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("isActive")));

            if (org.springframework.util.StringUtils.hasText(search)) {
                String searchPattern = "%" + search.toLowerCase().trim() + "%";
                jakarta.persistence.criteria.Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), searchPattern);
                jakarta.persistence.criteria.Predicate usernameMatch = cb.like(cb.lower(root.get("username")), searchPattern);
                predicates.add(cb.or(nameMatch, usernameMatch));
            }

            jakarta.persistence.criteria.Join<User, UserRoleAssignment> uraJoin = root.join("roleAssignments", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.isTrue(uraJoin.get("isActive")));

            jakarta.persistence.criteria.Predicate managerialRole = uraJoin.get("role").get("name").in(
                    List.of("SUPER_ADMIN", "ADMIN", "SUB_ADMIN", "HOD")
            );
            predicates.add(managerialRole);

            if (departmentId != null) {
                jakarta.persistence.criteria.Join<User, Department> deptJoin = root.join("departments", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.or(cb.equal(deptJoin.get("id"), departmentId), cb.equal(uraJoin.get("role").get("name"), "SUPER_ADMIN")));
            }

            if (subDepartmentId != null) {
                jakarta.persistence.criteria.Join<User, SubDepartment> subDeptJoin = root.join("subDepartments", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.or(
                        cb.equal(subDeptJoin.get("id"), subDepartmentId),
                        uraJoin.get("role").get("name").in(List.of("SUPER_ADMIN", "ADMIN"))
                ));
            }

            if (!isSuperAdmin) {
                List<jakarta.persistence.criteria.Predicate> scopePredicates = new ArrayList<>();
                if (allowedSubDeptIds != null && !allowedSubDeptIds.isEmpty()) {
                    jakarta.persistence.criteria.Join<User, SubDepartment> subDeptJoin = root.join("subDepartments", jakarta.persistence.criteria.JoinType.LEFT);
                    scopePredicates.add(subDeptJoin.get("id").in(allowedSubDeptIds));
                }
                if (allowedDeptIds != null && !allowedDeptIds.isEmpty()) {
                    jakarta.persistence.criteria.Join<User, Department> deptJoin = root.join("departments", jakarta.persistence.criteria.JoinType.LEFT);
                    scopePredicates.add(deptJoin.get("id").in(allowedDeptIds));
                }

                scopePredicates.add(uraJoin.get("role").get("name").in(List.of("SUPER_ADMIN", "ADMIN")));
                predicates.add(cb.or(scopePredicates.toArray(new jakarta.persistence.criteria.Predicate[0])));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<User> page = userRepository.findAll(spec, safePageable);
        return PageResponse.of(page, dropdownMapper.toUserDropdownDtoList(page.getContent()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskTemplateDropdownDto> getTaskTemplates(UUID departmentId, UUID subDepartmentId, String search, Boolean activeOnly, Pageable pageable) {
        scopeSecurity.validatePermission("dropdowns.templates");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();
        boolean isSuperAdmin = scopeSecurity.isSuperAdmin(principal);

        validateScopeIds(departmentId, subDepartmentId, context, principal);

        Set<UUID> allowedDeptIds = resolveAllowedDepartmentIds(principal, context, isSuperAdmin);
        Set<UUID> allowedSubDeptIds = resolveAllowedSubDepartmentIds(principal, context, isSuperAdmin);

        Pageable safePageable = sanitizePageable(pageable, ALLOWED_TEMPLATE_SORT, "name");
        Specification<TaskTemplate> spec = DropdownSpecification.templateFilter(
                departmentId,
                subDepartmentId,
                search,
                activeOnly != null ? activeOnly : true,
                allowedDeptIds,
                allowedSubDeptIds,
                isSuperAdmin
        );

        Page<TaskTemplate> page = templateRepository.findAll(spec, safePageable);
        return PageResponse.of(page, dropdownMapper.toTaskTemplateDropdownDtoList(page.getContent()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskDropdownDto> getTasks(UUID departmentId, UUID subDepartmentId, TaskStatus status, TaskPriority priority, Boolean isSelfTask, String search, Pageable pageable) {
        scopeSecurity.validatePermission("dropdowns.tasks");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();
        boolean isSuperAdmin = scopeSecurity.isSuperAdmin(principal);

        validateScopeIds(departmentId, subDepartmentId, context, principal);

        Pageable safePageable = sanitizePageable(pageable, ALLOWED_TASK_SORT, "dueDate");
        Specification<Task> spec = TaskSpecification.filter(
                search,
                status != null ? List.of(status) : null,
                priority,
                isSelfTask,
                null,
                null,
                null,
                departmentId,
                subDepartmentId,
                null,
                null,
                context,
                principal,
                isSuperAdmin
        );

        Page<Task> page = taskRepository.findAll(spec, safePageable);
        return PageResponse.of(page, dropdownMapper.toTaskDropdownDtoList(page.getContent()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoleDropdownDto> getRoles(String search, Pageable pageable) {
        scopeSecurity.validatePermission("dropdowns.roles");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        boolean isSuperAdmin = scopeSecurity.isSuperAdmin(principal);

        List<String> allowedRoleNames = Collections.emptyList();
        if (!isSuperAdmin) {
            boolean isAdmin = principal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
            if (isAdmin) {
                allowedRoleNames = List.of("ADMIN", "SUB_ADMIN", "HOD", "TEACHER");
            } else {
                allowedRoleNames = List.of("SUB_ADMIN", "HOD", "TEACHER");
            }
        }

        Pageable safePageable = sanitizePageable(pageable, ALLOWED_ROLE_SORT, "name");
        Specification<Role> spec = DropdownSpecification.roleFilter(search, allowedRoleNames, isSuperAdmin);

        Page<Role> page = roleRepository.findAll(spec, safePageable);
        return PageResponse.of(page, dropdownMapper.toRoleDropdownDtoList(page.getContent()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PermissionDropdownDto> getPermissions(String resource, String search, Pageable pageable) {
        scopeSecurity.validatePermission("dropdowns.permissions");

        Pageable safePageable = sanitizePageable(pageable, ALLOWED_PERMISSION_SORT, "authority");
        Specification<Permission> spec = DropdownSpecification.permissionFilter(resource, search);

        Page<Permission> page = permissionRepository.findAll(spec, safePageable);
        return PageResponse.of(page, dropdownMapper.toPermissionDropdownDtoList(page.getContent()));
    }

    @Override
    public List<StaticOptionDto> getTaskStatuses() {
        return Arrays.stream(TaskStatus.values())
                .filter(s -> s != TaskStatus.DELETED)
                .map(s -> StaticOptionDto.builder()
                        .value(s.name())
                        .label(formatEnumLabel(s.name()))
                        .description("Task lifecycle status: " + s.name())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<StaticOptionDto> getRequestStatuses() {
        return Arrays.stream(TaskRequestStatus.values())
                .map(s -> StaticOptionDto.builder()
                        .value(s.name())
                        .label(formatEnumLabel(s.name()))
                        .description("Task approval request status: " + s.name())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<StaticOptionDto> getTaskPriorities() {
        return Arrays.stream(TaskPriority.values())
                .map(p -> StaticOptionDto.builder()
                        .value(p.name())
                        .label(formatEnumLabel(p.name()))
                        .description("Task priority level: " + p.name())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<StaticOptionDto> getProofRequirementTypes() {
        return Arrays.stream(ProofType.values())
                .map(pt -> StaticOptionDto.builder()
                        .value(pt.name())
                        .label(formatEnumLabel(pt.name()))
                        .description("Acceptable proof attachment format: " + pt.name())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<StaticOptionDto> getDataScopeTypes() {
        return Arrays.stream(DataScopeType.values())
                .map(ds -> StaticOptionDto.builder()
                        .value(ds.name())
                        .label(formatEnumLabel(ds.name()))
                        .description("Authorization data access boundary: " + ds.name())
                        .build())
                .collect(Collectors.toList());
    }

    // Helper Methods

    private void validateScopeIds(UUID departmentId, UUID subDepartmentId, ActiveUserContext context, UserPrincipal principal) {
        if (departmentId != null && !scopeSecurity.canAccessDepartment(departmentId, context, principal)) {
            log.warn("Access denied to department ID: {} by user: {}", departmentId, principal.getUsername());
            throw new ScopeViolationException("Access denied: You do not have permission to access department ID: " + departmentId);
        }

        if (subDepartmentId != null && !scopeSecurity.canAccessSubDepartment(subDepartmentId, departmentId, context, principal)) {
            log.warn("Access denied to sub-department ID: {} by user: {}", subDepartmentId, principal.getUsername());
            throw new ScopeViolationException("Access denied: You do not have permission to access sub-department ID: " + subDepartmentId);
        }
    }

    private Set<UUID> resolveAllowedDepartmentIds(UserPrincipal principal, ActiveUserContext context, boolean isSuperAdmin) {
        if (isSuperAdmin) return Collections.emptySet();

        Set<UUID> deptIds = new HashSet<>();
        if (context != null) {
            if (context.getDepartmentId() != null) {
                deptIds.add(context.getDepartmentId());
            }
            if (context.getCustomDepartmentIds() != null) {
                deptIds.addAll(context.getCustomDepartmentIds());
            }
        }

        if (principal.getDepartmentIds() != null) {
            deptIds.addAll(principal.getDepartmentIds());
        }

        if (principal.getAssignments() != null) {
            for (UserRoleAssignment assignment : principal.getAssignments()) {
                if (assignment.isActive()) {
                    if (assignment.getDepartment() != null) {
                        deptIds.add(assignment.getDepartment().getId());
                    }
                    if (assignment.getCustomDepartments() != null) {
                        deptIds.addAll(assignment.getCustomDepartments().stream().map(Department::getId).collect(Collectors.toSet()));
                    }
                }
            }
        }

        return deptIds;
    }

    private Set<UUID> resolveAllowedSubDepartmentIds(UserPrincipal principal, ActiveUserContext context, boolean isSuperAdmin) {
        if (isSuperAdmin) return Collections.emptySet();

        Set<UUID> subDeptIds = new HashSet<>();
        if (context != null) {
            if (context.getSubDepartmentId() != null) {
                subDeptIds.add(context.getSubDepartmentId());
            }
            if (context.getCustomSubDepartmentIds() != null) {
                subDeptIds.addAll(context.getCustomSubDepartmentIds());
            }
        }

        if (principal.getSubDepartmentIds() != null) {
            subDeptIds.addAll(principal.getSubDepartmentIds());
        }

        if (principal.getAssignments() != null) {
            for (UserRoleAssignment assignment : principal.getAssignments()) {
                if (assignment.isActive()) {
                    if (assignment.getSubDepartment() != null) {
                        subDeptIds.add(assignment.getSubDepartment().getId());
                    }
                    if (assignment.getCustomSubDepartments() != null) {
                        subDeptIds.addAll(assignment.getCustomSubDepartments().stream().map(SubDepartment::getId).collect(Collectors.toSet()));
                    }
                }
            }
        }

        return subDeptIds;
    }

    private Pageable sanitizePageable(Pageable pageable, Set<String> allowedFields, String defaultSortField) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, defaultSortField));
        }

        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.min(Math.max(1, pageable.getPageSize()), 100);

        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, defaultSortField));
        }

        List<Sort.Order> validOrders = new ArrayList<>();
        for (Sort.Order order : sort) {
            String property = order.getProperty();
            if (!allowedFields.contains(property)) {
                throw new InvalidOperationException("Invalid sort field: '" + property + "'. Allowed sort fields: " + allowedFields);
            }
            validOrders.add(order);
        }

        return PageRequest.of(page, size, Sort.by(validOrders));
    }

    private ActiveUserContext getActiveContext() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            Object ctx = request.getAttribute(ScopeAuthorizationService.CONTEXT_ATTRIBUTE);
            if (ctx instanceof ActiveUserContext) {
                return (ActiveUserContext) ctx;
            }
        }
        return null;
    }

    private String formatEnumLabel(String name) {
        if (name == null) return "";
        return Arrays.stream(name.split("_"))
                .map(word -> word.isEmpty() ? "" : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
