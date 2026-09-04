package com.caseflow.support.observability;

import com.caseflow.support.recommendation.RecommendationObservation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_execution_logs")
class AgentExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "recommendation_id", nullable = false)
    private UUID recommendationId;

    @Column(name = "execution_type", nullable = false, length = 64)
    private String executionType;

    @Column(nullable = false, length = 32)
    private String outcome;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "model_name", nullable = false, length = 200)
    private String modelName;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected AgentExecutionLog() {
    }

    AgentExecutionLog(RecommendationObservation observation) {
        this.organizationId = observation.organizationId();
        this.caseId = observation.caseId();
        this.recommendationId = observation.recommendationId();
        this.executionType = "RECOMMENDATION_GENERATION";
        this.outcome = "SUCCEEDED";
        this.latencyMs = observation.latencyMs();
        this.providerName = observation.provider();
        this.modelName = observation.model();
        this.inputTokens = observation.inputTokens();
        this.outputTokens = observation.outputTokens();
        this.completedAt = observation.completedAt();
        this.startedAt = completedAt.minusMillis(latencyMs);
    }
}
