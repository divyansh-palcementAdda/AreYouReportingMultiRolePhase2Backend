package com.app.AreYouReporting.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubDepartmentRequest {

    @NotBlank(message = "SubDepartment name is required")
    private String name;

    @NotBlank(message = "SubDepartment code is required")
    private String code;

    private String description;
    private Boolean isActive;
}
