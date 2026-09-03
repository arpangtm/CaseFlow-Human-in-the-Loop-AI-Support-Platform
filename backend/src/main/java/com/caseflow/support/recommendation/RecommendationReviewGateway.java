package com.caseflow.support.recommendation;

import java.util.UUID;

public interface RecommendationReviewGateway {

    ReviewableRecommendation get(UUID recommendationId, UUID organizationId);

    void markReviewed(UUID recommendationId, UUID organizationId);
}
