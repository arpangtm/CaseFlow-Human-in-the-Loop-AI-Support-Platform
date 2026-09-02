package com.caseflow.support.recommendation;

import java.util.List;
import java.util.UUID;

public record ProviderRecommendation(
        String draftResponse,
        RecommendedAction recommendedAction,
        double confidence,
        boolean escalationRequired,
        List<UUID> citationChunkIds,
        ProviderMetadata metadata
) {
    public ProviderRecommendation {
        citationChunkIds = citationChunkIds == null ? List.of() : List.copyOf(citationChunkIds);
    }
}
