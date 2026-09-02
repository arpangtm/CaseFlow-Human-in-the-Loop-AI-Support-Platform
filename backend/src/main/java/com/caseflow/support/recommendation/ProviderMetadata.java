package com.caseflow.support.recommendation;

import java.util.Map;

public record ProviderMetadata(
        String provider,
        String model,
        Map<String, Object> configuration,
        long latencyMs,
        Integer inputTokens,
        Integer outputTokens
) {
    public ProviderMetadata {
        configuration = configuration == null ? Map.of() : Map.copyOf(configuration);
    }
}
