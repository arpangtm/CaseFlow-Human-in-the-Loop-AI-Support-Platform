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

## Human review

Each recommendation accepts one append-only human decision. Approval preserves the generated draft, editing requires the reviewer’s replacement response, and rejection requires a reason while producing no final response. The decision stores reviewer identity and review latency before the recommendation lifecycle is marked reviewed. The reviewed response remains an internal artifact: this workflow has no operation that sends it to a customer or resolves the case.

## Evaluation

Every human decision creates one organization-scoped evaluation tied to that recommendation version and review record. The synchronous evaluator records:

- accepted, edited, or rejected review outcome;
- schema and citation validity established by the pre-persistence validation gate;
- citation count and model confidence;
- provider generation latency and end-to-end review latency;
- normalized token edit distance for approved or edited responses.

Rejections intentionally have no edit-distance value because they produce no final response. Execution logs store provider, model, token, timing, and tenant-safe correlation identifiers without customer or prompt content. Micrometer counters, timers, and distributions expose aggregate operational behavior without tenant IDs in tags. Retrieval relevance labels and eventual case outcomes require later ground-truth workflows; they should be added to the same versioned evaluation boundary rather than inferred from sensitive telemetry.

## Prompt/data hygiene

- Treat case and article text as untrusted data, not instructions.
- Keep secrets and unrelated tenant context out of prompts.
- Redact sensitive values before telemetry export.
- Use bounded context sizes and explicit refusal/escalation states.
- Version prompts and output schemas in source control.

## Production safeguards

Production telemetry must retain the same content boundary as local execution logs: identifiers and bounded operational metadata are allowed, while customer text, knowledge text, drafts, reviewed responses, and full prompts are not. The deployed API remains private behind the same-origin gateway, and management access is limited to health probes. Production configuration must be supplied through deployment secrets rather than committed environment files.
