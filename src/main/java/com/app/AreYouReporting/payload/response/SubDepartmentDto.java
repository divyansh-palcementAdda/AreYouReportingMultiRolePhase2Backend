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
public class SubDepartmentDto {

    private UUID id;
    private UUID departmentId;
    private String departmentName;
    private String name;
    private String code;
    private String description;
    private boolean isActive;
}
