package com.app.AreYouReporting.service.interfaces;

import com.app.AreYouReporting.payload.request.DataScopeOverrideRequest;
import com.app.AreYouReporting.payload.request.UserCreateRequest;
import com.app.AreYouReporting.payload.request.UserRoleAssignmentRequest;
import com.app.AreYouReporting.payload.request.UserUpdateRequest;
import com.app.AreYouReporting.payload.response.PageResponse;
import com.app.AreYouReporting.payload.response.UserDto;
import com.app.AreYouReporting.payload.response.UserSummaryDto;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserService {

    PageResponse<UserDto> getAllUsers(Pageable pageable);

    UserDto getUserById(UUID id);

    UserDto createUser(UserCreateRequest request);

    UserDto updateUser(UUID id, UserUpdateRequest request);

    UserDto assignRoleToUser(UUID userId, UserRoleAssignmentRequest request);

    void removeRoleAssignment(UUID assignmentId);

    UserDto overrideUserDataScope(UUID assignmentId, DataScopeOverrideRequest request);

    void deactivateUser(UUID id);

    List<UserSummaryDto> getUsersByDepartment(UUID departmentId);

    List<UserSummaryDto> getUsersBySubDepartment(UUID subDepartmentId);
}
