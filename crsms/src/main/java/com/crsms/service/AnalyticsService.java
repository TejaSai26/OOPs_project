package com.crsms.service;

import com.crsms.dto.ApiDtos.AnalyticsResponse;
import com.crsms.dto.ApiDtos.CategoryCount;
import com.crsms.dto.ApiDtos.MonthPoint;
import com.crsms.model.enums.BorrowingStatus;
import com.crsms.model.enums.ListingStatus;
import com.crsms.model.enums.MaintenanceStatus;
import com.crsms.model.enums.PaymentStatus;
import com.crsms.model.enums.PenaltyStatus;
import com.crsms.model.enums.ReservationStatus;
import com.crsms.model.enums.UnitStatus;
import com.crsms.model.enums.UserRole;
import com.crsms.repository.BorrowingRecordRepository;
import com.crsms.repository.MaintenanceRequestRepository;
import com.crsms.repository.PaymentRepository;
import com.crsms.repository.PenaltyRepository;
import com.crsms.repository.ReservationRepository;
import com.crsms.repository.ResourceListingRepository;
import com.crsms.repository.ResourceUnitRepository;
import com.crsms.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private final UserRepository users;
    private final ResourceListingRepository listings;
    private final ResourceUnitRepository units;
    private final ReservationRepository reservations;
    private final BorrowingRecordRepository borrowings;
    private final MaintenanceRequestRepository maintenance;
    private final PaymentRepository payments;
    private final PenaltyRepository penalties;

    public AnalyticsService(UserRepository users, ResourceListingRepository listings, ResourceUnitRepository units,
                            ReservationRepository reservations, BorrowingRecordRepository borrowings,
                            MaintenanceRequestRepository maintenance, PaymentRepository payments,
                            PenaltyRepository penalties) {
        this.users = users;
        this.listings = listings;
        this.units = units;
        this.reservations = reservations;
        this.borrowings = borrowings;
        this.maintenance = maintenance;
        this.payments = payments;
        this.penalties = penalties;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse snapshot() {
        List<CategoryCount> topCategories = borrowings.categoryCounts().stream()
                .map(row -> new CategoryCount((String) row[0], ((Number) row[1]).longValue()))
                .toList();
        List<CategoryCount> topResources = borrowings.popularResources().stream().limit(8)
                .map(row -> new CategoryCount((String) row[0], ((Number) row[1]).longValue()))
                .toList();
        BigDecimal rental = zero(payments.sumByStatus(PaymentStatus.SIMULATED));
        BigDecimal paidPenalties = zero(penalties.sumByStatus(PenaltyStatus.PAID));
        BigDecimal outstanding = zero(penalties.sumByStatus(PenaltyStatus.UNPAID));
        BigDecimal repairs = zero(maintenance.sumRepairCost());
        return new AnalyticsResponse(
                users.count(),
                users.countByRole(UserRole.STUDENT),
                users.countByRole(UserRole.FACULTY),
                users.countByRole(UserRole.STAFF),
                users.countByRole(UserRole.ADMIN),
                listings.count(),
                listings.countByListingStatus(ListingStatus.ACTIVE),
                units.count(),
                units.countByStatus(UnitStatus.AVAILABLE),
                units.countByStatus(UnitStatus.RESERVED),
                units.countByStatus(UnitStatus.BORROWED),
                borrowings.countOverdue(Instant.now()),
                units.countOutOfService(EnumSet.of(UnitStatus.MAINTENANCE_REQUIRED, UnitStatus.UNDER_REPAIR)),
                rental.add(paidPenalties),
                outstanding,
                repairs,
                topCategories,
                topResources,
                lastMonths(reservations.monthlyCounts()),
                lastMonths(borrowings.monthlyCounts()),
                maintenance.countByStatus(MaintenanceStatus.OPEN),
                maintenance.countByStatus(MaintenanceStatus.IN_PROGRESS),
                maintenance.countByStatus(MaintenanceStatus.RESOLVED),
                maintenance.countByStatus(MaintenanceStatus.REJECTED));
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<MonthPoint> lastMonths(List<Object[]> rows) {
        Map<YearMonth, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            YearMonth key = YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            counts.put(key, ((Number) row[2]).longValue());
        }
        YearMonth cursor = YearMonth.now(ZoneOffset.UTC).minusMonths(5);
        List<MonthPoint> points = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            points.add(new MonthPoint(cursor.getYear(), cursor.getMonthValue(), counts.getOrDefault(cursor, 0L)));
            cursor = cursor.plusMonths(1);
        }
        return points;
    }

    /** Kept so older call sites that counted active reservations still compile if referenced in tests. */
    public long openReservations() {
        return reservations.countByStatusIn(EnumSet.of(
                ReservationStatus.REQUESTED, ReservationStatus.APPROVED, ReservationStatus.ACTIVE));
    }
}
