package com.app.AreYouReporting.service.interfaces;

import com.app.AreYouReporting.Entities.TaskPriority;
import com.app.AreYouReporting.Entities.TaskStatus;
import com.app.AreYouReporting.payload.response.PageResponse;
import com.app.AreYouReporting.payload.response.dropdown.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DropdownService {

    PageResponse<DepartmentDropdownDto> getDepartments(String search, Boolean activeOnly, Pageable pageable);

    PageResponse<SubDepartmentDropdownDto> getSubDepartments(UUID departmentId, String search, Boolean activeOnly, Pageable pageable);

    PageResponse<UserDropdownDto> getUsers(UUID departmentId, UUID subDepartmentId, String role, String search, Boolean activeOnly, Pageable pageable);

    PageResponse<UserDropdownDto> getEligibleAssignees(UUID departmentId, UUID subDepartmentId, String search, Pageable pageable);

    PageResponse<UserDropdownDto> getEligibleReportingManagers(UUID departmentId, UUID subDepartmentId, String search, Pageable pageable);

    PageResponse<TaskTemplateDropdownDto> getTaskTemplates(UUID departmentId, UUID subDepartmentId, String search, Boolean activeOnly, Pageable pageable);

    PageResponse<TaskDropdownDto> getTasks(UUID departmentId, UUID subDepartmentId, TaskStatus status, TaskPriority priority, Boolean isSelfTask, String search, Pageable pageable);

    PageResponse<RoleDropdownDto> getRoles(String search, Pageable pageable);

    PageResponse<PermissionDropdownDto> getPermissions(String resource, String search, Pageable pageable);

    List<StaticOptionDto> getTaskStatuses();

    List<StaticOptionDto> getRequestStatuses();

    List<StaticOptionDto> getTaskPriorities();

    List<StaticOptionDto> getProofRequirementTypes();

    List<StaticOptionDto> getDataScopeTypes();
}
