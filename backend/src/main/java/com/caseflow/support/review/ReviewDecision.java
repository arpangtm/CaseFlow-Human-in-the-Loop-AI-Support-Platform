package com.caseflow.support.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "human_review_decisions")
class ReviewDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "recommendation_id", nullable = false)
    private UUID recommendationId;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private HumanReviewDecision decision;

    @Column(name = "final_response", columnDefinition = "text")
    private String finalResponse;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "review_latency_ms", nullable = false)
    private long reviewLatencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReviewDecision() {
    }

    ReviewDecision(
            UUID organizationId,
            UUID recommendationId,
            UUID caseId,
            UUID reviewerId,
            HumanReviewDecision decision,
            ReviewPayload payload,
            long reviewLatencyMs,
            Instant createdAt
    ) {
        this.organizationId = organizationId;
        this.recommendationId = recommendationId;
        this.caseId = caseId;
        this.reviewerId = reviewerId;
        this.decision = decision;
        this.finalResponse = payload.finalResponse();
        this.rejectionReason = payload.rejectionReason();
        this.reviewLatencyMs = reviewLatencyMs;
        this.createdAt = createdAt;
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

    UUID getCaseId() {
        return caseId;
    }

    UUID getReviewerId() {
        return reviewerId;
    }

    HumanReviewDecision getDecision() {
        return decision;
    }

    String getFinalResponse() {
        return finalResponse;
    }

    String getRejectionReason() {
        return rejectionReason;
    }

    long getReviewLatencyMs() {
        return reviewLatencyMs;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
