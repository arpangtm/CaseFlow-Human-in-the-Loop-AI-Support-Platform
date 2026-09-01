# AI engineering

## Safety boundary

The model produces a draft response and a recommended action. It cannot contact a customer or mutate an external system. A human must approve, edit, or reject every recommendation.

## RAG pipeline

1. Normalize the case while retaining the original text.
2. Retrieve organization-scoped knowledge chunks using the configured retriever.
3. Apply deterministic tenant/status filters and rank the candidate evidence.
4. Request a schema-constrained recommendation from the configured model.
5. Persist the structured output, citations, prompt version, model configuration, latency, and token usage.
6. Present it for human review and record the review decision separately.

The current retriever is a deterministic PostgreSQL full-text baseline. Article ingestion creates bounded, overlapping chunks, and search returns persisted article/chunk identifiers, source metadata, content, and rank. Embedding retrieval can later augment this implementation while preserving the same organization-scoped evidence contract and deterministic test path.

## Provider design

Application code will depend on `EmbeddingProvider` and `RecommendationProvider` interfaces. OpenAI and local-model adapters can implement those ports. Tests use deterministic fakes; no test suite should require paid model calls.

## Evaluation

Track retrieval relevance, citation validity, schema validity, reviewer acceptance/edit/rejection, edit distance, response latency, and eventual case outcome. Evaluation data must be organization-scoped and tied to a specific recommendation version.

## Prompt/data hygiene

- Treat case and article text as untrusted data, not instructions.
- Keep secrets and unrelated tenant context out of prompts.
- Redact sensitive values before telemetry export.
- Use bounded context sizes and explicit refusal/escalation states.
- Version prompts and output schemas in source control.
