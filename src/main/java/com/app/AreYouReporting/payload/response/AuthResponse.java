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
public class AuthResponse {

    private String tokenType;
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UserSummaryDto user;
    private UUID activeRoleId;
    private String activeRoleName;
    private UUID activeDepartmentId;
    private String activeDepartmentName;
    private UUID activeSubDepartmentId;
    private String activeSubDepartmentName;
    private List<UserRoleAssignmentDto> availableAssignments;
}

