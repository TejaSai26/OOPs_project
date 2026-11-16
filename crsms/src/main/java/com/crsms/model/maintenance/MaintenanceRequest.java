package com.crsms.model.maintenance;

import com.crsms.model.enums.MaintenanceStatus;
import com.crsms.model.resource.ResourceUnit;
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
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "maintenance_requests")
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private ResourceUnit unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaintenanceStatus status;

    @Column(name = "admin_note", length = 2000)
    private String adminNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "repair_cost", precision = 12, scale = 2)
    private BigDecimal repairCost;

    @Column(name = "photo_ref", length = 500)
    private String photoRef;

    protected MaintenanceRequest() {
    }

    public MaintenanceRequest(ResourceUnit unit, User reporter, String description, String photoRef) {
        this.unit = unit;
        this.reporter = reporter;
        this.description = description;
        this.photoRef = photoRef == null || photoRef.isBlank() ? null : photoRef.trim();
        this.status = MaintenanceStatus.OPEN;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(MaintenanceStatus status, String adminNote) {
        if (status != null) {
            this.status = status;
        }
        if (adminNote != null) {
            this.adminNote = adminNote;
        }
        this.updatedAt = Instant.now();
    }

    public void assign(User assignee, BigDecimal repairCost, String photoRef) {
        if (repairCost != null && repairCost.signum() < 0) {
            throw new IllegalArgumentException("Repair cost must be nonnegative");
        }
        if (assignee != null) {
            this.assignee = assignee;
        }
        if (repairCost != null) {
            this.repairCost = repairCost;
        }
        if (photoRef != null && !photoRef.isBlank()) {
            this.photoRef = photoRef.trim();
        }
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ResourceUnit getUnit() {
        return unit;
    }

    public User getReporter() {
        return reporter;
    }

    public String getDescription() {
        return description;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public User getAssignee() {
        return assignee;
    }

    public BigDecimal getRepairCost() {
        return repairCost;
    }

    public String getPhotoRef() {
        return photoRef;
    }
}
