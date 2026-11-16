package com.crsms.controller;

import com.crsms.dto.ApiDtos.AnalyticsResponse;
import com.crsms.dto.ApiDtos.AuditResponse;
import com.crsms.dto.ApiDtos.BorrowingResponse;
import com.crsms.dto.ApiDtos.ListingResponse;
import com.crsms.dto.ApiDtos.MaintenanceResponse;
import com.crsms.dto.ApiDtos.PageResponse;
import com.crsms.dto.ApiDtos.PenaltyResponse;
import com.crsms.dto.ApiDtos.ReservationResponse;
import com.crsms.dto.ApiDtos.SettingsResponse;
import com.crsms.dto.ApiDtos.SettingsUpdate;
import com.crsms.dto.ApiDtos.StatusUpdate;
import com.crsms.dto.ApiDtos.UserResponse;
import com.crsms.dto.ApiDtos.WaiveRequest;
import com.crsms.service.AnalyticsService;
import com.crsms.service.AuditQueryService;
import com.crsms.service.BorrowingService;
import com.crsms.service.CurrentUserService;
import com.crsms.service.ListingService;
import com.crsms.service.MaintenanceService;
import com.crsms.service.PenaltyService;
import com.crsms.service.ReservationService;
import com.crsms.service.SettingsService;
import com.crsms.service.UserService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AnalyticsService analytics;
    private final UserService users;
    private final CurrentUserService current;
    private final AuditQueryService audits;
    private final ReservationService reservations;
    private final BorrowingService borrowings;
    private final ListingService listings;
    private final MaintenanceService maintenance;
    private final PenaltyService penalties;
    private final SettingsService settings;

    public AdminController(AnalyticsService analytics, UserService users, CurrentUserService current,
                           AuditQueryService audits, ReservationService reservations, BorrowingService borrowings,
                           ListingService listings, MaintenanceService maintenance, PenaltyService penalties,
                           SettingsService settings) {
        this.analytics = analytics;
        this.users = users;
        this.current = current;
        this.audits = audits;
        this.reservations = reservations;
        this.borrowings = borrowings;
        this.listings = listings;
        this.maintenance = maintenance;
        this.penalties = penalties;
        this.settings = settings;
    }

    @GetMapping("/analytics")
    public AnalyticsResponse analytics() {
        return analytics.snapshot();
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return users.listUsers();
    }

    @PutMapping("/users/{id}/status")
    public UserResponse status(@PathVariable Long id, @Valid @RequestBody StatusUpdate update) {
        return users.setStatus(current.require(), id, update.accountStatus());
    }

    @GetMapping("/audit")
    public List<AuditResponse> audit(@RequestParam(required = false) String action,
                                      @RequestParam(required = false) String entityType,
                                      @RequestParam(required = false) Long actorId,
                                      @RequestParam(required = false) Instant from,
                                      @RequestParam(required = false) Instant to) {
        return audits.search(action, entityType, actorId, from, to);
    }

    @GetMapping("/reservations")
    public List<ReservationResponse> reservations() {
        return reservations.all();
    }

    @GetMapping("/borrowings")
    public List<BorrowingResponse> borrowings() {
        return borrowings.all();
    }

    @GetMapping("/listings")
    public PageResponse<ListingResponse> listings(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return listings.allForAdmin(page, size);
    }

    @PostMapping("/listings/{id}/deactivate")
    public Map<String, String> deactivate(@PathVariable Long id) {
        listings.adminDeactivate(current.require(), id);
        return Map.of("status", "inactive");
    }

    @GetMapping("/maintenance")
    public List<MaintenanceResponse> maintenance() {
        return maintenance.list();
    }

    @GetMapping("/penalties")
    public List<PenaltyResponse> penalties() {
        return penalties.all();
    }

    @PostMapping("/penalties/{id}/waive")
    public PenaltyResponse waive(@PathVariable Long id, @Valid @RequestBody WaiveRequest request) {
        return penalties.waive(current.require(), id, request);
    }

    @GetMapping("/settings")
    public SettingsResponse settings() {
        return settings.view();
    }

    @PutMapping("/settings")
    public SettingsResponse updateSettings(@Valid @RequestBody SettingsUpdate update) {
        return settings.update(current.require(), update);
    }
}
