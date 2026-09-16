package com.app.AreYouReporting.service.interfaces;

import com.app.AreYouReporting.payload.request.DepartmentRequest;
import com.app.AreYouReporting.payload.request.SubDepartmentRequest;
import com.app.AreYouReporting.payload.response.DepartmentDto;
import com.app.AreYouReporting.payload.response.SubDepartmentDto;

import java.util.List;
import java.util.UUID;

public interface DepartmentService {

    List<DepartmentDto> getAllDepartments(boolean activeOnly);

    DepartmentDto getDepartmentById(UUID id);

    DepartmentDto createDepartment(DepartmentRequest request);

    DepartmentDto updateDepartment(UUID id, DepartmentRequest request);

    void deleteDepartment(UUID id);

    List<SubDepartmentDto> getSubDepartmentsByDeptId(UUID deptId, boolean activeOnly);

    SubDepartmentDto getSubDepartmentById(UUID id);

    SubDepartmentDto createSubDepartment(UUID deptId, SubDepartmentRequest request);

    SubDepartmentDto updateSubDepartment(UUID id, SubDepartmentRequest request);

    void deleteSubDepartment(UUID id);
}
