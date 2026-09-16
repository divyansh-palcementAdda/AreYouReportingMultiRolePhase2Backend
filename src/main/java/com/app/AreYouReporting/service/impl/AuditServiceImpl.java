package com.app.AreYouReporting.service.impl;

import com.app.AreYouReporting.Entities.AuditLog;
import com.app.AreYouReporting.Entities.AuditStatus;
import com.app.AreYouReporting.exceptions.ResourceNotFoundException;
import com.app.AreYouReporting.mapper.AuditLogMapper;
import com.app.AreYouReporting.payload.response.AuditLogDto;
import com.app.AreYouReporting.payload.response.PageResponse;
import com.app.AreYouReporting.repository.AuditLogRepository;
import com.app.AreYouReporting.service.interfaces.AuditService;
import com.app.AreYouReporting.specification.AuditLogSpecification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String actorUsername, String actorRole, String action, String entityType, String entityId,
                    String correlationId, Object detailsBefore, Object detailsAfter, AuditStatus status, String ipAddress) {
        try {
            String beforeJson = detailsBefore != null ? (detailsBefore instanceof String s ? s : objectMapper.writeValueAsString(detailsBefore)) : null;
            String afterJson = detailsAfter != null ? (detailsAfter instanceof String s ? s : objectMapper.writeValueAsString(detailsAfter)) : null;

            AuditLog logEntry = AuditLog.builder()
                    .actorUsername(actorUsername != null ? actorUsername : "ANONYMOUS")
                    .actorRole(actorRole)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .correlationId(correlationId)
                    .detailsBefore(beforeJson)
                    .detailsAfter(afterJson)
                    .status(status != null ? status : AuditStatus.SUCCESS)
                    .ipAddress(ipAddress)
                    .createdAt(Instant.now())
                    .build();

            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.error("Failed to write audit log entry: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogDto> getAuditLogs(String actorUsername, String action, String entityType,
                                                 String entityId, Instant fromDate, Instant toDate, Pageable pageable) {
        Specification<AuditLog> spec = AuditLogSpecification.filter(actorUsername, action, entityType, entityId, fromDate, toDate);
        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);
        return PageResponse.of(page, auditLogMapper.toDtoList(page.getContent()));
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogDto getAuditLogById(UUID id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", "id", id));
        return auditLogMapper.toDto(log);
    }
}
