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
public class DepartmentDto {

    private UUID id;
    private String name;
    private String code;
    private String description;
    private boolean isActive;
    private List<SubDepartmentDto> subDepartments;
}
