package com.app.AreYouReporting.service.impl;

import com.app.AreYouReporting.Entities.AuditStatus;
import com.app.AreYouReporting.Entities.Department;
import com.app.AreYouReporting.Entities.SubDepartment;
import com.app.AreYouReporting.exceptions.DepartmentNotFoundException;
import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ResourceNotFoundException;
import com.app.AreYouReporting.exceptions.ScopeViolationException;
import com.app.AreYouReporting.mapper.DepartmentMapper;
import com.app.AreYouReporting.payload.request.DepartmentRequest;
import com.app.AreYouReporting.payload.request.SubDepartmentRequest;
import com.app.AreYouReporting.payload.response.DepartmentDto;
import com.app.AreYouReporting.payload.response.SubDepartmentDto;
import com.app.AreYouReporting.repository.DepartmentRepository;
import com.app.AreYouReporting.repository.SubDepartmentRepository;
import com.app.AreYouReporting.security.ActiveUserContext;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.security.UserPrincipal;
import com.app.AreYouReporting.service.interfaces.AuditService;
import com.app.AreYouReporting.service.interfaces.DepartmentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final DepartmentMapper departmentMapper;
    private final AuditService auditService;
    private final ScopeAuthorizationService scopeSecurity;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${spring.datasource.url:unknown}")
    private String datasourceUrl;

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllDepartments(boolean activeOnly) {
        scopeSecurity.validatePermission("departments.read");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();
        boolean isSuperAdmin = scopeSecurity.isSuperAdmin(principal);

        logDiagnostic("getAllDepartments", null, principal, context);

        List<Department> list = activeOnly ? departmentRepository.findByIsActiveTrue() : departmentRepository.findAll();

        if (!isSuperAdmin) {
            list = list.stream()
                    .filter(d -> scopeSecurity.canAccessDepartment(d.getId(), context, principal))
                    .collect(Collectors.toList());
        }

        return departmentMapper.toDtoList(list);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(UUID id) {
        scopeSecurity.validatePermission("departments.read");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        logDiagnostic("getDepartmentById", id, principal, context);

        Department dept = getDepartmentByIdOrThrow(id);
        if (!dept.isActive()) {
            log.warn("[DepartmentLookupFailed] Inactive department requested via getById: '{}'", id);
            throw new DepartmentNotFoundException(id);
        }

        if (!scopeSecurity.canAccessDepartment(dept.getId(), context, principal)) {
            log.warn("[DepartmentAccessDenied] User '{}' lacks authority to view department '{}' ({})",
                    principal.getUsername(), dept.getName(), id);
            throw new ScopeViolationException("Access denied: You do not have permission to view department: " + dept.getName());
        }

        return departmentMapper.toDto(dept);
    }

    @Override
    @Transactional(readOnly = true)
    public Department getDepartmentByIdOrThrow(UUID departmentId) {
        if (departmentId == null) {
            throw new DepartmentNotFoundException("Department ID must not be null");
        }
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> {
                    log.warn("[DepartmentLookupFailed] Department not found for ID: '{}' (profile={}, db={})",
                            departmentId, activeProfile, sanitizeDbUrl(datasourceUrl));
                    return new DepartmentNotFoundException(departmentId);
                });
    }

    @Override
    @Transactional
    public DepartmentDto createDepartment(DepartmentRequest request) {
        scopeSecurity.validatePermission("departments.create");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();
        logDiagnostic("createDepartment", null, principal, context);

        if (departmentRepository.existsByName(request.getName().trim())) {
            throw new InvalidOperationException("Department with name '" + request.getName().trim() + "' already exists");
        }
        if (departmentRepository.existsByCode(request.getCode().trim().toUpperCase())) {
            throw new InvalidOperationException("Department with code '" + request.getCode().trim().toUpperCase() + "' already exists");
        }

        Department dept = Department.builder()
                .name(request.getName().trim())
                .code(request.getCode().trim().toUpperCase())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Department saved = departmentRepository.save(dept);
        log.info("[DepartmentCreated] Department '{}' ({}) successfully created by user '{}'",
                saved.getName(), saved.getId(), principal.getUsername());
        auditService.log(principal.getUsername(), "DEPARTMENT", "CREATE_DEPARTMENT", "DEPARTMENT",
                saved.getId().toString(), null, null, saved.getName(), AuditStatus.SUCCESS, null);
        return departmentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public DepartmentDto updateDepartment(UUID id, DepartmentRequest request) {
        scopeSecurity.validatePermission("departments.update");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        logDiagnostic("updateDepartment", id, principal, context);

        // 1. Canonical lookup
        Department dept = getDepartmentByIdOrThrow(id);

        // 2. Authorization check separate from lookup (returns 403 Forbidden if not permitted)
        if (!scopeSecurity.canAccessDepartment(dept.getId(), context, principal)) {
            log.warn("[DepartmentUpdateForbidden] User '{}' lacks authority to update department '{}' ({})",
                    principal.getUsername(), dept.getName(), id);
            throw new ScopeViolationException("Access denied: You do not have permission to update department: " + dept.getName());
        }

        // 3. Unique validation
        if (request.getName() != null && !request.getName().isBlank() && !request.getName().trim().equalsIgnoreCase(dept.getName())) {
            String newName = request.getName().trim();
            if (departmentRepository.existsByName(newName)) {
                throw new InvalidOperationException("Department with name '" + newName + "' already exists");
            }
            dept.setName(newName);
        }

        if (request.getCode() != null && !request.getCode().isBlank() && !request.getCode().trim().equalsIgnoreCase(dept.getCode())) {
            String newCode = request.getCode().trim().toUpperCase();
            if (departmentRepository.existsByCode(newCode)) {
                throw new InvalidOperationException("Department with code '" + newCode + "' already exists");
            }
            dept.setCode(newCode);
        }

        if (request.getDescription() != null) {
            dept.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            dept.setActive(request.getIsActive());
        }

        Department updated = departmentRepository.save(dept);
        log.info("[DepartmentUpdated] Department '{}' ({}) successfully updated by user '{}'",
                updated.getName(), updated.getId(), principal.getUsername());
        auditService.log(principal.getUsername(), "DEPARTMENT", "UPDATE_DEPARTMENT", "DEPARTMENT",
                updated.getId().toString(), null, null, updated.getName(), AuditStatus.SUCCESS, null);
        return departmentMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteDepartment(UUID id) {
        scopeSecurity.validatePermission("departments.delete");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        logDiagnostic("deleteDepartment", id, principal, context);

        Department dept = getDepartmentByIdOrThrow(id);

        if (!scopeSecurity.canAccessDepartment(dept.getId(), context, principal)) {
            log.warn("[DepartmentDeleteForbidden] User '{}' lacks authority to deactivate department '{}' ({})",
                    principal.getUsername(), dept.getName(), id);
            throw new ScopeViolationException("Access denied: You do not have permission to deactivate department: " + dept.getName());
        }

        dept.setActive(false);
        departmentRepository.save(dept);
        log.info("[DepartmentDeactivated] Department '{}' ({}) deactivated by user '{}'",
                dept.getName(), id, principal.getUsername());
        auditService.log(principal.getUsername(), "DEPARTMENT", "DEACTIVATE_DEPARTMENT", "DEPARTMENT",
                id.toString(), null, dept.getName(), null, AuditStatus.SUCCESS, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubDepartmentDto> getSubDepartmentsByDeptId(UUID deptId, boolean activeOnly) {
        scopeSecurity.validatePermission("sub-departments.read");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        Department dept = getDepartmentByIdOrThrow(deptId);
        if (!dept.isActive()) {
            throw new DepartmentNotFoundException(deptId);
        }
        if (!scopeSecurity.canAccessDepartment(dept.getId(), context, principal)) {
            throw new ScopeViolationException("Access denied: You do not have permission to view sub-departments for department: " + dept.getName());
        }

        List<SubDepartment> list = activeOnly
                ? subDepartmentRepository.findByDepartmentIdAndIsActiveTrue(deptId)
                : subDepartmentRepository.findByDepartmentId(deptId);
        return departmentMapper.toSubDeptDtoList(list);
    }

    @Override
    @Transactional(readOnly = true)
    public SubDepartmentDto getSubDepartmentById(UUID id) {
        scopeSecurity.validatePermission("sub-departments.read");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        SubDepartment subDept = getSubDepartmentByIdOrThrow(id);
        if (!subDept.isActive() || (subDept.getDepartment() != null && !subDept.getDepartment().isActive())) {
            throw new ResourceNotFoundException("SubDepartment", "id", id);
        }
        UUID deptId = subDept.getDepartment() != null ? subDept.getDepartment().getId() : null;

        if (!scopeSecurity.canAccessSubDepartment(subDept.getId(), deptId, context, principal)) {
            throw new ScopeViolationException("Access denied: You do not have permission to view sub-department: " + subDept.getName());
        }

        return departmentMapper.toSubDeptDto(subDept);
    }

    @Override
    @Transactional(readOnly = true)
    public SubDepartment getSubDepartmentByIdOrThrow(UUID id) {
        if (id == null) {
            throw new ResourceNotFoundException("SubDepartment ID must not be null");
        }
        return subDepartmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubDepartment", "id", id));
    }

    @Override
    @Transactional
    public SubDepartmentDto createSubDepartment(UUID deptId, SubDepartmentRequest request) {
        scopeSecurity.validatePermission("sub-departments.create");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        Department dept = getDepartmentByIdOrThrow(deptId);
        if (!scopeSecurity.canAccessDepartment(dept.getId(), context, principal)) {
            throw new ScopeViolationException("Access denied: You cannot create sub-departments in department: " + dept.getName());
        }

        if (subDepartmentRepository.existsByDepartmentIdAndName(deptId, request.getName().trim())) {
            throw new InvalidOperationException("SubDepartment with name '" + request.getName() + "' already exists in this department");
        }
        if (subDepartmentRepository.existsByDepartmentIdAndCode(deptId, request.getCode().trim().toUpperCase())) {
            throw new InvalidOperationException("SubDepartment with code '" + request.getCode() + "' already exists in this department");
        }

        SubDepartment subDept = SubDepartment.builder()
                .department(dept)
                .name(request.getName().trim())
                .code(request.getCode().trim().toUpperCase())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        SubDepartment saved = subDepartmentRepository.save(subDept);
        auditService.log(principal.getUsername(), "SUB_DEPARTMENT", "CREATE_SUB_DEPARTMENT", "SUB_DEPARTMENT",
                saved.getId().toString(), null, null, saved.getName(), AuditStatus.SUCCESS, null);
        return departmentMapper.toSubDeptDto(saved);
    }

    @Override
    @Transactional
    public SubDepartmentDto updateSubDepartment(UUID id, SubDepartmentRequest request) {
        scopeSecurity.validatePermission("sub-departments.update");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        SubDepartment subDept = getSubDepartmentByIdOrThrow(id);
        UUID deptId = subDept.getDepartment() != null ? subDept.getDepartment().getId() : null;

        if (!scopeSecurity.canAccessSubDepartment(subDept.getId(), deptId, context, principal)) {
            throw new ScopeViolationException("Access denied: You cannot update sub-department: " + subDept.getName());
        }

        if (request.getName() != null && !request.getName().isBlank() && !request.getName().equalsIgnoreCase(subDept.getName())) {
            if (subDepartmentRepository.existsByDepartmentIdAndName(subDept.getDepartment().getId(), request.getName().trim())) {
                throw new InvalidOperationException("SubDepartment with name '" + request.getName() + "' already exists in this department");
            }
            subDept.setName(request.getName().trim());
        }

        if (request.getCode() != null && !request.getCode().isBlank() && !request.getCode().equalsIgnoreCase(subDept.getCode())) {
            if (subDepartmentRepository.existsByDepartmentIdAndCode(subDept.getDepartment().getId(), request.getCode().trim().toUpperCase())) {
                throw new InvalidOperationException("SubDepartment with code '" + request.getCode() + "' already exists in this department");
            }
            subDept.setCode(request.getCode().trim().toUpperCase());
        }

        if (request.getDescription() != null) {
            subDept.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            subDept.setActive(request.getIsActive());
        }

        SubDepartment updated = subDepartmentRepository.save(subDept);
        auditService.log(principal.getUsername(), "SUB_DEPARTMENT", "UPDATE_SUB_DEPARTMENT", "SUB_DEPARTMENT",
                updated.getId().toString(), null, null, updated.getName(), AuditStatus.SUCCESS, null);
        return departmentMapper.toSubDeptDto(updated);
    }

    @Override
    @Transactional
    public void deleteSubDepartment(UUID id) {
        scopeSecurity.validatePermission("sub-departments.delete");

        UserPrincipal principal = scopeSecurity.getCurrentPrincipal();
        ActiveUserContext context = getActiveContext();

        SubDepartment subDept = getSubDepartmentByIdOrThrow(id);
        UUID deptId = subDept.getDepartment() != null ? subDept.getDepartment().getId() : null;

        if (!scopeSecurity.canAccessSubDepartment(subDept.getId(), deptId, context, principal)) {
            throw new ScopeViolationException("Access denied: You cannot deactivate sub-department: " + subDept.getName());
        }

        subDept.setActive(false);
        subDepartmentRepository.save(subDept);
        auditService.log(principal.getUsername(), "SUB_DEPARTMENT", "DEACTIVATE_SUB_DEPARTMENT", "SUB_DEPARTMENT",
                id.toString(), null, subDept.getName(), null, AuditStatus.SUCCESS, null);
    }

    // Diagnostic and helper routines

    private void logDiagnostic(String operation, UUID requestedId, UserPrincipal principal, ActiveUserContext context) {
        if (log.isDebugEnabled() || log.isInfoEnabled()) {
            String sanitizedDb = sanitizeDbUrl(datasourceUrl);
            String username = principal != null ? principal.getUsername() : "anonymous";
            String scope = context != null && context.getEffectiveDataScope() != null ? context.getEffectiveDataScope().name() : "N/A";
            log.debug("[DepartmentDiagnostic] op={} targetId={} user={} scope={} profile={} db={}",
                    operation, requestedId, username, scope, activeProfile, sanitizedDb);
        }
    }

    private String sanitizeDbUrl(String url) {
        if (url == null) return "unknown";
        int qIdx = url.indexOf('?');
        return qIdx != -1 ? url.substring(0, qIdx) : url;
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
}
