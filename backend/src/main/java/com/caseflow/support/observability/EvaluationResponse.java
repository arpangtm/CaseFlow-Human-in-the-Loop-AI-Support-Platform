package com.caseflow.support.observability;

import java.time.Instant;
import java.util.UUID;

record EvaluationResponse(
        UUID id,
        UUID organizationId,
        UUID recommendationId,
        UUID reviewDecisionId,
        UUID caseId,
        String reviewOutcome,
        boolean schemaValid,
        boolean citationsValid,
        int citationCount,
        Double normalizedEditDistance,
        double confidence,
        long generationLatencyMs,
        long reviewLatencyMs,
        Instant createdAt
) {
    static EvaluationResponse from(RecommendationEvaluation evaluation) {
        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getOrganizationId(),
                evaluation.getRecommendationId(),
                evaluation.getReviewDecisionId(),
                evaluation.getCaseId(),
                evaluation.getReviewOutcome().name(),
                evaluation.isSchemaValid(),
                evaluation.isCitationsValid(),
                evaluation.getCitationCount(),
                evaluation.getNormalizedEditDistance(),
                evaluation.getConfidence(),
                evaluation.getGenerationLatencyMs(),
                evaluation.getReviewLatencyMs(),
                evaluation.getCreatedAt()
        );
    }
}
