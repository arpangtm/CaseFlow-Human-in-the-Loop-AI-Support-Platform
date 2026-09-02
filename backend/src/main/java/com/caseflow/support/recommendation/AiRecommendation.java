package com.caseflow.support.recommendation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendations")
class AiRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "recommendation_version", nullable = false)
    private int recommendationVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecommendationStatus status;

    @Column(name = "draft_response", nullable = false, columnDefinition = "text")
    private String draftResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", nullable = false, length = 64)
    private RecommendedAction recommendedAction;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "escalation_required", nullable = false)
    private boolean escalationRequired;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "model_name", nullable = false, length = 200)
    private String modelName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "model_configuration", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> modelConfiguration;

    @Column(name = "prompt_version", nullable = false, length = 100)
    private String promptVersion;

    @Column(name = "schema_version", nullable = false, length = 100)
    private String schemaVersion;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiRecommendation() {
    }

    AiRecommendation(
            UUID organizationId,
            UUID caseId,
            int recommendationVersion,
            ProviderRecommendation recommendation,
            String promptVersion,
            String schemaVersion,
            Instant createdAt
    ) {
        ProviderMetadata metadata = recommendation.metadata();
        this.organizationId = organizationId;
        this.caseId = caseId;
        this.recommendationVersion = recommendationVersion;
        this.status = RecommendationStatus.PENDING_REVIEW;
        this.draftResponse = recommendation.draftResponse();
        this.recommendedAction = recommendation.recommendedAction();
        this.confidence = BigDecimal.valueOf(recommendation.confidence());
        this.escalationRequired = recommendation.escalationRequired();
        this.providerName = metadata.provider();
        this.modelName = metadata.model();
        this.modelConfiguration = Map.copyOf(metadata.configuration());
        this.promptVersion = promptVersion;
        this.schemaVersion = schemaVersion;
        this.latencyMs = metadata.latencyMs();
        this.inputTokens = metadata.inputTokens();
        this.outputTokens = metadata.outputTokens();
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getOrganizationId() {
        return organizationId;
    }

    UUID getCaseId() {
        return caseId;
    }

    int getRecommendationVersion() {
        return recommendationVersion;
    }

    RecommendationStatus getStatus() {
        return status;
    }

    String getDraftResponse() {
        return draftResponse;
    }

    RecommendedAction getRecommendedAction() {
        return recommendedAction;
    }

    double getConfidence() {
        return confidence.doubleValue();
    }

    boolean isEscalationRequired() {
        return escalationRequired;
    }

    String getProviderName() {
        return providerName;
    }

    String getModelName() {
        return modelName;
    }

    Map<String, Object> getModelConfiguration() {
        return Map.copyOf(modelConfiguration);
    }

    String getPromptVersion() {
        return promptVersion;
    }

    String getSchemaVersion() {
        return schemaVersion;
    }

    long getLatencyMs() {
        return latencyMs;
    }

    Integer getInputTokens() {
        return inputTokens;
    }

    Integer getOutputTokens() {
        return outputTokens;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
