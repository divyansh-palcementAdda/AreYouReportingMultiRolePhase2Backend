package com.app.AreYouReporting.payload.response;

import com.app.AreYouReporting.Entities.AuditStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {

    private UUID id;
    private String actorUsername;
    private String actorRole;
    private String action;
    private String entityType;
    private String entityId;
    private String correlationId;
    private String detailsBefore;
    private String detailsAfter;
    private AuditStatus status;
    private String ipAddress;
    private Instant createdAt;
}
