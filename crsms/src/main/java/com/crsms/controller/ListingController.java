package com.crsms.controller;

import com.crsms.dto.ApiDtos.AvailabilityResponse;
import com.crsms.dto.ApiDtos.CategoryResponse;
import com.crsms.dto.ApiDtos.ConditionHistoryResponse;
import com.crsms.dto.ApiDtos.ListingRequest;
import com.crsms.dto.ApiDtos.ListingResponse;
import com.crsms.dto.ApiDtos.PageResponse;
import com.crsms.dto.ApiDtos.UnitRequest;
import com.crsms.dto.ApiDtos.UnitResponse;
import com.crsms.repository.ResourceCategoryRepository;
import com.crsms.service.ConditionHistoryService;
import com.crsms.service.CurrentUserService;
import com.crsms.service.ListingService;
import com.crsms.service.ReservationService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ListingController {
    private final ListingService listings;
    private final CurrentUserService current;
    private final ResourceCategoryRepository categories;
    private final ReservationService reservations;
    private final ConditionHistoryService conditions;

    public ListingController(ListingService listings, CurrentUserService current, ResourceCategoryRepository categories,
                             ReservationService reservations, ConditionHistoryService conditions) {
        this.listings = listings;
        this.current = current;
        this.categories = categories;
        this.reservations = reservations;
        this.conditions = conditions;
    }

    @GetMapping("/api/categories")
    public List<CategoryResponse> categories() {
        return categories.findAll().stream().map(c -> new CategoryResponse(c.getId(), c.getName())).toList();
    }

    @GetMapping("/api/listings")
    public PageResponse<ListingResponse> browse(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) BigDecimal minRate,
                                                 @RequestParam(required = false) BigDecimal maxRate,
                                                 @RequestParam(required = false) String owner,
                                                 @RequestParam(required = false) String condition,
                                                 @RequestParam(defaultValue = "false") boolean availableOnly,
                                                 @RequestParam(defaultValue = "newest") String sort,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "12") int size) {
        return listings.browse(q, categoryId, minRate, maxRate, owner, condition, availableOnly, sort, page, size);
    }

    @GetMapping("/api/my/listings")
    public PageResponse<ListingResponse> mine(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return listings.mine(current.require(), page, size);
    }

    @GetMapping("/api/listings/{id}")
    public ListingResponse one(@PathVariable Long id) {
        return listings.get(id);
    }

    @PostMapping("/api/listings")
    @ResponseStatus(HttpStatus.CREATED)
    public ListingResponse create(@Valid @RequestBody ListingRequest request) {
        return listings.create(current.require(), request);
    }

    @PutMapping("/api/listings/{id}")
    public ListingResponse update(@PathVariable Long id, @Valid @RequestBody ListingRequest request) {
        return listings.update(current.require(), id, request);
    }

    @DeleteMapping("/api/listings/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        listings.deactivate(current.require(), id);
        return Map.of("status", "inactive");
    }

    @PostMapping("/api/listings/{id}/units")
    @ResponseStatus(HttpStatus.CREATED)
    public UnitResponse addUnit(@PathVariable Long id, @RequestBody UnitRequest request) {
        return listings.addUnit(current.require(), id, request);
    }

    @GetMapping("/api/units/{id}")
    public UnitResponse unit(@PathVariable Long id) {
        return listings.getUnit(id);
    }

    @GetMapping("/api/units/{id}/availability")
    public AvailabilityResponse availability(@PathVariable Long id,
                                             @RequestParam Instant startAt,
                                             @RequestParam Instant endAt) {
        return reservations.availability(id, startAt, endAt);
    }

    @GetMapping("/api/units/{id}/history")
    public List<ConditionHistoryResponse> history(@PathVariable Long id) {
        current.require();
        return conditions.forUnit(id);
    }
}
