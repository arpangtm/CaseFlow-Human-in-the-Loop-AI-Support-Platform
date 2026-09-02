package com.caseflow.support.recommendation;

import com.caseflow.support.knowledge.KnowledgeEvidence;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
class LocalRecommendationProvider implements RecommendationProvider {

    private static final int MAX_GUIDANCE_CHARS = 600;
    private static final int MAX_CITATIONS = 3;

    @Override
    public ProviderRecommendation generate(RecommendationRequest request) {
        long startedAt = System.nanoTime();
        List<KnowledgeEvidence> evidence = request.evidence();

        String draftResponse;
        RecommendedAction action;
        double confidence;
        boolean escalationRequired;
        List<java.util.UUID> citationIds;
        if (evidence.isEmpty()) {
            draftResponse = "Thanks for contacting support. We could not find verified guidance for this request, "
                    + "so a support specialist needs to review it before we provide next steps.";
            action = RecommendedAction.ESCALATE;
            confidence = 0.2;
            escalationRequired = true;
            citationIds = List.of();
        } else {
            String guidance = truncate(evidence.getFirst().content(), MAX_GUIDANCE_CHARS);
            draftResponse = "Thanks for contacting support. " + guidance;
            action = RecommendedAction.RESPOND_WITH_GUIDANCE;
            confidence = 0.65;
            escalationRequired = false;
            citationIds = evidence.stream()
                    .limit(MAX_CITATIONS)
                    .map(KnowledgeEvidence::chunkId)
                    .toList();
        }

        long latencyMs = Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
        ProviderMetadata metadata = new ProviderMetadata(
                "local",
                "caseflow-grounded-template-v1",
                Map.of("strategy", "deterministic-grounded-template", "maxCitations", MAX_CITATIONS),
                latencyMs,
                null,
                null
        );
        return new ProviderRecommendation(
                draftResponse,
                action,
                confidence,
                escalationRequired,
                citationIds,
                metadata
        );
    }

    private String truncate(String value, int maxChars) {
        if (value.length() <= maxChars) {
            return value;
        }
        int wordBreak = value.lastIndexOf(' ', maxChars - 1);
        int end = wordBreak > maxChars / 2 ? wordBreak : maxChars;
        return value.substring(0, end).stripTrailing() + "…";
    }
}
