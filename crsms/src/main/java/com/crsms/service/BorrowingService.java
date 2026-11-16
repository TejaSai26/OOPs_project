package com.crsms.service;

import com.crsms.dto.ApiDtos.BorrowingResponse;
import com.crsms.dto.ApiDtos.ReturnRequest;
import com.crsms.event.DomainEventPublisher;
import com.crsms.exception.ApiException;
import com.crsms.model.borrowing.BorrowingRecord;
import com.crsms.model.enums.BorrowingStatus;
import com.crsms.model.enums.ConditionPhase;
import com.crsms.model.enums.NotificationType;
import com.crsms.model.enums.ReservationStatus;
import com.crsms.model.enums.ResourceCondition;
import com.crsms.model.enums.UnitStatus;
import com.crsms.model.maintenance.MaintenanceRequest;
import com.crsms.model.payment.Payment;
import com.crsms.model.penalty.Penalty;
import com.crsms.model.reservation.Reservation;
import com.crsms.model.user.User;
import com.crsms.penalty.PenaltyStrategyFactory;
import com.crsms.pricing.PricingStrategyFactory;
import com.crsms.repository.BorrowingRecordRepository;
import com.crsms.repository.MaintenanceRequestRepository;
import com.crsms.repository.PaymentRepository;
import com.crsms.repository.PenaltyRepository;
import com.crsms.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BorrowingService {
    private final BorrowingRecordRepository borrowings;
    private final ReservationRepository reservations;
    private final PaymentRepository payments;
    private final PenaltyRepository penalties;
    private final MaintenanceRequestRepository maintenance;
    private final PricingStrategyFactory pricing;
    private final PenaltyStrategyFactory penaltyStrategies;
    private final SettingsService settings;
    private final ConditionHistoryService conditions;
    private final ReservationService reservationService;
    private final DomainEventPublisher events;

    public BorrowingService(BorrowingRecordRepository borrowings, ReservationRepository reservations,
                            PaymentRepository payments, PenaltyRepository penalties,
                            MaintenanceRequestRepository maintenance, PricingStrategyFactory pricing,
                            PenaltyStrategyFactory penaltyStrategies, SettingsService settings,
                            ConditionHistoryService conditions, ReservationService reservationService,
                            DomainEventPublisher events) {
        this.borrowings = borrowings;
        this.reservations = reservations;
        this.payments = payments;
        this.penalties = penalties;
        this.maintenance = maintenance;
        this.pricing = pricing;
        this.penaltyStrategies = penaltyStrategies;
        this.settings = settings;
        this.conditions = conditions;
        this.reservationService = reservationService;
        this.events = events;
    }

    @Transactional
    public BorrowingResponse pickup(User actor, Long reservationId) {
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new ApiException(404, "Reservation not found"));
        if (!reservation.getBorrower().getId().equals(actor.getId())) {
            throw new ApiException(403, "Only the borrower can confirm pickup");
        }
        if (reservation.getStatus() != ReservationStatus.APPROVED) {
            throw new ApiException(400, "Pickup is allowed only after approval");
        }
        if (borrowings.findByReservationId(reservationId).isPresent()) {
            throw new ApiException(409, "This reservation was already picked up");
        }
        if (reservation.getUnit().getCondition().blocksReservation()) {
            throw new ApiException(400, "This unit is under maintenance and cannot be picked up");
        }
        if (!reservation.getUnit().getStatus().isReservable()
                && reservation.getUnit().getStatus() != UnitStatus.RESERVED) {
            throw new ApiException(400, "Unit cannot be picked up in status " + reservation.getUnit().getStatus());
        }
        ResourceCondition out = reservation.getUnit().getCondition();
        conditions.record(reservation.getUnit(), out, ConditionPhase.BEFORE_BORROW, actor,
                "Condition recorded at pickup", null);
        reservation.markPickedUp();
        reservation.getUnit().changeStatus(UnitStatus.BORROWED);
        reservation.getUnit().getListing().incrementBorrowCount();
        BorrowingRecord record = borrowings.save(new BorrowingRecord(reservation));
        events.publish(actor.getId(), reservation.getUnit().getListing().getOwner().getId(), null, null, null,
                "PICKUP", "BorrowingRecord", record.getId(),
                actor.getFullName() + " picked up " + reservation.getUnit().getAssetCode());
        return toResponse(record);
    }

    @Transactional
    public BorrowingResponse giveBack(User actor, Long borrowingId, ReturnRequest request) {
        BorrowingRecord record = borrowings.findById(borrowingId)
                .orElseThrow(() -> new ApiException(404, "Borrowing not found"));
        if (!record.getBorrower().getId().equals(actor.getId())) {
            throw new ApiException(403, "Only the borrower can return this unit");
        }
        if (record.getStatus() == BorrowingStatus.RETURNED) {
            throw new ApiException(409, "This borrowing was already returned");
        }
        Instant now = Instant.now();
        boolean late = now.isAfter(record.getDueAt());
        BigDecimal charge = pricing.forType(record.getUnit().getListing().getPricingType())
                .calculate(record.getUnit().getListing().getRate(), record.getPickedUpAt(), now);
        BigDecimal penaltyAmount = penaltyStrategies.forType(record.getUnit().getListing().getPricingType())
                .calculate(record.getUnit().getListing().getRate(), record.getDueAt(), now, settings.multiplier());
        boolean damaged = request != null && request.damaged();
        ResourceCondition incoming = resolveIncoming(record, request, damaged);
        record.completeReturn(now, charge, late, penaltyAmount, incoming);
        record.getUnit().changeCondition(incoming);
        record.getReservation().markReturned();
        conditions.record(record.getUnit(), incoming, ConditionPhase.AFTER_RETURN, actor,
                damaged ? "Returned with damage" : "Returned",
                request == null ? null : request.photoRef());
        if (damaged) {
            record.getUnit().changeStatus(UnitStatus.MAINTENANCE_REQUIRED);
            String note = request.damageNote() == null || request.damageNote().isBlank()
                    ? "Damage reported at return" : request.damageNote().trim();
            MaintenanceRequest ticket = maintenance.save(new MaintenanceRequest(record.getUnit(), actor, note, request.photoRef()));
            conditions.record(record.getUnit(), ResourceCondition.DAMAGED, ConditionPhase.DAMAGE_REPORT, actor, note, request.photoRef());
            events.publish(actor.getId(), record.getUnit().getListing().getOwner().getId(),
                    NotificationType.DAMAGE_REPORT.name(),
                    "Damage reported",
                    actor.getFullName() + " reported damage on " + record.getUnit().getAssetCode() + ": " + note,
                    "MAINTENANCE_REPORTED", "MaintenanceRequest", ticket.getId(),
                    "Damage report on " + record.getUnit().getAssetCode());
        } else {
            reservationService.refreshUnit(record.getUnit());
        }
        Payment payment = payments.save(new Payment(record, charge));
        events.publish(actor.getId(), null, null, null, null,
                "PAYMENT", "Payment", payment.getId(),
                "Simulated rental charge " + charge + " for borrowing " + record.getId()
                        + " (" + payment.getStatus() + ")");
        events.publish(actor.getId(), record.getUnit().getListing().getOwner().getId(), null, null, null,
                "RETURN", "BorrowingRecord", record.getId(),
                actor.getFullName() + " returned " + record.getUnit().getAssetCode()
                        + (late ? " late" : " on time"));
        if (penaltyAmount.signum() > 0) {
            Penalty penalty = penalties.save(new Penalty(record, penaltyAmount,
                    "Late return after " + record.getDueAt()));
            events.publish(actor.getId(), actor.getId(), NotificationType.PENALTY_GENERATED.name(),
                    "Late penalty added",
                    "A simulated penalty of " + penaltyAmount + " was added for " + record.getUnit().getAssetCode() + ".",
                    "PENALTY_GENERATED", "Penalty", penalty.getId(),
                    "Penalty " + penaltyAmount + " on borrowing " + record.getId());
        }
        reservationService.promoteWaitlist(record.getUnit());
        return toResponse(record);
    }

    @Transactional
    public int flagOverdueAndRemind() {
        Instant now = Instant.now();
        int changes = 0;
        Instant horizon = now.plus(settings.reminderHours(), ChronoUnit.HOURS);
        for (BorrowingRecord record : borrowings.findNeedingReminder(now, horizon)) {
            if (record.markReminded()) {
                changes++;
                events.publish(null, record.getBorrower().getId(), NotificationType.UPCOMING_RETURN.name(),
                        "Return coming up",
                        record.getUnit().getListing().getTitle() + " (" + record.getUnit().getAssetCode()
                                + ") is due " + record.getDueAt() + ".",
                        null, "BorrowingRecord", record.getId(), null);
            }
        }
        for (BorrowingRecord record : borrowings.findNeedingOverdueNotice(now)) {
            record.markOverdue(now);
            if (record.claimOverdueNotice()) {
                changes++;
                events.publish(null, record.getBorrower().getId(), NotificationType.OVERDUE.name(),
                        "Resource is overdue",
                        record.getUnit().getListing().getTitle() + " was due " + record.getDueAt() + ". Please return it.",
                        "OVERDUE_FLAGGED", "BorrowingRecord", record.getId(),
                        "Borrowing " + record.getId() + " is overdue");
            }
        }
        return changes;
    }

    @Transactional
    public List<BorrowingResponse> mine(User user) {
        return borrowings.findByBorrowerIdOrderByPickedUpAtDesc(user.getId()).stream().map(record -> {
            record.markOverdue(Instant.now());
            return toResponse(record);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<BorrowingResponse> all() {
        return borrowings.findAllByOrderByPickedUpAtDesc().stream().limit(300).map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BorrowingResponse get(User user, Long id) {
        BorrowingRecord record = borrowings.findById(id).orElseThrow(() -> new ApiException(404, "Borrowing not found"));
        boolean borrower = record.getBorrower().getId().equals(user.getId());
        boolean owner = record.getUnit().getListing().getOwner().getId().equals(user.getId());
        if (!borrower && !owner && user.getRole() != com.crsms.model.enums.UserRole.ADMIN) {
            throw new ApiException(403, "You cannot view this borrowing");
        }
        return toResponse(record);
    }

    private ResourceCondition resolveIncoming(BorrowingRecord record, ReturnRequest request, boolean damaged) {
        if (damaged) {
            return ResourceCondition.DAMAGED;
        }
        if (request != null && request.condition() != null && !request.condition().isBlank()) {
            try {
                return ResourceCondition.valueOf(request.condition().trim().toUpperCase(Locale.ROOT));
            } catch (RuntimeException ex) {
                throw new ApiException(400, "Condition must be NEW, GOOD, FAIR, DAMAGED, or UNDER_MAINTENANCE");
            }
        }
        return record.getConditionOut() == null ? record.getUnit().getCondition() : record.getConditionOut();
    }

    private BorrowingResponse toResponse(BorrowingRecord record) {
        Payment payment = payments.findByBorrowingId(record.getId()).orElse(null);
        Penalty penalty = penalties.findByBorrowingId(record.getId()).orElse(null);
        return new BorrowingResponse(
                record.getId(),
                record.getReservation().getId(),
                record.getUnit().getListing().getTitle(),
                record.getUnit().getAssetCode(),
                record.getPickedUpAt(),
                record.getDueAt(),
                record.getReturnedAt(),
                record.getChargeAmount(),
                record.isLate(),
                record.getStatus().name(),
                payment == null ? null : payment.getStatus().name(),
                penalty == null ? record.getPenaltyAmount() : penalty.getAmount(),
                penalty == null ? null : penalty.getPaymentStatus().name(),
                record.getConditionOut() == null ? null : record.getConditionOut().name(),
                record.getConditionIn() == null ? null : record.getConditionIn().name());
    }

    static EnumSet<BorrowingStatus> openStatuses() {
        return EnumSet.of(BorrowingStatus.ACTIVE, BorrowingStatus.OVERDUE);
    }
}
