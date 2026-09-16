package com.app.AreYouReporting.service.impl;

import com.app.AreYouReporting.Entities.AuditStatus;
import com.app.AreYouReporting.Entities.Department;
import com.app.AreYouReporting.Entities.SubDepartment;
import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ResourceNotFoundException;
import com.app.AreYouReporting.mapper.DepartmentMapper;
import com.app.AreYouReporting.payload.request.DepartmentRequest;
import com.app.AreYouReporting.payload.request.SubDepartmentRequest;
import com.app.AreYouReporting.payload.response.DepartmentDto;
import com.app.AreYouReporting.payload.response.SubDepartmentDto;
import com.app.AreYouReporting.repository.DepartmentRepository;
import com.app.AreYouReporting.repository.SubDepartmentRepository;
import com.app.AreYouReporting.security.ScopeAuthorizationService;
import com.app.AreYouReporting.service.interfaces.AuditService;
import com.app.AreYouReporting.service.interfaces.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final DepartmentMapper departmentMapper;
    private final AuditService auditService;
    private final ScopeAuthorizationService scopeSecurity;

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllDepartments(boolean activeOnly) {
        List<Department> list = activeOnly ? departmentRepository.findByIsActiveTrue() : departmentRepository.findAll();
        return departmentMapper.toDtoList(list);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(UUID id) {
        Department dept = departmentRepository.findByIdWithSubDepartments(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return departmentMapper.toDto(dept);
    }

    @Override
    @Transactional
    public DepartmentDto createDepartment(DepartmentRequest request) {
        scopeSecurity.validatePermission("departments.create");

        if (departmentRepository.existsByName(request.getName())) {
            throw new InvalidOperationException("Department with name '" + request.getName() + "' already exists");
        }
        if (departmentRepository.existsByCode(request.getCode())) {
            throw new InvalidOperationException("Department with code '" + request.getCode() + "' already exists");
        }

        Department dept = Department.builder()
                .name(request.getName().trim())
                .code(request.getCode().trim().toUpperCase())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Department saved = departmentRepository.save(dept);
        auditService.log(scopeSecurity.getCurrentUsername(), "DEPARTMENT", "CREATE_DEPARTMENT", "DEPARTMENT", saved.getId().toString(), null, null, saved.getName(), AuditStatus.SUCCESS, null);
        return departmentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public DepartmentDto updateDepartment(UUID id, DepartmentRequest request) {
        scopeSecurity.validatePermission("departments.update");

        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        if (request.getName() != null && !request.getName().isBlank() && !request.getName().equalsIgnoreCase(dept.getName())) {
            if (departmentRepository.existsByName(request.getName().trim())) {
                throw new InvalidOperationException("Department with name '" + request.getName() + "' already exists");
            }
            dept.setName(request.getName().trim());
        }

        if (request.getCode() != null && !request.getCode().isBlank() && !request.getCode().equalsIgnoreCase(dept.getCode())) {
            if (departmentRepository.existsByCode(request.getCode().trim().toUpperCase())) {
                throw new InvalidOperationException("Department with code '" + request.getCode() + "' already exists");
            }
            dept.setCode(request.getCode().trim().toUpperCase());
        }

        if (request.getDescription() != null) {
            dept.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            dept.setActive(request.getIsActive());
        }

        Department updated = departmentRepository.save(dept);
        auditService.log(scopeSecurity.getCurrentUsername(), "DEPARTMENT", "UPDATE_DEPARTMENT", "DEPARTMENT", updated.getId().toString(), null, null, updated.getName(), AuditStatus.SUCCESS, null);
        return departmentMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteDepartment(UUID id) {
        scopeSecurity.validatePermission("departments.delete");

        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        dept.setActive(false);
        departmentRepository.save(dept);
        auditService.log(scopeSecurity.getCurrentUsername(), "DEPARTMENT", "DEACTIVATE_DEPARTMENT", "DEPARTMENT", id.toString(), null, dept.getName(), null, AuditStatus.SUCCESS, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubDepartmentDto> getSubDepartmentsByDeptId(UUID deptId, boolean activeOnly) {
        List<SubDepartment> list = activeOnly
                ? subDepartmentRepository.findByDepartmentIdAndIsActiveTrue(deptId)
                : subDepartmentRepository.findByDepartmentId(deptId);
        return departmentMapper.toSubDeptDtoList(list);
    }

    @Override
    @Transactional(readOnly = true)
    public SubDepartmentDto getSubDepartmentById(UUID id) {
        SubDepartment subDept = subDepartmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubDepartment", "id", id));
        return departmentMapper.toSubDeptDto(subDept);
    }

    @Override
    @Transactional
    public SubDepartmentDto createSubDepartment(UUID deptId, SubDepartmentRequest request) {
        scopeSecurity.validatePermission("sub-departments.create");

        Department dept = departmentRepository.findById(deptId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", deptId));

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
        auditService.log(scopeSecurity.getCurrentUsername(), "SUB_DEPARTMENT", "CREATE_SUB_DEPARTMENT", "SUB_DEPARTMENT", saved.getId().toString(), null, null, saved.getName(), AuditStatus.SUCCESS, null);
        return departmentMapper.toSubDeptDto(saved);
    }

    @Override
    @Transactional
    public SubDepartmentDto updateSubDepartment(UUID id, SubDepartmentRequest request) {
        scopeSecurity.validatePermission("sub-departments.update");

        SubDepartment subDept = subDepartmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubDepartment", "id", id));

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
        auditService.log(scopeSecurity.getCurrentUsername(), "SUB_DEPARTMENT", "UPDATE_SUB_DEPARTMENT", "SUB_DEPARTMENT", updated.getId().toString(), null, null, updated.getName(), AuditStatus.SUCCESS, null);
        return departmentMapper.toSubDeptDto(updated);
    }

    @Override
    @Transactional
    public void deleteSubDepartment(UUID id) {
        scopeSecurity.validatePermission("sub-departments.delete");

        SubDepartment subDept = subDepartmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubDepartment", "id", id));

        subDept.setActive(false);
        subDepartmentRepository.save(subDept);
        auditService.log(scopeSecurity.getCurrentUsername(), "SUB_DEPARTMENT", "DEACTIVATE_SUB_DEPARTMENT", "SUB_DEPARTMENT", id.toString(), null, subDept.getName(), null, AuditStatus.SUCCESS, null);
    }
}
