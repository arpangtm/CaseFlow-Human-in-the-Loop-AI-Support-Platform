package com.caseflow.support.recommendation;

import java.util.Map;

record RecommendationModelResponse(
        String provider,
        String model,
        Map<String, Object> configuration,
        String promptVersion,
        String schemaVersion,
        long latencyMs,
        Integer inputTokens,
        Integer outputTokens
) {
    static RecommendationModelResponse from(AiRecommendation recommendation) {
        return new RecommendationModelResponse(
                recommendation.getProviderName(),
                recommendation.getModelName(),
                recommendation.getModelConfiguration(),
                recommendation.getPromptVersion(),
                recommendation.getSchemaVersion(),
                recommendation.getLatencyMs(),
                recommendation.getInputTokens(),
                recommendation.getOutputTokens()
        );
    }
}
