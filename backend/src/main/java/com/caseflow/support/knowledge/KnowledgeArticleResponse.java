package com.caseflow.support.knowledge;

import java.time.Instant;
import java.util.UUID;

record KnowledgeArticleResponse(
        UUID id,
        UUID organizationId,
        String title,
        String sourceUrl,
        String status,
        int chunkCount,
        Instant createdAt,
        Instant updatedAt
) {
    static KnowledgeArticleResponse from(KnowledgeArticle article, int chunkCount) {
        return new KnowledgeArticleResponse(
                article.getId(),
                article.getOrganizationId(),
                article.getTitle(),
                article.getSourceUrl(),
                article.getStatus().name(),
                chunkCount,
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }
}
