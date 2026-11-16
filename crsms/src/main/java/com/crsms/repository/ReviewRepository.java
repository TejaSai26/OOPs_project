package com.crsms.repository;

import com.crsms.model.review.Review;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByBorrowingId(Long borrowingId);

    boolean existsByBorrowingId(Long borrowingId);

    List<Review> findByReviewerIdOrderByCreatedAtDesc(Long reviewerId);

    @Query("""
            SELECT r FROM Review r
            WHERE r.borrowing.unit.listing.id = :listingId
            ORDER BY r.createdAt DESC
            """)
    List<Review> findForListing(@Param("listingId") Long listingId);

    @Query("""
            SELECT r.borrowing.unit.listing.id, AVG(r.rating), COUNT(r.id)
            FROM Review r
            WHERE r.borrowing.unit.listing.id IN :ids
            GROUP BY r.borrowing.unit.listing.id
            """)
    List<Object[]> summarizeForListings(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT r.borrowing.unit.listing.owner.id, AVG(r.rating)
            FROM Review r
            WHERE r.borrowing.unit.listing.owner.id IN :ids
            GROUP BY r.borrowing.unit.listing.owner.id
            """)
    List<Object[]> summarizeForOwners(@Param("ids") Collection<Long> ids);
}
