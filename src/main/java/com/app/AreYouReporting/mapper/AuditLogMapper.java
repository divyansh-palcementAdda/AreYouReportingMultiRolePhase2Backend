package com.app.AreYouReporting.mapper;

import com.app.AreYouReporting.Entities.AuditLog;
import com.app.AreYouReporting.payload.response.AuditLogDto;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuditLogMapper {

    public AuditLogDto toDto(AuditLog audit) {
        if (audit == null) return null;
        return AuditLogDto.builder()
                .id(audit.getId())
                .actorUsername(audit.getActorUsername())
                .actorRole(audit.getActorRole())
                .action(audit.getAction())
                .entityType(audit.getEntityType())
                .entityId(audit.getEntityId())
                .correlationId(audit.getCorrelationId())
                .detailsBefore(audit.getDetailsBefore())
                .detailsAfter(audit.getDetailsAfter())
                .status(audit.getStatus())
                .ipAddress(audit.getIpAddress())
                .createdAt(audit.getCreatedAt())
                .build();
    }

    public List<AuditLogDto> toDtoList(Collection<AuditLog> auditLogs) {
        if (auditLogs == null) return Collections.emptyList();
        return auditLogs.stream().map(this::toDto).collect(Collectors.toList());
    }
}
