package com.caseflow.support.recommendation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record RecommendationResponse(
        UUID id,
        UUID organizationId,
        UUID caseId,
        int version,
        String status,
        String draftResponse,
        String recommendedAction,
        double confidence,
        boolean escalationRequired,
        List<RecommendationCitationResponse> citations,
        RecommendationModelResponse model,
        Instant createdAt
) {
    static RecommendationResponse from(
            AiRecommendation recommendation,
            List<AiRecommendationCitation> citations
    ) {
        return new RecommendationResponse(
                recommendation.getId(),
                recommendation.getOrganizationId(),
                recommendation.getCaseId(),
                recommendation.getRecommendationVersion(),
                recommendation.getStatus().name(),
                recommendation.getDraftResponse(),
                recommendation.getRecommendedAction().name(),
                recommendation.getConfidence(),
                recommendation.isEscalationRequired(),
                citations.stream().map(RecommendationCitationResponse::from).toList(),
                RecommendationModelResponse.from(recommendation),
                recommendation.getCreatedAt()
        );
    }
}
