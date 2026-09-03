package com.caseflow.support.review;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

record CreateReviewDecisionRequest(
        @NotNull UUID organizationId,
        @NotNull UUID reviewerId,
        @NotNull HumanReviewDecision decision,
        @Size(max = 20_000) String editedResponse,
        @Size(max = 2000) String rejectionReason
) {
}
