package com.app.AreYouReporting.payload.response.dropdown;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaticOptionDto {

    private String value;
    private String label;
    private String description;
}
