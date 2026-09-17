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
public class DepartmentDropdownDto {

    private UUID id;
    private String label;
    private String name;
    private String code;
    private boolean active;
}
