package com.caseflow.support.recommendation;

import java.time.Instant;
import java.util.UUID;

public record RecommendationObservation(
        UUID organizationId,
        UUID caseId,
        UUID recommendationId,
        String provider,
        String model,
        long latencyMs,
        Integer inputTokens,
        Integer outputTokens,
        Instant completedAt
) {
}
