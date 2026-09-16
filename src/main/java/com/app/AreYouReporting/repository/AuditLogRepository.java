package com.app.AreYouReporting.repository;

import com.app.AreYouReporting.Entities.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByActorUsername(String actorUsername);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);

    List<AuditLog> findByCreatedAtBetween(Instant fromDate, Instant toDate);
}
