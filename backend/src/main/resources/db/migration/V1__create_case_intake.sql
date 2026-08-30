CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_customers_org_email UNIQUE (organization_id, email),
    CONSTRAINT uq_customers_org_id UNIQUE (organization_id, id)
);

CREATE TABLE support_cases (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    customer_id UUID NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description VARCHAR(10000) NOT NULL,
    category VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cases_customer_tenant
        FOREIGN KEY (organization_id, customer_id)
        REFERENCES customers(organization_id, id),
    CONSTRAINT ck_cases_category CHECK (category IN ('ACCESS', 'BILLING', 'TECHNICAL', 'GENERAL')),
    CONSTRAINT ck_cases_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_cases_status CHECK (status IN ('NEW', 'IN_REVIEW', 'RESOLVED', 'CLOSED'))
);

CREATE INDEX idx_cases_org_created_at ON support_cases (organization_id, created_at DESC);

-- Stable demo tenant for local development and the agent workspace.
INSERT INTO organizations (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'CaseFlow Demo');

INSERT INTO customers (id, organization_id, email, display_name)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'customer@example.com',
    'Demo Customer'
);
