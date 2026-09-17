package com.app.AreYouReporting.payload.response.dropdown;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectOptionDto {

    private Object id;
    private String label;
    private String code;
    @Builder.Default
    private boolean disabled = false;
    private Map<String, Object> metadata;
}
