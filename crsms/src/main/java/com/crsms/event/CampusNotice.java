package com.crsms.event;

/**
 * Immutable domain notice. Services publish it; observers decide how to deliver
 * notifications and how to record the audit trail.
 */
public record CampusNotice(
        Long actorId,
        Long recipientId,
        String notificationType,
        String title,
        String message,
        String auditAction,
        String entityType,
        String entityId,
        String description
) {
}
