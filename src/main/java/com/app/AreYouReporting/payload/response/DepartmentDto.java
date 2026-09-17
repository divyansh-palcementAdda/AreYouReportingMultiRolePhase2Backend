package com.app.AreYouReporting.payload.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("isActive")
    @JsonAlias({"active", "isActive", "is_active"})
    private boolean isActive;

    private List<SubDepartmentDto> subDepartments;
}
