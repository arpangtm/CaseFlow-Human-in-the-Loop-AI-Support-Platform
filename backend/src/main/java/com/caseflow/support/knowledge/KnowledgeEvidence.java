package com.caseflow.support.knowledge;

import java.util.UUID;

record KnowledgeEvidence(
        UUID articleId,
        UUID chunkId,
        String title,
        String content,
        String sourceUrl,
        double score
) {
    static KnowledgeEvidence from(KnowledgeSearchRow row) {
        return new KnowledgeEvidence(
                row.getArticleId(),
                row.getChunkId(),
                row.getTitle(),
                row.getContent(),
                row.getSourceUrl(),
                row.getScore()
        );
    }
}
