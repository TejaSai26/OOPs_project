package com.crsms.service;

import com.crsms.dto.ApiDtos.PayRequest;
import com.crsms.dto.ApiDtos.PenaltyResponse;
import com.crsms.dto.ApiDtos.WaiveRequest;
import com.crsms.event.DomainEventPublisher;
import com.crsms.exception.ApiException;
import com.crsms.model.enums.NotificationType;
import com.crsms.model.enums.UserRole;
import com.crsms.model.penalty.Penalty;
import com.crsms.model.user.User;
import com.crsms.repository.PenaltyRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PenaltyService {
    private final PenaltyRepository penalties;
    private final DomainEventPublisher events;

    public PenaltyService(PenaltyRepository penalties, DomainEventPublisher events) {
        this.penalties = penalties;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<PenaltyResponse> mine(User user) {
        return penalties.findByBorrowerIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PenaltyResponse> all() {
        return penalties.findAllByOrderByCreatedAtDesc().stream().limit(300).map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal outstanding(User user) {
        BigDecimal sum = penalties.sumForBorrower(user.getId(), com.crsms.model.enums.PenaltyStatus.UNPAID);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Transactional
    public PenaltyResponse pay(User actor, Long id, PayRequest request) {
        Penalty penalty = load(id);
        if (!penalty.getBorrower().getId().equals(actor.getId())) {
            throw new ApiException(403, "Only the borrower can pay this penalty");
        }
        if (request.amount() == null || request.amount().compareTo(penalty.getAmount()) != 0) {
            throw new ApiException(400, "Payment amount must match the outstanding penalty");
        }
        try {
            penalty.markPaid();
        } catch (IllegalStateException ex) {
            throw new ApiException(409, ex.getMessage());
        }
        events.publish(actor.getId(), null, null, null, null,
                "PAYMENT", "Penalty", penalty.getId(),
                "Simulated penalty payment of " + penalty.getAmount() + " on penalty " + penalty.getId());
        events.publish(actor.getId(), actor.getId(), NotificationType.PENALTY_PAID.name(),
                "Penalty marked paid",
                "Your simulated payment of " + penalty.getAmount() + " was recorded.",
                null, "Penalty", penalty.getId(), null);
        return toResponse(penalty);
    }

    @Transactional
    public PenaltyResponse waive(User admin, Long id, WaiveRequest request) {
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ApiException(403, "Only an administrator can waive a penalty");
        }
        Penalty penalty = load(id);
        try {
            penalty.waive(admin, request.reason());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ApiException(400, ex.getMessage());
        }
        events.publish(admin.getId(), penalty.getBorrower().getId(), NotificationType.PENALTY_WAIVED.name(),
                "Penalty waived",
                "An administrator waived " + penalty.getAmount() + ". Reason: " + request.reason().trim(),
                "PENALTY_WAIVED", "Penalty", penalty.getId(),
                "Waived penalty " + penalty.getId() + ": " + request.reason().trim());
        return toResponse(penalty);
    }

    private Penalty load(Long id) {
        return penalties.findById(id).orElseThrow(() -> new ApiException(404, "Penalty not found"));
    }

    private PenaltyResponse toResponse(Penalty penalty) {
        return new PenaltyResponse(
                penalty.getId(),
                penalty.getBorrowing().getId(),
                penalty.getBorrowing().getUnit().getListing().getTitle(),
                penalty.getBorrowing().getUnit().getAssetCode(),
                penalty.getAmount(),
                penalty.getPaymentStatus().name(),
                penalty.getReason(),
                penalty.getWaivedReason(),
                penalty.getCreatedAt());
    }
}
