package com.caseflow.support.recommendation;

import java.util.UUID;

public class RecommendationNotFoundException extends RuntimeException {

    RecommendationNotFoundException(UUID recommendationId) {
        super("Recommendation %s was not found".formatted(recommendationId));
    }
}
