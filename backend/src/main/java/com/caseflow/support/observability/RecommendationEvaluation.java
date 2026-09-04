package com.caseflow.support.observability;

import com.caseflow.support.review.ReviewEvaluationEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_evaluations")
class RecommendationEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "recommendation_id", nullable = false)
    private UUID recommendationId;

    @Column(name = "review_decision_id", nullable = false)
    private UUID reviewDecisionId;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_outcome", nullable = false, length = 32)
    private ReviewOutcome reviewOutcome;

    @Column(name = "schema_valid", nullable = false)
    private boolean schemaValid;

    @Column(name = "citations_valid", nullable = false)
    private boolean citationsValid;

    @Column(name = "citation_count", nullable = false)
    private int citationCount;

    @Column(name = "normalized_edit_distance", precision = 6, scale = 5)
    private BigDecimal normalizedEditDistance;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "generation_latency_ms", nullable = false)
    private long generationLatencyMs;

    @Column(name = "review_latency_ms", nullable = false)
    private long reviewLatencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RecommendationEvaluation() {
    }

    RecommendationEvaluation(
            ReviewEvaluationEvent event,
            ReviewOutcome outcome,
            Double normalizedEditDistance
    ) {
        this.organizationId = event.organizationId();
        this.recommendationId = event.recommendationId();
        this.reviewDecisionId = event.reviewDecisionId();
        this.caseId = event.caseId();
        this.reviewOutcome = outcome;
        this.schemaValid = true;
        this.citationsValid = true;
        this.citationCount = event.citationCount();
        this.normalizedEditDistance = normalizedEditDistance == null
                ? null
                : BigDecimal.valueOf(normalizedEditDistance).setScale(5, RoundingMode.HALF_UP);
        this.confidence = BigDecimal.valueOf(event.confidence());
        this.generationLatencyMs = event.generationLatencyMs();
        this.reviewLatencyMs = event.reviewLatencyMs();
        this.createdAt = event.reviewedAt();
    }

    UUID getId() {
        return id;
    }

    UUID getOrganizationId() {
        return organizationId;
    }

    UUID getRecommendationId() {
        return recommendationId;
    }

    UUID getReviewDecisionId() {
        return reviewDecisionId;
    }

    UUID getCaseId() {
        return caseId;
    }

    ReviewOutcome getReviewOutcome() {
        return reviewOutcome;
    }

    boolean isSchemaValid() {
        return schemaValid;
    }

    boolean isCitationsValid() {
        return citationsValid;
    }

    int getCitationCount() {
        return citationCount;
    }

    Double getNormalizedEditDistance() {
        return normalizedEditDistance == null ? null : normalizedEditDistance.doubleValue();
    }

    double getConfidence() {
        return confidence.doubleValue();
    }

    long getGenerationLatencyMs() {
        return generationLatencyMs;
    }

    long getReviewLatencyMs() {
        return reviewLatencyMs;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
