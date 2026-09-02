package com.caseflow.support.recommendation;

import java.util.UUID;

record RecommendationCitationResponse(
        UUID articleId,
        UUID chunkId,
        int position,
        String title,
        String content,
        String sourceUrl,
        double retrievalScore
) {
    static RecommendationCitationResponse from(AiRecommendationCitation citation) {
        return new RecommendationCitationResponse(
                citation.getArticleId(),
                citation.getChunkId(),
                citation.getPosition(),
                citation.getTitle(),
                citation.getContent(),
                citation.getSourceUrl(),
                citation.getRetrievalScore()
        );
    }
}
