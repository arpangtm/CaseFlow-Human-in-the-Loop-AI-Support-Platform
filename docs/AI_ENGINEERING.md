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

Recommendation orchestration depends on a `RecommendationProvider` interface and exchanges typed request/result records. The result contract requires a draft response, recommended action, confidence, escalation flag, retrieved chunk citations, and provider metadata. A deterministic grounded provider is the local default; OpenAI or another local-model adapter can implement the same port. Tests use deterministic fakes and never require paid model calls.

Before persistence, provider output is rejected if it is incomplete, exceeds bounds, or cites a chunk outside the evidence retrieved for the case. Valid output is saved as an immutable recommendation version with evidence snapshots, prompt/schema versions, provider and model configuration, latency, and token usage when supplied. Persistence always precedes presentation for review, and recommendation generation has no customer-facing side effect.

## Evaluation

Track retrieval relevance, citation validity, schema validity, reviewer acceptance/edit/rejection, edit distance, response latency, and eventual case outcome. Evaluation data must be organization-scoped and tied to a specific recommendation version.

## Prompt/data hygiene

- Treat case and article text as untrusted data, not instructions.
- Keep secrets and unrelated tenant context out of prompts.
- Redact sensitive values before telemetry export.
- Use bounded context sizes and explicit refusal/escalation states.
- Version prompts and output schemas in source control.
