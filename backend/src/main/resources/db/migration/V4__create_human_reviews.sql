ALTER TABLE ai_recommendations
    DROP CONSTRAINT ck_ai_recommendations_status;

ALTER TABLE ai_recommendations
    ADD CONSTRAINT ck_ai_recommendations_status
        CHECK (status IN ('PENDING_REVIEW', 'REVIEWED')),
    ADD COLUMN lifecycle_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_users_org_email UNIQUE (organization_id, email),
    CONSTRAINT uq_users_org_id UNIQUE (organization_id, id),
    CONSTRAINT ck_users_role CHECK (role IN ('AGENT', 'ADMIN'))
);

CREATE TABLE human_review_decisions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    recommendation_id UUID NOT NULL,
    case_id UUID NOT NULL,
    reviewer_id UUID NOT NULL,
    decision VARCHAR(32) NOT NULL,
    final_response TEXT,
    rejection_reason VARCHAR(2000),
    review_latency_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_human_reviews_recommendation_tenant
        FOREIGN KEY (organization_id, recommendation_id)
        REFERENCES ai_recommendations(organization_id, id),
    CONSTRAINT fk_human_reviews_case_tenant
        FOREIGN KEY (organization_id, case_id)
        REFERENCES support_cases(organization_id, id),
    CONSTRAINT fk_human_reviews_reviewer_tenant
        FOREIGN KEY (organization_id, reviewer_id)
        REFERENCES users(organization_id, id),
    CONSTRAINT uq_human_reviews_recommendation UNIQUE (recommendation_id),
    CONSTRAINT ck_human_reviews_decision CHECK (decision IN ('APPROVED', 'EDITED', 'REJECTED')),
    CONSTRAINT ck_human_reviews_latency CHECK (review_latency_ms >= 0),
    CONSTRAINT ck_human_reviews_payload CHECK (
        (decision IN ('APPROVED', 'EDITED')
            AND final_response IS NOT NULL
            AND char_length(btrim(final_response)) > 0
            AND rejection_reason IS NULL)
        OR
        (decision = 'REJECTED'
            AND final_response IS NULL
            AND rejection_reason IS NOT NULL
            AND char_length(btrim(rejection_reason)) > 0)
    )
);

CREATE INDEX idx_human_reviews_org_created
    ON human_review_decisions (organization_id, created_at DESC);

INSERT INTO users (id, organization_id, email, display_name, role, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    'agent@example.com',
    'Demo Agent',
    'AGENT',
    now()
);
