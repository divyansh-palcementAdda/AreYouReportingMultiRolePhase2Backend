package com.app.AreYouReporting.payload.request;

import com.app.AreYouReporting.Entities.TaskRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestSubmitRequest {

    @NotNull(message = "Request type is required")
    private TaskRequestType requestType;

    @NotBlank(message = "Reason is required")
    private String reason;

    private Instant requestedDueDate;

    private List<UUID> proofIds;
}
