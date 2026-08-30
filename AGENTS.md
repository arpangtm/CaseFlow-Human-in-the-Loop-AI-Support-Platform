# CaseFlow contributor guide

## Product intent

CaseFlow is a human-in-the-loop support platform. AI output is always a recommendation: a human reviewer owns the final customer-facing decision.

## Delivery rules

- Deliver one coherent capability per commit and keep unrelated future scope out of the change.
- Keep the backend organized by feature under `com.caseflow.support`.
- Prefer explicit request/response records at REST boundaries; never expose JPA entities directly.
- Every schema change must be a forward-only Flyway migration.
- Add tests for business rules and at least one integration test for every new API workflow.
- Do not put secrets, customer content, or full prompts in logs.
- AI responses must use structured outputs and be persisted with model/configuration metadata before human review.
- A recommendation can never send a customer reply without a recorded human decision.

## Required checks

Run from the repository root:

```bash
cd backend && mvn test
```

When the frontend exists, also run:

```bash
cd frontend && npm run lint && npm test -- --run && npm run build
```
