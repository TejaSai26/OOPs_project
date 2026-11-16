package com.crsms.repository;

import com.crsms.model.enums.ListingStatus;
import com.crsms.model.enums.ResourceCondition;
import com.crsms.model.resource.ResourceListing;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceListingRepository extends JpaRepository<ResourceListing, Long> {
    Page<ResourceListing> findByOwnerId(Long ownerId, Pageable pageable);

    long countByListingStatus(ListingStatus status);

    long countByOwnerId(Long ownerId);

    @Query(value = """
            SELECT l FROM ResourceListing l
            WHERE l.listingStatus = com.crsms.model.enums.ListingStatus.ACTIVE
              AND (:categoryId IS NULL OR l.category.id = :categoryId)
              AND (:q IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(l.description) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:minRate IS NULL OR l.rate >= :minRate)
              AND (:maxRate IS NULL OR l.rate <= :maxRate)
              AND (:owner IS NULL OR LOWER(l.owner.fullName) LIKE LOWER(CONCAT('%', :owner, '%')))
              AND (:condition IS NULL OR EXISTS (
                    SELECT u FROM ResourceUnit u WHERE u.listing = l AND u.condition = :condition))
              AND (:availableOnly = false OR EXISTS (
                    SELECT u2 FROM ResourceUnit u2
                    WHERE u2.listing = l
                      AND u2.status = com.crsms.model.enums.UnitStatus.AVAILABLE
                      AND u2.condition <> com.crsms.model.enums.ResourceCondition.DAMAGED
                      AND u2.condition <> com.crsms.model.enums.ResourceCondition.UNDER_MAINTENANCE))
            """,
            countQuery = """
            SELECT COUNT(l) FROM ResourceListing l
            WHERE l.listingStatus = com.crsms.model.enums.ListingStatus.ACTIVE
              AND (:categoryId IS NULL OR l.category.id = :categoryId)
              AND (:q IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(l.description) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:minRate IS NULL OR l.rate >= :minRate)
              AND (:maxRate IS NULL OR l.rate <= :maxRate)
              AND (:owner IS NULL OR LOWER(l.owner.fullName) LIKE LOWER(CONCAT('%', :owner, '%')))
              AND (:condition IS NULL OR EXISTS (
                    SELECT u FROM ResourceUnit u WHERE u.listing = l AND u.condition = :condition))
              AND (:availableOnly = false OR EXISTS (
                    SELECT u2 FROM ResourceUnit u2
                    WHERE u2.listing = l
                      AND u2.status = com.crsms.model.enums.UnitStatus.AVAILABLE
                      AND u2.condition <> com.crsms.model.enums.ResourceCondition.DAMAGED
                      AND u2.condition <> com.crsms.model.enums.ResourceCondition.UNDER_MAINTENANCE))
            """)
    Page<ResourceListing> search(@Param("q") String q,
                                 @Param("categoryId") Long categoryId,
                                 @Param("minRate") BigDecimal minRate,
                                 @Param("maxRate") BigDecimal maxRate,
                                 @Param("owner") String owner,
                                 @Param("condition") ResourceCondition condition,
                                 @Param("availableOnly") boolean availableOnly,
                                 Pageable pageable);
}
