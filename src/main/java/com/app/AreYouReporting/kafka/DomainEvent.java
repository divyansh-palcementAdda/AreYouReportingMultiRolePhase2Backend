package com.app.AreYouReporting.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent implements Serializable {

    private String eventId;
    private String eventType;
    private String entityType;
    private String entityId;
    private String actorUsername;
    private String actorRole;
    private Map<String, Object> payload;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
