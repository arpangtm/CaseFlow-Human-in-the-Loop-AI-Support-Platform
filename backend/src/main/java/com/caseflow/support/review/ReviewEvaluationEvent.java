package com.caseflow.support.review;

import java.time.Instant;
import java.util.UUID;

public record ReviewEvaluationEvent(
        UUID organizationId,
        UUID caseId,
        UUID recommendationId,
        UUID reviewDecisionId,
        HumanReviewDecision decision,
        String draftResponse,
        String finalResponse,
        double confidence,
        long generationLatencyMs,
        long reviewLatencyMs,
        int citationCount,
        Instant reviewedAt
) {
}
