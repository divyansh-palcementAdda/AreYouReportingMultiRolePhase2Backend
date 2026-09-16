package com.app.AreYouReporting.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto {

    private UUID id;
    private String resource;
    private String action;
    private String authority;
    private String description;
    private boolean isSystemPermission;
}
