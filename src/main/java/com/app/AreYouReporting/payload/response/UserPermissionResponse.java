package com.app.AreYouReporting.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionResponse {

    private UUID userId;
    private String username;
    private String fullName;
    private UUID activeRoleId;
    private String activeRoleName;
    private List<UserRoleAssignmentDto> assignments;
    private List<ResourceGrantsDto> grants;
    private List<String> permissions;
}
