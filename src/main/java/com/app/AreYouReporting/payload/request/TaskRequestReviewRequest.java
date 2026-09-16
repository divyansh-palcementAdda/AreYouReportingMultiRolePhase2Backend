package com.app.AreYouReporting.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestReviewRequest {

    @NotNull(message = "Approval status (approve: true/false) is required")
    private Boolean approve;

    private String remarks;
}
