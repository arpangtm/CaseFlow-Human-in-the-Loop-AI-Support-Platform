package com.caseflow.support.recommendation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class JpaRecommendationReviewGateway implements RecommendationReviewGateway {

    private final AiRecommendationRepository repository;

    JpaRecommendationReviewGateway(AiRecommendationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewableRecommendation get(UUID recommendationId, UUID organizationId) {
        return repository.findByIdAndOrganizationId(recommendationId, organizationId)
                .map(ReviewableRecommendation::from)
                .orElseThrow(() -> new RecommendationNotFoundException(recommendationId));
    }

    @Override
    @Transactional
    public void markReviewed(UUID recommendationId, UUID organizationId) {
        AiRecommendation recommendation = repository.findByIdAndOrganizationId(recommendationId, organizationId)
                .orElseThrow(() -> new RecommendationNotFoundException(recommendationId));
        recommendation.markReviewed();
    }
}
