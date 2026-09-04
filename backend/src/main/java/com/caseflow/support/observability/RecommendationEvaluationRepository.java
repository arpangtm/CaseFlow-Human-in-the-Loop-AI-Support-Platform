package com.caseflow.support.observability;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface RecommendationEvaluationRepository extends JpaRepository<RecommendationEvaluation, UUID> {

    Optional<RecommendationEvaluation> findByOrganizationIdAndRecommendationId(
            UUID organizationId,
            UUID recommendationId
    );
}
