ALTER TABLE human_review_decisions
    ADD CONSTRAINT uq_human_reviews_org_id UNIQUE (organization_id, id);

CREATE TABLE agent_execution_logs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    case_id UUID NOT NULL,
    recommendation_id UUID NOT NULL,
    execution_type VARCHAR(64) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    latency_ms BIGINT NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    model_name VARCHAR(200) NOT NULL,
    input_tokens INTEGER,
    output_tokens INTEGER,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_agent_execution_case_tenant
        FOREIGN KEY (organization_id, case_id)
        REFERENCES support_cases(organization_id, id),
    CONSTRAINT fk_agent_execution_recommendation_tenant
        FOREIGN KEY (organization_id, recommendation_id)
        REFERENCES ai_recommendations(organization_id, id),
    CONSTRAINT ck_agent_execution_type CHECK (execution_type = 'RECOMMENDATION_GENERATION'),
    CONSTRAINT ck_agent_execution_outcome CHECK (outcome = 'SUCCEEDED'),
    CONSTRAINT ck_agent_execution_latency CHECK (latency_ms >= 0),
    CONSTRAINT ck_agent_execution_input_tokens CHECK (input_tokens IS NULL OR input_tokens >= 0),
    CONSTRAINT ck_agent_execution_output_tokens CHECK (output_tokens IS NULL OR output_tokens >= 0),
    CONSTRAINT ck_agent_execution_time CHECK (completed_at >= started_at)
);

CREATE INDEX idx_agent_execution_logs_org_completed
    ON agent_execution_logs (organization_id, completed_at DESC);

CREATE TABLE ai_evaluations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    recommendation_id UUID NOT NULL,
    review_decision_id UUID NOT NULL,
    case_id UUID NOT NULL,
    review_outcome VARCHAR(32) NOT NULL,
    schema_valid BOOLEAN NOT NULL,
    citations_valid BOOLEAN NOT NULL,
    citation_count INTEGER NOT NULL,
    normalized_edit_distance NUMERIC(6, 5),
    confidence NUMERIC(5, 4) NOT NULL,
    generation_latency_ms BIGINT NOT NULL,
    review_latency_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_ai_evaluations_recommendation_tenant
        FOREIGN KEY (organization_id, recommendation_id)
        REFERENCES ai_recommendations(organization_id, id),
    CONSTRAINT fk_ai_evaluations_review_tenant
        FOREIGN KEY (organization_id, review_decision_id)
        REFERENCES human_review_decisions(organization_id, id),
    CONSTRAINT fk_ai_evaluations_case_tenant
        FOREIGN KEY (organization_id, case_id)
        REFERENCES support_cases(organization_id, id),
    CONSTRAINT uq_ai_evaluations_recommendation UNIQUE (recommendation_id),
    CONSTRAINT ck_ai_evaluations_outcome CHECK (review_outcome IN ('ACCEPTED', 'EDITED', 'REJECTED')),
    CONSTRAINT ck_ai_evaluations_citation_count CHECK (citation_count >= 0),
    CONSTRAINT ck_ai_evaluations_edit_distance CHECK (
        (review_outcome = 'ACCEPTED' AND normalized_edit_distance = 0)
        OR (review_outcome = 'EDITED'
            AND normalized_edit_distance IS NOT NULL
            AND normalized_edit_distance >= 0
            AND normalized_edit_distance <= 1)
        OR (review_outcome = 'REJECTED' AND normalized_edit_distance IS NULL)
    ),
    CONSTRAINT ck_ai_evaluations_confidence CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT ck_ai_evaluations_generation_latency CHECK (generation_latency_ms >= 0),
    CONSTRAINT ck_ai_evaluations_review_latency CHECK (review_latency_ms >= 0)
);

CREATE INDEX idx_ai_evaluations_org_created
    ON ai_evaluations (organization_id, created_at DESC);
