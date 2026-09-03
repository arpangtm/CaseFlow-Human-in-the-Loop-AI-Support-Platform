package com.caseflow.support.review;

public class InvalidReviewDecisionException extends RuntimeException {

    InvalidReviewDecisionException(String detail) {
        super(detail);
    }
}
