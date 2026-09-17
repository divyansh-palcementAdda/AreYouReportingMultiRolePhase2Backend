package com.app.AreYouReporting.payload.response.dropdown;

import com.app.AreYouReporting.Entities.DataScopeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDropdownDto {

    private UUID id;
    private String label;
    private String name;
    private String description;
    private DataScopeType defaultDataScope;
    private boolean isSystemRole;
}
