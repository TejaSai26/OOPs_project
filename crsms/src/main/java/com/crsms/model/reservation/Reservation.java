package com.crsms.model.reservation;

import com.crsms.model.enums.ReservationStatus;
import com.crsms.model.resource.ResourceUnit;
import com.crsms.model.user.User;
import com.crsms.reservation.ReservationStateMachine;
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
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private User borrower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private ResourceUnit unit;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    @Column(name = "queue_position")
    private Integer queuePosition;

    protected Reservation() {
    }

    public Reservation(User borrower, ResourceUnit unit, Instant startAt, Instant endAt) {
        if (borrower == null || unit == null) {
            throw new IllegalArgumentException("Borrower and unit are required");
        }
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        this.borrower = borrower;
        this.unit = unit;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = ReservationStatus.REQUESTED;
        this.createdAt = Instant.now();
    }

    public void approve() {
        move(ReservationStatus.APPROVED);
    }

    public void reject(String note) {
        move(ReservationStatus.REJECTED);
        this.decisionNote = blankToNull(note);
    }

    public void cancel() {
        move(ReservationStatus.CANCELLED);
    }

    public void expire() {
        move(ReservationStatus.EXPIRED);
    }

    public void placeOnWaitlist(int position) {
        if (position < 1) {
            throw new IllegalArgumentException("Queue position must be positive");
        }
        move(ReservationStatus.WAITLISTED);
        this.queuePosition = position;
    }

    public void promoteFromWaitlist() {
        move(ReservationStatus.REQUESTED);
        this.queuePosition = null;
    }

    public void markPickedUp() {
        move(ReservationStatus.ACTIVE);
    }

    public void markReturned() {
        move(ReservationStatus.RETURNED);
    }

    private void move(ReservationStatus next) {
        ReservationStateMachine.move(status, next);
        this.status = next;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public Long getId() {
        return id;
    }

    public User getBorrower() {
        return borrower;
    }

    public ResourceUnit getUnit() {
        return unit;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public Integer getQueuePosition() {
        return queuePosition;
    }
}
