package com.caseflow.support.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ReviewDecisionRepository extends JpaRepository<ReviewDecision, UUID> {

    boolean existsByOrganizationIdAndRecommendationId(UUID organizationId, UUID recommendationId);

    Optional<ReviewDecision> findByOrganizationIdAndRecommendationId(
            UUID organizationId,
            UUID recommendationId
    );
}
