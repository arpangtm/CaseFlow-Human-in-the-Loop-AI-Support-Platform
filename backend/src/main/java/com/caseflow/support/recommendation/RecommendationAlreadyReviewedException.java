package com.caseflow.support.recommendation;

import java.util.UUID;

public class RecommendationAlreadyReviewedException extends RuntimeException {

    public RecommendationAlreadyReviewedException(UUID recommendationId) {
        super("Recommendation %s already has a human review decision".formatted(recommendationId));
    }
}
