package com.caseflow.support.recommendation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class JpaRecommendationReviewGateway implements RecommendationReviewGateway {

    private final AiRecommendationRepository repository;
    private final AiRecommendationCitationRepository citationRepository;

    JpaRecommendationReviewGateway(
            AiRecommendationRepository repository,
            AiRecommendationCitationRepository citationRepository
    ) {
        this.repository = repository;
        this.citationRepository = citationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewableRecommendation get(UUID recommendationId, UUID organizationId) {
        return repository.findByIdAndOrganizationId(recommendationId, organizationId)
                .map(recommendation -> ReviewableRecommendation.from(
                        recommendation,
                        citationRepository.countByOrganizationIdAndRecommendationId(organizationId, recommendationId)
                ))
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
