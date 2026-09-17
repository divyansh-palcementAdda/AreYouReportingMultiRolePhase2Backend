package com.app.AreYouReporting.payload.response.dropdown;

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
public class UserDropdownDto {

    private UUID id;
    private String label;
    private String fullName;
    private String username;
    private String email;
    private List<String> roles;
    private List<String> departmentNames;
    private List<String> subDepartmentNames;
    private boolean active;
}
