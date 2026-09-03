package com.caseflow.support.recommendation;

import java.time.Instant;
import java.util.UUID;

public record ReviewableRecommendation(
        UUID id,
        UUID organizationId,
        UUID caseId,
        String status,
        String draftResponse,
        Instant createdAt
) {
    static ReviewableRecommendation from(AiRecommendation recommendation) {
        return new ReviewableRecommendation(
                recommendation.getId(),
                recommendation.getOrganizationId(),
                recommendation.getCaseId(),
                recommendation.getStatus().name(),
                recommendation.getDraftResponse(),
                recommendation.getCreatedAt()
        );
    }
}
