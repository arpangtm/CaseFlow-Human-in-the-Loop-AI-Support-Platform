package com.caseflow.support.recommendation;

public class RecommendationGenerationException extends RuntimeException {

    RecommendationGenerationException(String detail) {
        super("The recommendation provider returned an invalid structured result: " + detail);
    }
}
