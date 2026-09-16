package com.app.AreYouReporting.controller;

import com.app.AreYouReporting.payload.response.ApiResponse;
import com.app.AreYouReporting.payload.response.AuditLogDto;
import com.app.AreYouReporting.payload.response.PageResponse;
import com.app.AreYouReporting.service.interfaces.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
@Tag(name = "Immutable Audit Logs", description = "Endpoints for Super Administrators to query and inspect system-wide audit records")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("@scopeSecurity.hasPermission('audits.read')")
    @Operation(summary = "Search and filter immutable audit logs (actor, action, entity type, entity ID, date range)")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> getAuditLogs(
            @RequestParam(value = "actorUsername", required = false) String actorUsername,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "entityType", required = false) String entityType,
            @RequestParam(value = "entityId", required = false) String entityId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<AuditLogDto> logs = auditService.getAuditLogs(actorUsername, action, entityType, entityId, fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@scopeSecurity.hasPermission('audits.read')")
    @Operation(summary = "Get detailed audit log by ID")
    public ResponseEntity<ApiResponse<AuditLogDto>> getAuditLogById(@PathVariable("id") UUID id) {
        AuditLogDto log = auditService.getAuditLogById(id);
        return ResponseEntity.ok(ApiResponse.success(log));
    }
}
