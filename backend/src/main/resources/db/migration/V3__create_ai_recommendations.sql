ALTER TABLE support_cases
    ADD CONSTRAINT uq_support_cases_org_id UNIQUE (organization_id, id);

ALTER TABLE knowledge_chunks
    ADD CONSTRAINT uq_knowledge_chunks_org_id UNIQUE (organization_id, id);

CREATE TABLE ai_recommendations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    case_id UUID NOT NULL,
    recommendation_version INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    draft_response TEXT NOT NULL,
    recommended_action VARCHAR(64) NOT NULL,
    confidence NUMERIC(5, 4) NOT NULL,
    escalation_required BOOLEAN NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    model_name VARCHAR(200) NOT NULL,
    model_configuration JSONB NOT NULL,
    prompt_version VARCHAR(100) NOT NULL,
    schema_version VARCHAR(100) NOT NULL,
    latency_ms BIGINT NOT NULL,
    input_tokens INTEGER,
    output_tokens INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_ai_recommendations_case_tenant
        FOREIGN KEY (organization_id, case_id)
        REFERENCES support_cases(organization_id, id),
    CONSTRAINT uq_ai_recommendations_org_id UNIQUE (organization_id, id),
    CONSTRAINT uq_ai_recommendations_case_version
        UNIQUE (organization_id, case_id, recommendation_version),
    CONSTRAINT ck_ai_recommendations_version CHECK (recommendation_version > 0),
    CONSTRAINT ck_ai_recommendations_status CHECK (status = 'PENDING_REVIEW'),
    CONSTRAINT ck_ai_recommendations_action CHECK (
        recommended_action IN ('RESPOND_WITH_GUIDANCE', 'REQUEST_INFORMATION', 'ESCALATE', 'NO_ACTION')
    ),
    CONSTRAINT ck_ai_recommendations_confidence CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT ck_ai_recommendations_latency CHECK (latency_ms >= 0),
    CONSTRAINT ck_ai_recommendations_input_tokens CHECK (input_tokens IS NULL OR input_tokens >= 0),
    CONSTRAINT ck_ai_recommendations_output_tokens CHECK (output_tokens IS NULL OR output_tokens >= 0)
);

CREATE TABLE ai_recommendation_citations (
    recommendation_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    chunk_id UUID NOT NULL,
    article_id UUID NOT NULL,
    position INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    source_url VARCHAR(2048),
    retrieval_score DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (recommendation_id, chunk_id),
    CONSTRAINT fk_ai_citations_recommendation_tenant
        FOREIGN KEY (organization_id, recommendation_id)
        REFERENCES ai_recommendations(organization_id, id),
    CONSTRAINT fk_ai_citations_chunk_tenant
        FOREIGN KEY (organization_id, chunk_id)
        REFERENCES knowledge_chunks(organization_id, id),
    CONSTRAINT uq_ai_citations_position UNIQUE (recommendation_id, position),
    CONSTRAINT ck_ai_citations_position CHECK (position >= 0),
    CONSTRAINT ck_ai_citations_score CHECK (retrieval_score >= 0)
);

CREATE INDEX idx_ai_recommendations_case_created
    ON ai_recommendations (organization_id, case_id, created_at DESC);
