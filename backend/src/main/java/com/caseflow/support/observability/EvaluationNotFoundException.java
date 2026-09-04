package com.caseflow.support.observability;

import java.util.UUID;

public class EvaluationNotFoundException extends RuntimeException {

    public EvaluationNotFoundException(UUID recommendationId) {
        super("Evaluation not found for recommendation " + recommendationId);
    }
}
