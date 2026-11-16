package com.crsms.service;

import com.crsms.dto.ApiDtos.NotificationCount;
import com.crsms.dto.ApiDtos.NotificationResponse;
import com.crsms.exception.ApiException;
import com.crsms.model.notification.Notification;
import com.crsms.model.user.User;
import com.crsms.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> history(User user) {
        return notifications.findTop100ByRecipientIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public NotificationCount unread(User user) {
        return new NotificationCount(notifications.countByRecipientIdAndReadFlagFalse(user.getId()));
    }

    @Transactional
    public NotificationResponse markRead(User user, Long id) {
        Notification notification = notifications.findById(id)
                .orElseThrow(() -> new ApiException(404, "Notification not found"));
        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new ApiException(403, "You cannot change another person's notifications");
        }
        notification.markRead();
        return toResponse(notification);
    }

    @Transactional
    public NotificationCount markAll(User user) {
        notifications.markAllRead(user.getId());
        return unread(user);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isReadFlag(),
                notification.getCreatedAt(),
                notification.getEntityType(),
                notification.getEntityId());
    }
}
