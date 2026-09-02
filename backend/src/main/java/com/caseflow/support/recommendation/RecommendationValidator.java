package com.caseflow.support.recommendation;

import com.caseflow.support.knowledge.KnowledgeEvidence;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
class RecommendationValidator {

    private static final int MAX_DRAFT_CHARS = 20_000;

    void validate(ProviderRecommendation recommendation, List<KnowledgeEvidence> evidence) {
        if (recommendation == null) {
            throw invalid("result is missing");
        }
        if (recommendation.draftResponse() == null || recommendation.draftResponse().isBlank()) {
            throw invalid("draft response is missing");
        }
        if (recommendation.draftResponse().length() > MAX_DRAFT_CHARS) {
            throw invalid("draft response exceeds the configured limit");
        }
        if (recommendation.recommendedAction() == null) {
            throw invalid("recommended action is missing");
        }
        if (!Double.isFinite(recommendation.confidence())
                || recommendation.confidence() < 0
                || recommendation.confidence() > 1) {
            throw invalid("confidence must be between zero and one");
        }
        validateMetadata(recommendation.metadata());
        validateCitations(recommendation, evidence);
    }

    private void validateMetadata(ProviderMetadata metadata) {
        if (metadata == null || metadata.provider() == null || metadata.provider().isBlank()) {
            throw invalid("provider metadata is missing");
        }
        if (metadata.model() == null || metadata.model().isBlank()) {
            throw invalid("model metadata is missing");
        }
        if (metadata.latencyMs() < 0) {
            throw invalid("latency cannot be negative");
        }
        if (metadata.inputTokens() != null && metadata.inputTokens() < 0) {
            throw invalid("input token count cannot be negative");
        }
        if (metadata.outputTokens() != null && metadata.outputTokens() < 0) {
            throw invalid("output token count cannot be negative");
        }
    }

    private void validateCitations(
            ProviderRecommendation recommendation,
            List<KnowledgeEvidence> evidence
    ) {
        List<UUID> citationIds = recommendation.citationChunkIds();
        Set<UUID> uniqueCitationIds = new HashSet<>(citationIds);
        if (uniqueCitationIds.size() != citationIds.size()) {
            throw invalid("citations must be unique");
        }

        Set<UUID> retrievedChunkIds = evidence.stream()
                .map(KnowledgeEvidence::chunkId)
                .collect(java.util.stream.Collectors.toSet());
        if (!retrievedChunkIds.containsAll(uniqueCitationIds)) {
            throw invalid("citations must reference retrieved evidence");
        }
        if (recommendation.recommendedAction() == RecommendedAction.RESPOND_WITH_GUIDANCE
                && citationIds.isEmpty()) {
            throw invalid("guided responses require at least one citation");
        }
    }

    private RecommendationGenerationException invalid(String detail) {
        return new RecommendationGenerationException(detail);
    }
}
