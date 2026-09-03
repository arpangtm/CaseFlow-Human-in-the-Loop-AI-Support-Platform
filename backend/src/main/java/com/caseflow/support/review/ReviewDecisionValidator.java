package com.caseflow.support.review;

import com.caseflow.support.recommendation.ReviewableRecommendation;
import org.springframework.stereotype.Component;

@Component
class ReviewDecisionValidator {

    ReviewPayload validate(
            CreateReviewDecisionRequest request,
            ReviewableRecommendation recommendation
    ) {
        String editedResponse = normalize(request.editedResponse());
        String rejectionReason = normalize(request.rejectionReason());

        return switch (request.decision()) {
            case APPROVED -> {
                requireAbsent(editedResponse, "An approval cannot include an edited response");
                requireAbsent(rejectionReason, "An approval cannot include a rejection reason");
                yield new ReviewPayload(recommendation.draftResponse(), null);
            }
            case EDITED -> {
                requirePresent(editedResponse, "An edited decision requires a final response");
                requireAbsent(rejectionReason, "An edited decision cannot include a rejection reason");
                yield new ReviewPayload(editedResponse, null);
            }
            case REJECTED -> {
                requireAbsent(editedResponse, "A rejection cannot include an edited response");
                requirePresent(rejectionReason, "A rejection requires a reason");
                yield new ReviewPayload(null, rejectionReason);
            }
        };
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private void requirePresent(String value, String message) {
        if (value == null) {
            throw new InvalidReviewDecisionException(message);
        }
    }

    private void requireAbsent(String value, String message) {
        if (value != null) {
            throw new InvalidReviewDecisionException(message);
        }
    }
}
