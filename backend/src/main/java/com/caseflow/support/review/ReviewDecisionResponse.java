package com.caseflow.support.review;

import java.time.Instant;
import java.util.UUID;

record ReviewDecisionResponse(
        UUID id,
        UUID organizationId,
        UUID recommendationId,
        UUID caseId,
        UUID reviewerId,
        String reviewerName,
        String decision,
        String finalResponse,
        String rejectionReason,
        long reviewLatencyMs,
        Instant createdAt
) {
    static ReviewDecisionResponse from(ReviewDecision review, SupportUser reviewer) {
        return new ReviewDecisionResponse(
                review.getId(),
                review.getOrganizationId(),
                review.getRecommendationId(),
                review.getCaseId(),
                review.getReviewerId(),
                reviewer.getDisplayName(),
                review.getDecision().name(),
                review.getFinalResponse(),
                review.getRejectionReason(),
                review.getReviewLatencyMs(),
                review.getCreatedAt()
        );
    }
}
