package com.caseflow.support.shared;

import com.caseflow.support.casework.CaseNotFoundException;
import com.caseflow.support.recommendation.RecommendationGenerationException;
import com.caseflow.support.recommendation.RecommendationAlreadyReviewedException;
import com.caseflow.support.recommendation.RecommendationNotFoundException;
import com.caseflow.support.observability.EvaluationNotFoundException;
import com.caseflow.support.review.InvalidReviewDecisionException;
import com.caseflow.support.review.ReviewDecisionNotFoundException;
import com.caseflow.support.review.ReviewerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(CaseNotFoundException.class)
    ProblemDetail notFound(CaseNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(RecommendationGenerationException.class)
    ProblemDetail invalidRecommendation(RecommendationGenerationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    @ExceptionHandler({RecommendationNotFoundException.class, ReviewDecisionNotFoundException.class,
            EvaluationNotFoundException.class,
            ReviewerNotFoundException.class})
    ProblemDetail reviewNotFound(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(RecommendationAlreadyReviewedException.class)
    ProblemDetail alreadyReviewed(RecommendationAlreadyReviewedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidReviewDecisionException.class)
    ProblemDetail invalidReview(InvalidReviewDecisionException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
        return problem;
    }
}
