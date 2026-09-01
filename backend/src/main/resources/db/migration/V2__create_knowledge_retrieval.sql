CREATE TABLE knowledge_articles (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    source_url VARCHAR(2048),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_knowledge_articles_org_id UNIQUE (organization_id, id),
    CONSTRAINT ck_knowledge_articles_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE TABLE knowledge_chunks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    article_id UUID NOT NULL,
    sequence_number INTEGER NOT NULL,
    content TEXT NOT NULL,
    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('english'::regconfig, content)
    ) STORED,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_knowledge_chunks_article_tenant
        FOREIGN KEY (organization_id, article_id)
        REFERENCES knowledge_articles(organization_id, id),
    CONSTRAINT uq_knowledge_chunks_article_sequence UNIQUE (article_id, sequence_number),
    CONSTRAINT ck_knowledge_chunks_sequence CHECK (sequence_number >= 0),
    CONSTRAINT ck_knowledge_chunks_content_length CHECK (char_length(content) <= 1200)
);

CREATE INDEX idx_knowledge_articles_org_updated
    ON knowledge_articles (organization_id, updated_at DESC);

CREATE INDEX idx_knowledge_chunks_org_article
    ON knowledge_chunks (organization_id, article_id);

CREATE INDEX idx_knowledge_chunks_search
    ON knowledge_chunks USING GIN (search_vector);
