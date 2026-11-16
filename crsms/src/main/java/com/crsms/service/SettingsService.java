package com.crsms.service;

import com.crsms.dto.ApiDtos.SettingsResponse;
import com.crsms.dto.ApiDtos.SettingsUpdate;
import com.crsms.event.DomainEventPublisher;
import com.crsms.exception.ApiException;
import com.crsms.model.enums.UserRole;
import com.crsms.model.settings.SystemSettings;
import com.crsms.model.user.User;
import com.crsms.repository.SystemSettingsRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {
    public static final long SETTINGS_ID = 1L;

    private final SystemSettingsRepository settings;
    private final DomainEventPublisher events;

    public SettingsService(SystemSettingsRepository settings, DomainEventPublisher events) {
        this.settings = settings;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public SystemSettings current() {
        return settings.findById(SETTINGS_ID)
                .orElseThrow(() -> new ApiException(500, "System settings are not initialized"));
    }

    @Transactional(readOnly = true)
    public SettingsResponse view() {
        SystemSettings row = current();
        return new SettingsResponse(row.getLateFeeMultiplier(), row.getReminderHours());
    }

    @Transactional(readOnly = true)
    public BigDecimal multiplier() {
        return current().getLateFeeMultiplier();
    }

    @Transactional(readOnly = true)
    public int reminderHours() {
        return current().getReminderHours();
    }

    @Transactional
    public SettingsResponse update(User admin, SettingsUpdate update) {
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ApiException(403, "Only an administrator can change system settings");
        }
        SystemSettings row = current();
        row.update(update.lateFeeMultiplier(), update.reminderHours());
        events.publish(admin.getId(), null, null, null, null,
                "SETTINGS_UPDATED", "SystemSettings", row.getId(),
                "Late fee multiplier set to " + update.lateFeeMultiplier()
                        + " and reminder window to " + update.reminderHours() + " hours");
        return new SettingsResponse(row.getLateFeeMultiplier(), row.getReminderHours());
    }
}
