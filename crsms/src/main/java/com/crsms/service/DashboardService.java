package com.crsms.service;

import com.crsms.dto.ApiDtos.DashboardSummary;
import com.crsms.model.enums.BorrowingStatus;
import com.crsms.model.enums.ReservationStatus;
import com.crsms.model.user.User;
import com.crsms.repository.BorrowingRecordRepository;
import com.crsms.repository.ReservationRepository;
import com.crsms.repository.ResourceListingRepository;
import java.util.EnumSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final ResourceListingRepository listings;
    private final ReservationRepository reservations;
    private final BorrowingRecordRepository borrowings;
    private final NotificationService notifications;
    private final PenaltyService penalties;

    public DashboardService(ResourceListingRepository listings, ReservationRepository reservations,
                            BorrowingRecordRepository borrowings, NotificationService notifications,
                            PenaltyService penalties) {
        this.listings = listings;
        this.reservations = reservations;
        this.borrowings = borrowings;
        this.notifications = notifications;
        this.penalties = penalties;
    }

    @Transactional(readOnly = true)
    public DashboardSummary mine(User user) {
        return new DashboardSummary(
                user.getRole().name(),
                user.getFullName(),
                listings.countByOwnerId(user.getId()),
                reservations.countOpenForBorrower(user.getId(), EnumSet.of(
                        ReservationStatus.REQUESTED, ReservationStatus.APPROVED, ReservationStatus.WAITLISTED, ReservationStatus.ACTIVE)),
                borrowings.countByBorrowerIdAndStatusIn(user.getId(), EnumSet.of(BorrowingStatus.ACTIVE, BorrowingStatus.OVERDUE)),
                notifications.unread(user).unread(),
                penalties.outstanding(user),
                reservations.countIncomingRequests(user.getId()));
    }
}
