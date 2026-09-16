package com.app.AreYouReporting.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DomainEventListener {

    @KafkaListener(
            topics = "${app.kafka.topics.task-events:task-events}",
            groupId = "${spring.kafka.consumer.group-id:notifications-group}",
            autoStartup = "${app.kafka.listener.enabled:false}"
    )
    public void handleTaskEvent(DomainEvent event) {
        log.info("Received Task Domain Event: {}", event);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.request-events:request-events}",
            groupId = "${spring.kafka.consumer.group-id:notifications-group}",
            autoStartup = "${app.kafka.listener.enabled:false}"
    )
    public void handleRequestEvent(DomainEvent event) {
        log.info("Received Request Domain Event: {}", event);
    }
}
