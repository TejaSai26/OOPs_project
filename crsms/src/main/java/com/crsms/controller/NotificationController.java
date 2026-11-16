package com.crsms.controller;

import com.crsms.dto.ApiDtos.NotificationCount;
import com.crsms.dto.ApiDtos.NotificationResponse;
import com.crsms.service.CurrentUserService;
import com.crsms.service.NotificationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;
    private final CurrentUserService current;

    public NotificationController(NotificationService notifications, CurrentUserService current) {
        this.notifications = notifications;
        this.current = current;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        return notifications.history(current.require());
    }

    @GetMapping("/count")
    public NotificationCount count() {
        return notifications.unread(current.require());
    }

    @PostMapping("/{id}/read")
    public NotificationResponse read(@PathVariable Long id) {
        return notifications.markRead(current.require(), id);
    }

    @PostMapping("/read-all")
    public NotificationCount readAll() {
        return notifications.markAll(current.require());
    }
}
