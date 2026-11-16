package com.crsms.model.resource;

import com.crsms.model.enums.ResourceCondition;
import com.crsms.model.enums.UnitStatus;
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

@Entity
@Table(name = "resource_units")
public class ResourceUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private ResourceListing listing;

    @Column(name = "asset_code", nullable = false, unique = true, length = 40)
    private String assetCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UnitStatus status;

    @Column(name = "location_note", length = 200)
    private String locationNote;

    protected ResourceUnit() {
    }

    public ResourceUnit(ResourceListing listing, String assetCode, ResourceCondition condition, String locationNote) {
        this.listing = listing;
        this.assetCode = assetCode;
        this.condition = condition == null ? ResourceCondition.GOOD : condition;
        this.status = UnitStatus.AVAILABLE;
        this.locationNote = locationNote;
    }

    public void changeStatus(UnitStatus next) {
        if (next == null) {
            throw new IllegalArgumentException("Status is required");
        }
        this.status = next;
    }

    public void changeCondition(ResourceCondition condition) {
        if (condition != null) {
            this.condition = condition;
        }
    }

    public Long getId() {
        return id;
    }

    public ResourceListing getListing() {
        return listing;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public ResourceCondition getCondition() {
        return condition;
    }

    public UnitStatus getStatus() {
        return status;
    }

    public String getLocationNote() {
        return locationNote;
    }
}
