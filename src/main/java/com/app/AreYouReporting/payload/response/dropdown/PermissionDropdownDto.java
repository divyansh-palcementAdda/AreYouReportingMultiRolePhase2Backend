package com.app.AreYouReporting.payload.response.dropdown;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDropdownDto {

    private UUID id;
    private String label;
    private String authority;
    private String resource;
    private String action;
    private String description;
}
