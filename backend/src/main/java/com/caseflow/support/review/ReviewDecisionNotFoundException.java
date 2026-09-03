package com.caseflow.support.review;

import java.util.UUID;

public class ReviewDecisionNotFoundException extends RuntimeException {

    ReviewDecisionNotFoundException(UUID recommendationId) {
        super("No human review was found for recommendation %s".formatted(recommendationId));
    }
}
