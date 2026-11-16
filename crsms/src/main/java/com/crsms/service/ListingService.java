package com.crsms.service;

import com.crsms.dto.ApiDtos.ListingRequest;
import com.crsms.dto.ApiDtos.ListingResponse;
import com.crsms.dto.ApiDtos.PageResponse;
import com.crsms.dto.ApiDtos.UnitRequest;
import com.crsms.dto.ApiDtos.UnitResponse;
import com.crsms.event.DomainEventPublisher;
import com.crsms.exception.ApiException;
import com.crsms.model.enums.ConditionPhase;
import com.crsms.model.enums.ListingStatus;
import com.crsms.model.enums.PricingType;
import com.crsms.model.enums.ResourceCondition;
import com.crsms.model.enums.UnitStatus;
import com.crsms.model.enums.UserRole;
import com.crsms.model.resource.ResourceCategory;
import com.crsms.model.resource.ResourceListing;
import com.crsms.model.resource.ResourceUnit;
import com.crsms.model.user.User;
import com.crsms.repository.ResourceCategoryRepository;
import com.crsms.repository.ResourceListingRepository;
import com.crsms.repository.ResourceUnitRepository;
import com.crsms.repository.ReviewRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListingService {
    private final ResourceListingRepository listings;
    private final ResourceUnitRepository units;
    private final ResourceCategoryRepository categories;
    private final ReviewRepository reviews;
    private final ConditionHistoryService conditions;
    private final DomainEventPublisher events;

    public ListingService(ResourceListingRepository listings, ResourceUnitRepository units,
                          ResourceCategoryRepository categories, ReviewRepository reviews,
                          ConditionHistoryService conditions, DomainEventPublisher events) {
        this.listings = listings;
        this.units = units;
        this.categories = categories;
        this.reviews = reviews;
        this.conditions = conditions;
        this.events = events;
    }

    @Transactional
    public ListingResponse create(User owner, ListingRequest request) {
        ResourceCategory category = categories.findById(request.categoryId())
                .orElseThrow(() -> new ApiException(404, "Category not found"));
        PricingType pricing = parsePricing(request.pricingType());
        ResourceListing listing = listings.save(new ResourceListing(owner, category, request.title().trim(),
                request.description().trim(), pricing, request.rate()));
        events.publish(owner.getId(), null, null, null, null,
                "RESOURCE_CREATED", "ResourceListing", listing.getId(),
                owner.getFullName() + " listed " + listing.getTitle());
        return toResponse(listing, List.of(), Map.of(), Map.of());
    }

    @Transactional
    public ListingResponse update(User owner, Long id, ListingRequest request) {
        ResourceListing listing = requireOwned(owner, id);
        ResourceCategory category = categories.findById(request.categoryId())
                .orElseThrow(() -> new ApiException(404, "Category not found"));
        listing.edit(request.title().trim(), request.description().trim(), category,
                parsePricing(request.pricingType()), request.rate());
        events.publish(owner.getId(), null, null, null, null,
                "RESOURCE_MODIFIED", "ResourceListing", listing.getId(),
                "Updated listing " + listing.getTitle());
        return enrich(List.of(listing)).get(0);
    }

    @Transactional
    public void deactivate(User owner, Long id) {
        ResourceListing listing = requireOwned(owner, id);
        listing.deactivate();
        events.publish(owner.getId(), null, null, null, null,
                "RESOURCE_DEACTIVATED", "ResourceListing", listing.getId(),
                "Deactivated listing " + listing.getTitle());
    }

    @Transactional
    public void adminDeactivate(User admin, Long id) {
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ApiException(403, "Only an administrator can moderate listings");
        }
        ResourceListing listing = listings.findById(id).orElseThrow(() -> new ApiException(404, "Listing not found"));
        listing.deactivate();
        events.publish(admin.getId(), listing.getOwner().getId(), null, null, null,
                "RESOURCE_DEACTIVATED", "ResourceListing", listing.getId(),
                "Administrator deactivated listing " + listing.getTitle());
    }

    @Transactional
    public UnitResponse addUnit(User owner, Long listingId, UnitRequest request) {
        ResourceListing listing = requireOwned(owner, listingId);
        String code = request.assetCode() == null || request.assetCode().isBlank()
                ? "AST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT)
                : request.assetCode().trim();
        if (units.existsByAssetCodeIgnoreCase(code)) {
            throw new ApiException(409, "Asset code already exists");
        }
        ResourceCondition condition = ResourceCondition.GOOD;
        if (request.condition() != null && !request.condition().isBlank()) {
            condition = parseCondition(request.condition());
        }
        ResourceUnit unit = units.save(new ResourceUnit(listing, code, condition, request.locationNote()));
        conditions.record(unit, condition, ConditionPhase.INITIAL, owner, "Unit added to the catalogue", null);
        events.publish(owner.getId(), null, null, null, null,
                "RESOURCE_CREATED", "ResourceUnit", unit.getId(),
                "Added unit " + unit.getAssetCode() + " to " + listing.getTitle());
        return toUnit(unit);
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> browse(String q, Long categoryId, BigDecimal minRate, BigDecimal maxRate,
                                                 String owner, String condition, boolean availableOnly,
                                                 String sort, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<ResourceListing> result = listings.search(blank(q), categoryId, minRate, maxRate, blank(owner),
                parseOptionalCondition(condition), availableOnly, PageRequest.of(safePage, safeSize, sortOf(sort)));
        List<ListingResponse> items = enrich(result.getContent());
        return new PageResponse<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> mine(User owner, int page, int size) {
        Page<ResourceListing> result = listings.findByOwnerId(owner.getId(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("createdAt").descending()));
        return new PageResponse<>(enrich(result.getContent()), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> allForAdmin(int page, int size) {
        Page<ResourceListing> result = listings.findAll(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by("createdAt").descending()));
        return new PageResponse<>(enrich(result.getContent()), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ListingResponse get(Long id) {
        ResourceListing listing = listings.findById(id).orElseThrow(() -> new ApiException(404, "Listing not found"));
        return enrich(List.of(listing)).get(0);
    }

    @Transactional(readOnly = true)
    public UnitResponse getUnit(Long id) {
        return toUnit(units.findById(id).orElseThrow(() -> new ApiException(404, "Unit not found")));
    }

    private List<ListingResponse> enrich(List<ResourceListing> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(ResourceListing::getId).toList();
        Map<Long, List<ResourceUnit>> byListing = new HashMap<>();
        for (ResourceUnit unit : units.findByListingIdIn(ids)) {
            byListing.computeIfAbsent(unit.getListing().getId(), key -> new java.util.ArrayList<>()).add(unit);
        }
        Map<Long, double[]> rating = new HashMap<>();
        for (Object[] row : reviews.summarizeForListings(ids)) {
            rating.put(((Number) row[0]).longValue(), new double[]{((Number) row[1]).doubleValue(), ((Number) row[2]).longValue()});
        }
        List<Long> ownerIds = rows.stream().map(row -> row.getOwner().getId()).distinct().toList();
        Map<Long, Double> owners = new HashMap<>();
        if (!ownerIds.isEmpty()) {
            for (Object[] row : reviews.summarizeForOwners(ownerIds)) {
                owners.put(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue());
            }
        }
        return rows.stream().map(listing -> toResponse(listing,
                byListing.getOrDefault(listing.getId(), List.of()), rating, owners)).toList();
    }

    private ResourceListing requireOwned(User owner, Long id) {
        ResourceListing listing = listings.findById(id).orElseThrow(() -> new ApiException(404, "Listing not found"));
        if (!listing.getOwner().getId().equals(owner.getId())) {
            throw new ApiException(403, "You can only change your own listings");
        }
        return listing;
    }

    private PricingType parsePricing(String raw) {
        try {
            return PricingType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new ApiException(400, "Pricing must be FREE, HOURLY, or DAILY");
        }
    }

    private ResourceCondition parseCondition(String raw) {
        try {
            return ResourceCondition.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new ApiException(400, "Condition must be NEW, GOOD, FAIR, DAMAGED, or UNDER_MAINTENANCE");
        }
    }

    private ResourceCondition parseOptionalCondition(String raw) {
        if (raw == null || raw.isBlank() || "ANY".equalsIgnoreCase(raw)) {
            return null;
        }
        return parseCondition(raw);
    }

    private Sort sortOf(String sort) {
        if (sort == null) {
            return Sort.by("createdAt").descending();
        }
        return switch (sort) {
            case "price_asc" -> Sort.by("rate").ascending();
            case "price_desc" -> Sort.by("rate").descending();
            case "popularity" -> Sort.by("borrowCount").descending();
            default -> Sort.by("createdAt").descending();
        };
    }

    private String blank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ListingResponse toResponse(ResourceListing listing, List<ResourceUnit> unitList,
                                       Map<Long, double[]> ratings, Map<Long, Double> owners) {
        double[] rating = ratings.get(listing.getId());
        int available = (int) unitList.stream()
                .filter(unit -> unit.getStatus() == UnitStatus.AVAILABLE && !unit.getCondition().blocksReservation())
                .count();
        return new ListingResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getCategory().getName(),
                listing.getCategory().getId(),
                listing.getPricingType().name(),
                listing.getRate(),
                listing.getListingStatus().name(),
                listing.getOwner().getId(),
                listing.getOwner().getFullName(),
                unitList.stream().map(this::toUnit).toList(),
                listing.getBorrowCount(),
                rating == null ? null : rating[0],
                rating == null ? 0 : (long) rating[1],
                owners.get(listing.getOwner().getId()),
                available);
    }

    private UnitResponse toUnit(ResourceUnit unit) {
        return new UnitResponse(unit.getId(), unit.getAssetCode(), unit.getCondition().name(),
                unit.getStatus().name(), unit.getLocationNote());
    }
}
