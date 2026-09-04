package com.caseflow.support.review;

import com.caseflow.support.recommendation.ReviewableRecommendation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewDecisionValidatorTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID REVIEWER_ID = UUID.randomUUID();

    private final ReviewDecisionValidator validator = new ReviewDecisionValidator();

    @Test
    void approvalUsesTheUnchangedRecommendationDraft() {
        ReviewPayload payload = validator.validate(
                request(HumanReviewDecision.APPROVED, null, null),
                recommendation()
        );

        assertThat(payload.finalResponse()).isEqualTo("Original AI draft");
        assertThat(payload.rejectionReason()).isNull();
    }

    @Test
    void editRequiresAndNormalizesTheHumanResponse() {
        ReviewPayload payload = validator.validate(
                request(HumanReviewDecision.EDITED, "  Human-edited response  ", null),
                recommendation()
        );

        assertThat(payload.finalResponse()).isEqualTo("Human-edited response");
        assertThat(payload.rejectionReason()).isNull();
    }

    @Test
    void rejectionRequiresAReasonAndNeverCreatesAFinalResponse() {
        ReviewPayload payload = validator.validate(
                request(HumanReviewDecision.REJECTED, null, "  Guidance is not applicable  "),
                recommendation()
        );

        assertThat(payload.finalResponse()).isNull();
        assertThat(payload.rejectionReason()).isEqualTo("Guidance is not applicable");
    }

    @Test
    void rejectsAmbiguousDecisionPayloads() {
        assertThatThrownBy(() -> validator.validate(
                request(HumanReviewDecision.APPROVED, "Changed response", null),
                recommendation()
        )).isInstanceOf(InvalidReviewDecisionException.class)
                .hasMessageContaining("approval cannot include an edited response");

        assertThatThrownBy(() -> validator.validate(
                request(HumanReviewDecision.REJECTED, null, " "),
                recommendation()
        )).isInstanceOf(InvalidReviewDecisionException.class)
                .hasMessageContaining("rejection requires a reason");
    }

    private CreateReviewDecisionRequest request(
            HumanReviewDecision decision,
            String editedResponse,
            String rejectionReason
    ) {
        return new CreateReviewDecisionRequest(
                ORGANIZATION_ID,
                REVIEWER_ID,
                decision,
                editedResponse,
                rejectionReason
        );
    }

    private ReviewableRecommendation recommendation() {
        return new ReviewableRecommendation(
                UUID.randomUUID(),
                ORGANIZATION_ID,
                UUID.randomUUID(),
                "PENDING_REVIEW",
                "Original AI draft",
                0.8,
                12,
                1,
                Instant.parse("2026-09-03T12:00:00Z")
        );
    }
}
