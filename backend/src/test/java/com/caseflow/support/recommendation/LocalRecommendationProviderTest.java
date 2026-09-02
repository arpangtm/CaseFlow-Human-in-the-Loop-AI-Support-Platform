package com.caseflow.support.recommendation;

import com.caseflow.support.casework.CaseCategory;
import com.caseflow.support.casework.CasePriority;
import com.caseflow.support.casework.CaseResponse;
import com.caseflow.support.casework.CaseStatus;
import com.caseflow.support.knowledge.KnowledgeEvidence;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocalRecommendationProviderTest {

    private final LocalRecommendationProvider provider = new LocalRecommendationProvider();

    @Test
    void escalatesWhenNoVerifiedKnowledgeWasRetrieved() {
        ProviderRecommendation result = provider.generate(request(List.of()));

        assertThat(result.recommendedAction()).isEqualTo(RecommendedAction.ESCALATE);
        assertThat(result.escalationRequired()).isTrue();
        assertThat(result.citationChunkIds()).isEmpty();
        assertThat(result.metadata().provider()).isEqualTo("local");
    }

    @Test
    void citesRetrievedEvidenceForGuidedResponses() {
        UUID chunkId = UUID.randomUUID();
        KnowledgeEvidence evidence = new KnowledgeEvidence(
                UUID.randomUUID(),
                chunkId,
                "Reset a password",
                "Password reset links expire after fifteen minutes.",
                "https://docs.example.com/passwords",
                0.7
        );

        ProviderRecommendation result = provider.generate(request(List.of(evidence)));

        assertThat(result.recommendedAction()).isEqualTo(RecommendedAction.RESPOND_WITH_GUIDANCE);
        assertThat(result.escalationRequired()).isFalse();
        assertThat(result.citationChunkIds()).containsExactly(chunkId);
        assertThat(result.draftResponse()).contains("Password reset links expire");
    }

    private RecommendationRequest request(List<KnowledgeEvidence> evidence) {
        Instant now = Instant.parse("2026-09-02T12:00:00Z");
        CaseResponse supportCase = new CaseResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Password reset",
                "The customer cannot sign in.",
                CaseCategory.ACCESS,
                CasePriority.NORMAL,
                CaseStatus.NEW,
                now,
                now
        );
        return new RecommendationRequest(
                supportCase,
                evidence,
                RecommendationService.PROMPT_VERSION,
                RecommendationService.SCHEMA_VERSION
        );
    }
}
