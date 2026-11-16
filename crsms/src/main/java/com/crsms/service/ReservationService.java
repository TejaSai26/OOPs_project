package com.crsms.service;

import com.crsms.dto.ApiDtos.AvailabilityResponse;
import com.crsms.dto.ApiDtos.ReservationRequest;
import com.crsms.dto.ApiDtos.ReservationResponse;
import com.crsms.event.DomainEventPublisher;
import com.crsms.exception.ApiException;
import com.crsms.model.enums.BorrowingStatus;
import com.crsms.model.enums.ListingStatus;
import com.crsms.model.enums.NotificationType;
import com.crsms.model.enums.ReservationStatus;
import com.crsms.model.enums.UnitStatus;
import com.crsms.model.reservation.Reservation;
import com.crsms.model.resource.ResourceUnit;
import com.crsms.model.user.User;
import com.crsms.repository.BorrowingRecordRepository;
import com.crsms.repository.ReservationRepository;
import com.crsms.repository.ResourceUnitRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
    static final EnumSet<ReservationStatus> BLOCKING = EnumSet.of(
            ReservationStatus.REQUESTED, ReservationStatus.APPROVED, ReservationStatus.ACTIVE);
    private static final EnumSet<BorrowingStatus> OPEN_BORROW = EnumSet.of(
            BorrowingStatus.ACTIVE, BorrowingStatus.OVERDUE);
    private static final EnumSet<UnitStatus> STICKY = EnumSet.of(
            UnitStatus.MAINTENANCE_REQUIRED, UnitStatus.UNDER_REPAIR, UnitStatus.INACTIVE, UnitStatus.LOST);

    private final ReservationRepository reservations;
    private final ResourceUnitRepository units;
    private final BorrowingRecordRepository borrowings;
    private final DomainEventPublisher events;
    private final EntityManager entityManager;

    public ReservationService(ReservationRepository reservations, ResourceUnitRepository units,
                              BorrowingRecordRepository borrowings, DomainEventPublisher events,
                              EntityManager entityManager) {
        this.reservations = reservations;
        this.units = units;
        this.borrowings = borrowings;
        this.events = events;
        this.entityManager = entityManager;
    }

    @Transactional
    public ReservationResponse request(User borrower, ReservationRequest request) {
        // Lock the physical unit row before the overlap read so two transactions
        // cannot both observe "free" and insert overlapping reservations.
        ResourceUnit unit = units.findByIdForUpdate(request.unitId())
                .orElseThrow(() -> new ApiException(404, "Resource unit not found"));
        if (unit.getListing().getOwner().getId().equals(borrower.getId())) {
            throw new ApiException(400, "You cannot reserve a unit you own");
        }
        if (unit.getListing().getListingStatus() != ListingStatus.ACTIVE) {
            throw new ApiException(400, "This listing is not active");
        }
        if (!unit.getStatus().isReservable() || unit.getCondition().blocksReservation()) {
            throw new ApiException(400, "This unit is unavailable (" + unit.getStatus() + " / " + unit.getCondition() + ")");
        }
        if (request.startAt() == null || request.endAt() == null || !request.endAt().isAfter(request.startAt())) {
            throw new ApiException(400, "End time must be after start time");
        }
        if (request.startAt().isBefore(Instant.now().minusSeconds(120))) {
            throw new ApiException(400, "Reservations cannot start in the past");
        }
        long overlaps = reservations.countOverlapping(unit.getId(), request.startAt(), request.endAt(), BLOCKING);
        boolean joinWaitlist = Boolean.TRUE.equals(request.joinWaitlist());
        if (overlaps > 0 && !joinWaitlist) {
            throw new ApiException(409, "That unit is already reserved for an overlapping period");
        }
        Reservation reservation = new Reservation(borrower, unit, request.startAt(), request.endAt());
        if (overlaps > 0) {
            int position = (int) reservations.countByUnitIdAndStatus(unit.getId(), ReservationStatus.WAITLISTED) + 1;
            reservation.placeOnWaitlist(position);
        }
        reservation = reservations.save(reservation);
        User owner = unit.getListing().getOwner();
        String title = unit.getListing().getTitle();
        if (reservation.getStatus() == ReservationStatus.WAITLISTED) {
            events.publish(borrower.getId(), borrower.getId(), NotificationType.RESERVATION_SUBMITTED.name(),
                    "Added to the waiting list",
                    "You are #" + reservation.getQueuePosition() + " for " + title + " (" + unit.getAssetCode() + ").",
                    "RESERVATION_WAITLISTED", "Reservation", reservation.getId(),
                    borrower.getFullName() + " joined the waiting list for " + unit.getAssetCode());
            events.publish(borrower.getId(), owner.getId(), NotificationType.RESERVATION_SUBMITTED.name(),
                    "Waiting list joined",
                    borrower.getFullName() + " is waiting for " + title + ".",
                    null, "Reservation", reservation.getId(), null);
        } else {
            events.publish(borrower.getId(), owner.getId(), NotificationType.RESERVATION_SUBMITTED.name(),
                    "Reservation request",
                    borrower.getFullName() + " requested " + title + " (" + unit.getAssetCode() + ").",
                    "RESERVATION_REQUESTED", "Reservation", reservation.getId(),
                    "Reservation " + reservation.getId() + " requested by " + borrower.getEmail());
            events.publish(borrower.getId(), borrower.getId(), NotificationType.RESERVATION_SUBMITTED.name(),
                    "Request submitted",
                    "Your request for " + title + " is waiting for the owner.",
                    null, "Reservation", reservation.getId(), null);
        }
        return toResponse(reservation);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(Long unitId, Instant startAt, Instant endAt) {
        ResourceUnit unit = units.findById(unitId).orElseThrow(() -> new ApiException(404, "Resource unit not found"));
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new ApiException(400, "End time must be after start time");
        }
        long overlaps = reservations.countOverlapping(unitId, startAt, endAt, BLOCKING);
        boolean open = unit.getStatus().isReservable() && !unit.getCondition().blocksReservation() && overlaps == 0;
        return new AvailabilityResponse(open, overlaps, unit.getStatus().name(), unit.getCondition().name());
    }

    @Transactional
    public ReservationResponse approve(User actor, Long id) {
        Reservation reservation = load(id);
        assertOwner(actor, reservation);
        reservation.approve();
        refreshUnit(reservation.getUnit());
        events.publish(actor.getId(), reservation.getBorrower().getId(), NotificationType.RESERVATION_APPROVED.name(),
                "Reservation approved",
                actor.getFullName() + " approved " + reservation.getUnit().getListing().getTitle() + ".",
                "RESERVATION_APPROVED", "Reservation", reservation.getId(),
                "Approved reservation " + reservation.getId());
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse reject(User actor, Long id, String note) {
        Reservation reservation = load(id);
        assertOwner(actor, reservation);
        reservation.reject(note);
        refreshUnit(reservation.getUnit());
        promoteWaitlist(reservation.getUnit());
        String reason = note == null || note.isBlank() ? "No reason was given." : note.trim();
        events.publish(actor.getId(), reservation.getBorrower().getId(), NotificationType.RESERVATION_REJECTED.name(),
                "Reservation rejected",
                "Your request for " + reservation.getUnit().getListing().getTitle() + " was rejected. " + reason,
                "RESERVATION_REJECTED", "Reservation", reservation.getId(),
                "Rejected reservation " + reservation.getId());
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse cancel(User actor, Long id) {
        Reservation reservation = load(id);
        boolean borrower = reservation.getBorrower().getId().equals(actor.getId());
        boolean owner = reservation.getUnit().getListing().getOwner().getId().equals(actor.getId());
        if (!borrower && !owner && actor.getRole() != com.crsms.model.enums.UserRole.ADMIN) {
            throw new ApiException(403, "You cannot cancel this reservation");
        }
        User other = borrower ? reservation.getUnit().getListing().getOwner() : reservation.getBorrower();
        reservation.cancel();
        refreshUnit(reservation.getUnit());
        promoteWaitlist(reservation.getUnit());
        events.publish(actor.getId(), other.getId(), NotificationType.RESERVATION_CANCELLED.name(),
                "Reservation cancelled",
                actor.getFullName() + " cancelled the reservation for " + reservation.getUnit().getListing().getTitle() + ".",
                "RESERVATION_CANCELLED", "Reservation", reservation.getId(),
                "Cancelled reservation " + reservation.getId());
        return toResponse(reservation);
    }

    @Transactional
    public int expireDue() {
        List<Reservation> due = reservations.findByStatusInAndEndAtBefore(
                EnumSet.of(ReservationStatus.REQUESTED, ReservationStatus.APPROVED, ReservationStatus.WAITLISTED),
                Instant.now());
        for (Reservation reservation : due) {
            reservation.expire();
            refreshUnit(reservation.getUnit());
            events.publish(null, reservation.getBorrower().getId(), NotificationType.RESERVATION_EXPIRED.name(),
                    "Reservation expired",
                    "Your reservation for " + reservation.getUnit().getListing().getTitle() + " expired.",
                    "RESERVATION_EXPIRED", "Reservation", reservation.getId(),
                    "Expired reservation " + reservation.getId());
            promoteWaitlist(reservation.getUnit());
        }
        return due.size();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> mine(User user) {
        return reservations.findByBorrowerIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> incoming(User owner) {
        return reservations.findIncoming(owner.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> all() {
        return reservations.findAllByOrderByCreatedAtDesc().stream().limit(300).map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(User user, Long id) {
        Reservation reservation = load(id);
        boolean borrower = reservation.getBorrower().getId().equals(user.getId());
        boolean owner = reservation.getUnit().getListing().getOwner().getId().equals(user.getId());
        if (!borrower && !owner && user.getRole() != com.crsms.model.enums.UserRole.ADMIN) {
            throw new ApiException(403, "You cannot view this reservation");
        }
        return toResponse(reservation);
    }

    Reservation load(Long id) {
        return reservations.findById(id).orElseThrow(() -> new ApiException(404, "Reservation not found"));
    }

    /**
     * Recompute the unit flag from open borrowings and approved holds.
     * Maintenance, lost, and inactive states are left untouched.
     */
    public void refreshUnit(ResourceUnit unit) {
        if (STICKY.contains(unit.getStatus())) {
            return;
        }
        long openBorrows = borrowings.countByUnitIdAndStatusIn(unit.getId(), OPEN_BORROW);
        if (openBorrows > 0) {
            unit.changeStatus(UnitStatus.BORROWED);
            return;
        }
        long approved = reservations.countByUnitIdAndStatus(unit.getId(), ReservationStatus.APPROVED);
        unit.changeStatus(approved > 0 ? UnitStatus.RESERVED : UnitStatus.AVAILABLE);
    }

    public void promoteWaitlist(ResourceUnit unit) {
        if (STICKY.contains(unit.getStatus()) || unit.getCondition().blocksReservation()) {
            return;
        }
        List<Reservation> waiting = reservations.findByUnitIdAndStatusOrderByCreatedAtAsc(
                unit.getId(), ReservationStatus.WAITLISTED);
        for (Reservation candidate : waiting) {
            if (!candidate.getEndAt().isAfter(Instant.now())) {
                candidate.expire();
                events.publish(null, candidate.getBorrower().getId(), NotificationType.RESERVATION_EXPIRED.name(),
                        "Waiting-list entry expired",
                        "Your waiting-list window for " + unit.getListing().getTitle() + " has passed.",
                        "RESERVATION_EXPIRED", "Reservation", candidate.getId(),
                        "Expired waiting-list reservation " + candidate.getId());
                continue;
            }
            entityManager.flush();
            long overlaps = reservations.countOverlapping(unit.getId(), candidate.getStartAt(), candidate.getEndAt(), BLOCKING);
            if (overlaps > 0) {
                continue;
            }
            candidate.promoteFromWaitlist();
            entityManager.flush();
            events.publish(null, candidate.getBorrower().getId(), NotificationType.WAITLIST_PROMOTED.name(),
                    "You can be reviewed",
                    "A slot opened for " + unit.getListing().getTitle() + ". The owner can now approve your request.",
                    "WAITLIST_PROMOTED", "Reservation", candidate.getId(),
                    "Promoted reservation " + candidate.getId() + " off the waiting list");
            events.publish(null, unit.getListing().getOwner().getId(), NotificationType.WAITLIST_PROMOTED.name(),
                    "Waiting list moved up",
                    candidate.getBorrower().getFullName() + " is now requesting " + unit.getAssetCode() + ".",
                    null, "Reservation", candidate.getId(), null);
        }
    }

    private void assertOwner(User actor, Reservation reservation) {
        if (reservation.getBorrower().getId().equals(actor.getId())) {
            throw new ApiException(403, "You cannot approve or reject your own request");
        }
        if (!reservation.getUnit().getListing().getOwner().getId().equals(actor.getId())) {
            throw new ApiException(403, "Only the owner can decide this request");
        }
    }

    ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUnit().getId(),
                reservation.getUnit().getAssetCode(),
                reservation.getUnit().getListing().getTitle(),
                reservation.getBorrower().getId(),
                reservation.getBorrower().getFullName(),
                reservation.getUnit().getListing().getOwner().getId(),
                reservation.getStartAt(),
                reservation.getEndAt(),
                reservation.getStatus().name(),
                reservation.getDecisionNote(),
                reservation.getQueuePosition());
    }
}
