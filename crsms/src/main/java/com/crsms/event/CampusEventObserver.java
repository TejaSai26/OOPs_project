package com.crsms.event;

import com.crsms.model.audit.AuditLog;
import com.crsms.model.notification.Notification;
import com.crsms.repository.AuditLogRepository;
import com.crsms.repository.NotificationRepository;
import com.crsms.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Observer for campus domain notices. Reservation, borrowing, and admin
 * services do not write notification or audit rows themselves.
 */
@Component
public class CampusEventObserver {
    private final NotificationRepository notifications;
    private final AuditLogRepository audits;
    private final UserRepository users;

    public CampusEventObserver(NotificationRepository notifications, AuditLogRepository audits, UserRepository users) {
        this.notifications = notifications;
        this.audits = audits;
        this.users = users;
    }

    @EventListener
    @Transactional
    public void onNotice(CampusNotice notice) {
        if (notice.notificationType() != null && notice.recipientId() != null) {
            Long entityId = parseId(notice.entityId());
            notifications.save(new Notification(
                    users.getReferenceById(notice.recipientId()),
                    notice.notificationType(),
                    notice.title(),
                    notice.message(),
                    notice.entityType(),
                    entityId));
        }
        if (notice.auditAction() != null) {
            var actor = notice.actorId() == null ? null : users.getReferenceById(notice.actorId());
            audits.save(new AuditLog(actor, notice.auditAction(), notice.entityType(), notice.entityId(),
                    notice.description() == null ? notice.auditAction() : notice.description()));
        }
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
