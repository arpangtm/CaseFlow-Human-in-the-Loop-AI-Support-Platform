package com.caseflow.support.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AiRecommendationRepository extends JpaRepository<AiRecommendation, UUID> {

    @Query("""
            SELECT COALESCE(MAX(recommendation.recommendationVersion), 0)
              FROM AiRecommendation recommendation
             WHERE recommendation.organizationId = :organizationId
               AND recommendation.caseId = :caseId
            """)
    int findMaxVersion(
            @Param("organizationId") UUID organizationId,
            @Param("caseId") UUID caseId
    );

    List<AiRecommendation> findAllByOrganizationIdAndCaseIdOrderByRecommendationVersionDesc(
            UUID organizationId,
            UUID caseId
    );

    Optional<AiRecommendation> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
