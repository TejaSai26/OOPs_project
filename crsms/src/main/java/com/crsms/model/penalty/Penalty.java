package com.crsms.model.penalty;

import com.crsms.model.borrowing.BorrowingRecord;
import com.crsms.model.enums.PenaltyStatus;
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
@Table(name = "penalties")
public class Penalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrowing_id", nullable = false, unique = true)
    private BorrowingRecord borrowing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private User borrower;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PenaltyStatus paymentStatus;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "waived_reason", length = 500)
    private String waivedReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waived_by_id")
    private User waivedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Penalty() {
    }

    public Penalty(BorrowingRecord borrowing, BigDecimal amount, String reason) {
        if (borrowing == null) {
            throw new IllegalArgumentException("Borrowing is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Penalty amount must be positive");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Penalty reason is required");
        }
        this.borrowing = borrowing;
        this.borrower = borrowing.getBorrower();
        this.amount = amount;
        this.reason = reason.trim();
        this.paymentStatus = PenaltyStatus.UNPAID;
        this.createdAt = Instant.now();
    }

    public void markPaid() {
        if (paymentStatus != PenaltyStatus.UNPAID) {
            throw new IllegalStateException("Only unpaid penalties can be paid");
        }
        this.paymentStatus = PenaltyStatus.PAID;
    }

    public void waive(User admin, String reason) {
        if (paymentStatus != PenaltyStatus.UNPAID) {
            throw new IllegalStateException("Only unpaid penalties can be waived");
        }
        if (admin == null) {
            throw new IllegalArgumentException("An administrator must record the waiver");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Waiver reason is required");
        }
        this.paymentStatus = PenaltyStatus.WAIVED;
        this.waivedReason = reason.trim();
        this.waivedBy = admin;
    }

    public Long getId() {
        return id;
    }

    public BorrowingRecord getBorrowing() {
        return borrowing;
    }

    public User getBorrower() {
        return borrower;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PenaltyStatus getPaymentStatus() {
        return paymentStatus;
    }

    public String getReason() {
        return reason;
    }

    public String getWaivedReason() {
        return waivedReason;
    }

    public User getWaivedBy() {
        return waivedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
