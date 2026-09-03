package com.caseflow.support.review;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations/{recommendationId}/review")
class ReviewController {

    private final ReviewService service;

    ReviewController(ReviewService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ReviewDecisionResponse submit(
            @PathVariable UUID recommendationId,
            @Valid @RequestBody CreateReviewDecisionRequest request
    ) {
        return service.submit(recommendationId, request);
    }

    @GetMapping
    ReviewDecisionResponse get(
            @PathVariable UUID recommendationId,
            @RequestParam UUID organizationId
    ) {
        return service.get(recommendationId, organizationId);
    }
}
