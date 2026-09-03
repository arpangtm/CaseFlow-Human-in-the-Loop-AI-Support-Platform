# Architecture

## System context

CaseFlow receives support cases, enriches them with retrieved knowledge, asks an AI provider for a structured recommendation, and pauses for a human decision. Only the reviewed response can become the customer-facing result.

```text
React agent workspace
        |
        | REST/JSON
        v
Spring Boot API ----> PostgreSQL
        |
        +----> retrieval / embeddings provider
        |
        +----> structured AI recommendation provider
        |
        +----> metrics, traces, evaluation events
```

The React workspace is a separate Vite application. During local development it proxies `/api` to Spring Boot; production can serve both applications behind the same origin.

## Backend boundaries

- `casework`: intake, classification, priority, lifecycle, and comments.
- `knowledge`: article management, chunking, embeddings, and retrieval.
- `recommendation`: prompts, model calls, structured outputs, and suggested actions.
- `review`: approve/edit/reject decisions and final-response ownership.
- `observability`: execution logs, evaluation signals, metrics, and traces.

The implementation is a modular monolith. Feature boundaries are kept explicit so asynchronous workers or separate services can be extracted only when load or ownership warrants it.

Knowledge articles are normalized into bounded, overlapping chunks during ingestion. PostgreSQL stores a generated `tsvector` per chunk behind a GIN index, and retrieval applies the organization boundary before ranking matches. The retrieval API returns article and chunk identifiers with each evidence item so downstream recommendations can persist verifiable citations. An embedding-backed retriever can replace or augment this lexical baseline without changing that evidence contract.

Recommendation generation reads a tenant-scoped case, retrieves bounded evidence, and invokes a typed `RecommendationProvider` port outside the persistence transaction. Provider output is validated against the structured contract, including confidence bounds and citation membership, before it is stored. Each recommendation version keeps its draft, suggested action, evidence snapshots, prompt/schema versions, provider/model configuration, latency, and token counts immutable. Only its lifecycle status can move from `PENDING_REVIEW` to `REVIEWED` after a decision is stored; generation and retrieval expose no customer-send operation.

Human review resolves the reviewer and recommendation inside the same organization boundary, validates decision-specific payload rules, and atomically appends one decision while advancing the recommendation lifecycle. Approvals snapshot the unchanged draft, edits snapshot the human response, and rejections store a reason without a final response. The decision records reviewer identity and elapsed review time. No review path sends customer content or changes the support-case resolution state.

## Data model sequence

The schema includes organizations, users, customers, support cases, knowledge articles, retrievable knowledge chunks, versioned AI recommendations, citation snapshots, and human review decisions. Later migrations add comments, execution logs, and performance metrics alongside the feature that owns them.

## Key invariants

1. Organization data is isolated at every query boundary.
2. AI output is advisory and cannot directly transition a case to resolved.
3. Human decisions are append-only audit records.
4. Model, prompt, retrieval, timing, and evaluation metadata remain reproducible.
5. Customer content and credentials are never written to diagnostic logs.
