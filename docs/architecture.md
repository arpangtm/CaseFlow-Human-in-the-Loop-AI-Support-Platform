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

The initial implementation is a modular monolith. Feature boundaries are kept explicit so asynchronous workers or separate services can be extracted only when load or ownership warrants it.

## Data model sequence

The initial schema introduces organizations, customers, and support cases. Later migrations add users, comments, articles, recommendations, review decisions, execution logs, and performance metrics alongside the feature that owns them.

## Key invariants

1. Organization data is isolated at every query boundary.
2. AI output is advisory and cannot directly transition a case to resolved.
3. Human decisions are append-only audit records.
4. Model, prompt, retrieval, timing, and evaluation metadata remain reproducible.
5. Customer content and credentials are never written to diagnostic logs.
