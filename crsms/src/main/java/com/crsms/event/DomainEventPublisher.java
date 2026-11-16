package com.crsms.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class DomainEventPublisher {
    private final ApplicationEventPublisher events;

    public DomainEventPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    public void publish(Long actorId, Long recipientId, String notificationType, String title, String message,
                        String auditAction, String entityType, Object entityId, String description) {
        events.publishEvent(new CampusNotice(
                actorId,
                recipientId,
                notificationType,
                title,
                message,
                auditAction,
                entityType,
                entityId == null ? null : entityId.toString(),
                description));
    }
}
