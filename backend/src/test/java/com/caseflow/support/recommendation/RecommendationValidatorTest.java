package com.caseflow.support.recommendation;

import com.caseflow.support.knowledge.KnowledgeEvidence;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecommendationValidatorTest {

    private final RecommendationValidator validator = new RecommendationValidator();

    @Test
    void rejectsCitationsThatWereNotRetrievedForTheCase() {
        KnowledgeEvidence evidence = evidence(UUID.randomUUID());
        ProviderRecommendation recommendation = recommendation(
                List.of(UUID.randomUUID()),
                RecommendedAction.RESPOND_WITH_GUIDANCE
        );

        assertThatThrownBy(() -> validator.validate(recommendation, List.of(evidence)))
                .isInstanceOf(RecommendationGenerationException.class)
                .hasMessageContaining("citations must reference retrieved evidence");
    }

    @Test
    void rejectsGroundedResponseActionsWithoutACitation() {
        ProviderRecommendation recommendation = recommendation(
                List.of(),
                RecommendedAction.RESPOND_WITH_GUIDANCE
        );

        assertThatThrownBy(() -> validator.validate(recommendation, List.of()))
                .isInstanceOf(RecommendationGenerationException.class)
                .hasMessageContaining("require at least one citation");
    }

    @Test
    void rejectsConfidenceOutsideTheStructuredSchema() {
        ProviderRecommendation recommendation = new ProviderRecommendation(
                "A draft response",
                RecommendedAction.ESCALATE,
                1.2,
                true,
                List.of(),
                metadata()
        );

        assertThatThrownBy(() -> validator.validate(recommendation, List.of()))
                .isInstanceOf(RecommendationGenerationException.class)
                .hasMessageContaining("confidence must be between zero and one");
    }

    private ProviderRecommendation recommendation(
            List<UUID> citationIds,
            RecommendedAction action
    ) {
        return new ProviderRecommendation(
                "A draft response",
                action,
                0.8,
                false,
                citationIds,
                metadata()
        );
    }

    private ProviderMetadata metadata() {
        return new ProviderMetadata("test", "test-model", Map.of("temperature", 0), 10, 25, 12);
    }

    private KnowledgeEvidence evidence(UUID chunkId) {
        return new KnowledgeEvidence(
                UUID.randomUUID(),
                chunkId,
                "Reset a password",
                "Password reset links expire after fifteen minutes.",
                "https://docs.example.com/passwords",
                0.75
        );
    }
}
