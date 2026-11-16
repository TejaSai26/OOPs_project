package com.crsms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 80) String password,
            @NotBlank String role,
            String department,
            String memberId,
            String designation
    ) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record ProfileUpdate(@NotBlank String fullName, String department) {
    }

    public record PasswordChange(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 80) String newPassword) {
    }

    public record UserResponse(Long id, String fullName, String email, String role, String accountStatus,
                                String department, String memberLabel) {
    }

    public record CategoryResponse(Long id, String name) {
    }

    public record ListingRequest(
            @NotBlank String title,
            @NotBlank String description,
            @NotNull Long categoryId,
            @NotBlank String pricingType,
            @NotNull BigDecimal rate
    ) {
    }

    public record UnitRequest(String assetCode, String condition, String locationNote) {
    }

    public record UnitResponse(Long id, String assetCode, String condition, String status, String locationNote) {
    }

    public record ListingResponse(Long id, String title, String description, String category, Long categoryId,
                                   String pricingType, BigDecimal rate, String listingStatus,
                                   Long ownerId, String ownerName, List<UnitResponse> units,
                                   long borrowCount, Double averageRating, long reviewCount, Double ownerRating,
                                   int availableCount) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long total) {
    }

    public record ReservationRequest(@NotNull Long unitId, @NotNull Instant startAt, @NotNull Instant endAt,
                                      Boolean joinWaitlist) {
    }

    public record ReservationResponse(Long id, Long unitId, String assetCode, String listingTitle,
                                       Long borrowerId, String borrowerName, Long ownerId,
                                       Instant startAt, Instant endAt, String status,
                                       String decisionNote, Integer queuePosition) {
    }

    public record DecisionNote(String note) {
    }

    public record AvailabilityResponse(boolean available, long overlaps, String unitStatus, String condition) {
    }

    public record BorrowingResponse(Long id, Long reservationId, String listingTitle, String assetCode,
                                     Instant pickedUpAt, Instant dueAt, Instant returnedAt,
                                     BigDecimal chargeAmount, boolean late, String status, String paymentStatus,
                                     BigDecimal penaltyAmount, String penaltyStatus,
                                     String conditionOut, String conditionIn) {
    }

    public record ReturnRequest(boolean damaged, String damageNote, String condition, String photoRef) {
    }

    public record MaintenanceCreate(@NotNull Long unitId, @NotBlank String description, String photoRef) {
    }

    public record MaintenanceUpdate(String status, String unitStatus, String condition, String adminNote,
                                     Long assigneeId, BigDecimal repairCost, String photoRef) {
    }

    public record MaintenanceResponse(Long id, Long unitId, String assetCode, String listingTitle,
                                       String reporterName, String description, String status,
                                       String adminNote, String unitStatus, Instant createdAt,
                                       String assigneeName, BigDecimal repairCost, String photoRef) {
    }

    public record ReviewRequest(@NotNull Long borrowingId, @Min(1) @Max(5) int rating, String comment) {
    }

    public record ReviewResponse(Long id, int rating, String comment, String reviewerName,
                                  Instant createdAt, String listingTitle, Long borrowingId) {
    }

    public record ReviewPage(List<ReviewResponse> reviews, Double averageRating, long count) {
    }

    public record StatusUpdate(@NotBlank String accountStatus) {
    }

    public record CategoryCount(String category, long borrowings) {
    }

    public record MonthPoint(int year, int month, long count) {
    }

    public record AnalyticsResponse(long totalUsers, long students, long faculty, long staff, long admins,
                                     long totalListings, long activeListings, long totalUnits,
                                     long availableUnits, long reservedUnits, long borrowedUnits,
                                     long overdueBorrowings, long maintenanceUnits,
                                     BigDecimal totalRevenue, BigDecimal outstandingPenalties,
                                     BigDecimal maintenanceSpend,
                                     List<CategoryCount> topCategories, List<CategoryCount> topResources,
                                     List<MonthPoint> reservationTrend, List<MonthPoint> borrowingTrend,
                                     long maintenanceOpen, long maintenanceInProgress,
                                     long maintenanceResolved, long maintenanceRejected) {
    }

    public record PenaltyResponse(Long id, Long borrowingId, String listingTitle, String assetCode,
                                   BigDecimal amount, String paymentStatus, String reason,
                                   String waivedReason, Instant createdAt) {
    }

    public record PayRequest(@NotNull BigDecimal amount) {
    }

    public record WaiveRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record NotificationResponse(Long id, String type, String title, String message, boolean read,
                                        Instant createdAt, String entityType, Long entityId) {
    }

    public record NotificationCount(long unread) {
    }

    public record AuditResponse(Long id, Long actorId, String actorName, String action, String entityType,
                                 String entityId, String description, Instant createdAt) {
    }

    public record SettingsResponse(BigDecimal lateFeeMultiplier, int reminderHours) {
    }

    public record SettingsUpdate(@NotNull BigDecimal lateFeeMultiplier, @Min(1) @Max(168) int reminderHours) {
    }

    public record ConditionHistoryResponse(Long id, String condition, String phase, String note, String photoRef,
                                            String recordedBy, Instant recordedAt) {
    }

    public record DashboardSummary(String role, String fullName, long myListings, long myOpenReservations,
                                    long myActiveBorrowings, long unreadNotifications,
                                    BigDecimal outstandingPenalties, long incomingRequests) {
    }

    public record UploadResponse(String url) {
    }
}
