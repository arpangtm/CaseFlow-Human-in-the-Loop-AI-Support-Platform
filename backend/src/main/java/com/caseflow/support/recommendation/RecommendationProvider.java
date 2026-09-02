package com.caseflow.support.recommendation;

public interface RecommendationProvider {

    ProviderRecommendation generate(RecommendationRequest request);
}
