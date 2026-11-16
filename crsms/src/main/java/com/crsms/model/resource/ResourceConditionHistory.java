package com.crsms.model.resource;

import com.crsms.model.enums.ConditionPhase;
import com.crsms.model.enums.ResourceCondition;
import com.crsms.model.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "resource_condition_history")
public class ResourceConditionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private ResourceUnit unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConditionPhase phase;

    @Column(length = 1000)
    private String note;

    @Column(name = "photo_ref", length = 500)
    private String photoRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_id")
    private User recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected ResourceConditionHistory() {
    }

    public ResourceConditionHistory(ResourceUnit unit, ResourceCondition condition, ConditionPhase phase,
                                    String note, String photoRef, User recordedBy) {
        if (unit == null || condition == null || phase == null) {
            throw new IllegalArgumentException("Unit, condition, and phase are required");
        }
        this.unit = unit;
        this.condition = condition;
        this.phase = phase;
        this.note = note == null || note.isBlank() ? null : note.trim();
        this.photoRef = photoRef == null || photoRef.isBlank() ? null : photoRef.trim();
        this.recordedBy = recordedBy;
        this.recordedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ResourceUnit getUnit() {
        return unit;
    }

    public ResourceCondition getCondition() {
        return condition;
    }

    public ConditionPhase getPhase() {
        return phase;
    }

    public String getNote() {
        return note;
    }

    public String getPhotoRef() {
        return photoRef;
    }

    public User getRecordedBy() {
        return recordedBy;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
