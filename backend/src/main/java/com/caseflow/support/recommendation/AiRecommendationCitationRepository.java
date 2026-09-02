package com.caseflow.support.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AiRecommendationCitationRepository extends JpaRepository<AiRecommendationCitation, AiRecommendationCitation.CitationId> {

    List<AiRecommendationCitation> findAllByOrganizationIdAndRecommendationIdOrderByPositionAsc(
            UUID organizationId,
            UUID recommendationId
    );
}
