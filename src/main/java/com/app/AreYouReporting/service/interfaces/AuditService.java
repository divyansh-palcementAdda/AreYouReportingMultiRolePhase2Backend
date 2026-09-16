package com.app.AreYouReporting.service.interfaces;

import com.app.AreYouReporting.Entities.AuditStatus;
import com.app.AreYouReporting.payload.response.AuditLogDto;
import com.app.AreYouReporting.payload.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface AuditService {

    void log(String actorUsername, String actorRole, String action, String entityType, String entityId,
             String correlationId, Object detailsBefore, Object detailsAfter, AuditStatus status, String ipAddress);

    PageResponse<AuditLogDto> getAuditLogs(String actorUsername, String action, String entityType,
                                          String entityId, Instant fromDate, Instant toDate, Pageable pageable);

    AuditLogDto getAuditLogById(UUID id);
}
