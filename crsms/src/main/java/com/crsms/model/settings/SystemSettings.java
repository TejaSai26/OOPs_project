package com.crsms.model.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "system_settings")
public class SystemSettings {

    @Id
    private Long id;

    @Column(name = "late_fee_multiplier", nullable = false, precision = 8, scale = 2)
    private BigDecimal lateFeeMultiplier;

    @Column(name = "reminder_hours", nullable = false)
    private int reminderHours;

    protected SystemSettings() {
    }

    public void update(BigDecimal lateFeeMultiplier, int reminderHours) {
        if (lateFeeMultiplier == null || lateFeeMultiplier.signum() < 0
                || lateFeeMultiplier.compareTo(new BigDecimal("10")) > 0) {
            throw new IllegalArgumentException("Late fee multiplier must be between 0 and 10");
        }
        if (reminderHours < 1 || reminderHours > 168) {
            throw new IllegalArgumentException("Reminder window must be between 1 and 168 hours");
        }
        this.lateFeeMultiplier = lateFeeMultiplier;
        this.reminderHours = reminderHours;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getLateFeeMultiplier() {
        return lateFeeMultiplier;
    }

    public int getReminderHours() {
        return reminderHours;
    }
}
