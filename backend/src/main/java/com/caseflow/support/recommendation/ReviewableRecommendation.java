package com.caseflow.support.recommendation;

import java.time.Instant;
import java.util.UUID;

public record ReviewableRecommendation(
        UUID id,
        UUID organizationId,
        UUID caseId,
        String status,
        String draftResponse,
        double confidence,
        long generationLatencyMs,
        int citationCount,
        Instant createdAt
) {
    static ReviewableRecommendation from(AiRecommendation recommendation, int citationCount) {
        return new ReviewableRecommendation(
                recommendation.getId(),
                recommendation.getOrganizationId(),
                recommendation.getCaseId(),
                recommendation.getStatus().name(),
                recommendation.getDraftResponse(),
                recommendation.getConfidence(),
                recommendation.getLatencyMs(),
                citationCount,
                recommendation.getCreatedAt()
        );
    }
}
