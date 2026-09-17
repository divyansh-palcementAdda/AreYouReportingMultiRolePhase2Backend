package com.app.AreYouReporting.mapper;

import com.app.AreYouReporting.Entities.Department;
import com.app.AreYouReporting.Entities.SubDepartment;
import com.app.AreYouReporting.payload.response.DepartmentDto;
import com.app.AreYouReporting.payload.response.SubDepartmentDto;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DepartmentMapper {

    public DepartmentDto toDto(Department dept) {
        if (dept == null) return null;
        List<SubDepartmentDto> activeSubDepts = Collections.emptyList();
        if (dept.getSubDepartments() != null) {
            activeSubDepts = dept.getSubDepartments().stream()
                    .filter(SubDepartment::isActive)
                    .map(this::toSubDeptDto)
                    .collect(Collectors.toList());
        }
        return DepartmentDto.builder()
                .id(dept.getId())
                .name(dept.getName())
                .code(dept.getCode())
                .description(dept.getDescription())
                .isActive(dept.isActive())
                .subDepartments(activeSubDepts)
                .build();
    }

    public List<DepartmentDto> toDtoList(Collection<Department> departments) {
        if (departments == null) return Collections.emptyList();
        return departments.stream().map(this::toDto).collect(Collectors.toList());
    }

    public SubDepartmentDto toSubDeptDto(SubDepartment subDept) {
        if (subDept == null) return null;
        return SubDepartmentDto.builder()
                .id(subDept.getId())
                .departmentId(subDept.getDepartment() != null ? subDept.getDepartment().getId() : null)
                .departmentName(subDept.getDepartment() != null ? subDept.getDepartment().getName() : null)
                .name(subDept.getName())
                .code(subDept.getCode())
                .description(subDept.getDescription())
                .isActive(subDept.isActive())
                .build();
    }

    public List<SubDepartmentDto> toSubDeptDtoList(Collection<SubDepartment> subDepartments) {
        if (subDepartments == null) return Collections.emptyList();
        return subDepartments.stream().map(this::toSubDeptDto).collect(Collectors.toList());
    }
}
