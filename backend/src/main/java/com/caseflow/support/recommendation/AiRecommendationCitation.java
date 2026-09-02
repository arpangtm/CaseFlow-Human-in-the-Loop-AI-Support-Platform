package com.caseflow.support.recommendation;

import com.caseflow.support.knowledge.KnowledgeEvidence;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendation_citations")
@IdClass(AiRecommendationCitation.CitationId.class)
class AiRecommendationCitation {

    @Id
    @Column(name = "recommendation_id", nullable = false)
    private UUID recommendationId;

    @Id
    @Column(name = "chunk_id", nullable = false)
    private UUID chunkId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    @Column(name = "retrieval_score", nullable = false)
    private double retrievalScore;

    protected AiRecommendationCitation() {
    }

    AiRecommendationCitation(
            UUID recommendationId,
            UUID organizationId,
            int position,
            KnowledgeEvidence evidence
    ) {
        this.recommendationId = recommendationId;
        this.chunkId = evidence.chunkId();
        this.organizationId = organizationId;
        this.articleId = evidence.articleId();
        this.position = position;
        this.title = evidence.title();
        this.content = evidence.content();
        this.sourceUrl = evidence.sourceUrl();
        this.retrievalScore = evidence.score();
    }

    UUID getRecommendationId() {
        return recommendationId;
    }

    UUID getChunkId() {
        return chunkId;
    }

    UUID getArticleId() {
        return articleId;
    }

    int getPosition() {
        return position;
    }

    String getTitle() {
        return title;
    }

    String getContent() {
        return content;
    }

    String getSourceUrl() {
        return sourceUrl;
    }

    double getRetrievalScore() {
        return retrievalScore;
    }

    static class CitationId implements Serializable {
        private UUID recommendationId;
        private UUID chunkId;

        public CitationId() {
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CitationId that)) {
                return false;
            }
            return Objects.equals(recommendationId, that.recommendationId)
                    && Objects.equals(chunkId, that.chunkId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(recommendationId, chunkId);
        }
    }
}
