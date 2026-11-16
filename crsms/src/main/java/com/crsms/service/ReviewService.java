package com.crsms.service;

import com.crsms.dto.ApiDtos.ReviewPage;
import com.crsms.dto.ApiDtos.ReviewRequest;
import com.crsms.dto.ApiDtos.ReviewResponse;
import com.crsms.event.DomainEventPublisher;
import com.crsms.exception.ApiException;
import com.crsms.model.borrowing.BorrowingRecord;
import com.crsms.model.enums.BorrowingStatus;
import com.crsms.model.review.Review;
import com.crsms.model.user.User;
import com.crsms.repository.BorrowingRecordRepository;
import com.crsms.repository.ResourceListingRepository;
import com.crsms.repository.ReviewRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final BorrowingRecordRepository borrowings;
    private final ResourceListingRepository listings;
    private final DomainEventPublisher events;

    public ReviewService(ReviewRepository reviews, BorrowingRecordRepository borrowings,
                         ResourceListingRepository listings, DomainEventPublisher events) {
        this.reviews = reviews;
        this.borrowings = borrowings;
        this.listings = listings;
        this.events = events;
    }

    @Transactional
    public ReviewResponse create(User reviewer, ReviewRequest request) {
        BorrowingRecord borrowing = borrowings.findById(request.borrowingId())
                .orElseThrow(() -> new ApiException(404, "Borrowing not found"));
        if (!borrowing.getBorrower().getId().equals(reviewer.getId())) {
            throw new ApiException(403, "Only the borrower can review this borrowing");
        }
        // A review is accepted only after a real pickup and return, so ratings cannot be posted from a bare reservation.
        if (borrowing.getStatus() != BorrowingStatus.RETURNED) {
            throw new ApiException(400, "Reviews require a completed borrowing");
        }
        if (reviews.existsByBorrowingId(borrowing.getId())) {
            throw new ApiException(409, "This borrowing already has a review");
        }
        Review review = reviews.save(new Review(borrowing, reviewer, request.rating(), request.comment()));
        events.publish(reviewer.getId(), borrowing.getUnit().getListing().getOwner().getId(), null, null, null,
                "REVIEW_CREATED", "Review", review.getId(),
                reviewer.getFullName() + " rated " + borrowing.getUnit().getListing().getTitle()
                        + " " + review.getRating() + "/5");
        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public ReviewPage forListing(Long listingId) {
        if (!listings.existsById(listingId)) {
            throw new ApiException(404, "Listing not found");
        }
        List<ReviewResponse> rows = reviews.findForListing(listingId).stream().map(this::toResponse).toList();
        double average = rows.stream().mapToInt(ReviewResponse::rating).average().orElse(Double.NaN);
        return new ReviewPage(rows, rows.isEmpty() ? null : average, rows.size());
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> mine(User reviewer) {
        return reviews.findByReviewerIdOrderByCreatedAtDesc(reviewer.getId()).stream().map(this::toResponse).toList();
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getRating(),
                review.getComment(),
                review.getReviewer().getFullName(),
                review.getCreatedAt(),
                review.getBorrowing().getUnit().getListing().getTitle(),
                review.getBorrowing().getId());
    }
}
