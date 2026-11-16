package com.crsms.controller;

import com.crsms.dto.ApiDtos.ReviewPage;
import com.crsms.dto.ApiDtos.ReviewRequest;
import com.crsms.dto.ApiDtos.ReviewResponse;
import com.crsms.service.CurrentUserService;
import com.crsms.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviews;
    private final CurrentUserService current;

    public ReviewController(ReviewService reviews, CurrentUserService current) {
        this.reviews = reviews;
        this.current = current;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@Valid @RequestBody ReviewRequest request) {
        return reviews.create(current.require(), request);
    }

    @GetMapping("/my")
    public List<ReviewResponse> mine() {
        return reviews.mine(current.require());
    }

    @GetMapping("/listing/{listingId}")
    public ReviewPage listing(@PathVariable Long listingId) {
        return reviews.forListing(listingId);
    }
}
