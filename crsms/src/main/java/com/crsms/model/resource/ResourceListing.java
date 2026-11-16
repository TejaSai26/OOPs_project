package com.crsms.model.resource;

import com.crsms.model.enums.ListingStatus;
import com.crsms.model.enums.PricingType;
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
@Table(name = "resource_listings")
public class ResourceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ResourceCategory category;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false, length = 20)
    private PricingType pricingType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_status", nullable = false, length = 20)
    private ListingStatus listingStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "borrow_count", nullable = false)
    private long borrowCount;

    protected ResourceListing() {
    }

    public ResourceListing(User owner, ResourceCategory category, String title, String description,
                           PricingType pricingType, BigDecimal rate) {
        this.owner = owner;
        this.category = category;
        this.title = title;
        this.description = description;
        this.pricingType = pricingType;
        setRate(rate);
        this.listingStatus = ListingStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.borrowCount = 0;
    }

    public void incrementBorrowCount() {
        this.borrowCount++;
    }

    public void edit(String title, String description, ResourceCategory category,
                     PricingType pricingType, BigDecimal rate) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.pricingType = pricingType;
        setRate(rate);
    }

    public void deactivate() {
        this.listingStatus = ListingStatus.INACTIVE;
    }

    private void setRate(BigDecimal rate) {
        if (rate == null || rate.signum() < 0) {
            throw new IllegalArgumentException("Rate must be nonnegative");
        }
        this.rate = rate;
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public ResourceCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public PricingType getPricingType() {
        return pricingType;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public ListingStatus getListingStatus() {
        return listingStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getBorrowCount() {
        return borrowCount;
    }
}
