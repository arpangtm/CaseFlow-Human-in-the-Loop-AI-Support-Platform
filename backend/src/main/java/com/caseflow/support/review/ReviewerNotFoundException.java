package com.caseflow.support.review;

import java.util.UUID;

public class ReviewerNotFoundException extends RuntimeException {

    ReviewerNotFoundException(UUID reviewerId) {
        super("Reviewer %s was not found".formatted(reviewerId));
    }
}
