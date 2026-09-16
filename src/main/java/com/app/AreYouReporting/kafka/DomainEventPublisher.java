package com.app.AreYouReporting.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class DomainEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Value("${app.kafka.topics.task-events:task-events}")
    private String taskEventsTopic;

    @Value("${app.kafka.topics.request-events:request-events}")
    private String requestEventsTopic;

    @Value("${app.kafka.topics.audit-events:audit-events}")
    private String auditEventsTopic;

    public DomainEventPublisher(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTaskEvent(String eventType, String taskId, String actor, String role, Map<String, Object> payload) {
        DomainEvent event = DomainEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .entityType("TASK")
                .entityId(taskId)
                .actorUsername(actor)
                .actorRole(role)
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        send(taskEventsTopic, taskId, event);
    }

    public void publishRequestEvent(String eventType, String requestId, String actor, String role, Map<String, Object> payload) {
        DomainEvent event = DomainEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .entityType("TASK_REQUEST")
                .entityId(requestId)
                .actorUsername(actor)
                .actorRole(role)
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        send(requestEventsTopic, requestId, event);
    }

    private void send(String topic, String key, DomainEvent event) {
        log.info("Domain Event [{}]: entity={} id={} actor={}", event.getEventType(), event.getEntityType(), event.getEntityId(), event.getActorUsername());
        if (kafkaEnabled && kafkaTemplate != null) {
            try {
                kafkaTemplate.send(topic, key, event);
            } catch (Exception ex) {
                log.warn("Failed to publish domain event to Kafka topic {}: {}", topic, ex.getMessage());
            }
        }
    }
}
