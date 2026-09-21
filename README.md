# CaseFlow

CaseFlow is a human-in-the-loop AI support platform. It classifies incoming cases, retrieves organization knowledge, drafts grounded recommendations, and keeps a human reviewer in control of every customer-facing action.

## Support case intake

The current application provides:

- a Spring Boot REST API for creating, reading, and listing support cases;
- deterministic baseline category and priority assignment;
- PostgreSQL persistence with tenant-safe organization/customer relationships;
- validation, RFC 9457 problem responses, health endpoints, and automated tests;
- a React/TypeScript agent workspace for submitting, searching, filtering, and reviewing cases;
- organization-scoped knowledge article ingestion and ranked PostgreSQL full-text retrieval;
- grounded, structured AI recommendations persisted with citations and model metadata;
- append-only human approve, edit, and reject decisions with reviewer identity and latency;
- tenant-scoped recommendation evaluations, privacy-safe execution logs, and Actuator metrics.

## Run locally

Requirements: Java 21+, Maven 3.6.3+, Docker, and Docker Compose.

```bash
docker compose up -d postgres
cd backend && mvn spring-boot:run
```

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The Vite development server proxies `/api` requests to the backend.

Create a case using the seeded demo organization and customer:

```bash
curl -X POST http://localhost:8080/api/v1/cases \
  -H 'Content-Type: application/json' \
  -d '{
    "organizationId":"00000000-0000-0000-0000-000000000001",
    "customerId":"00000000-0000-0000-0000-000000000002",
    "subject":"Production down",
    "description":"There is a service outage for all users"
  }'
```

List the demo organization's cases:

```bash
curl 'http://localhost:8080/api/v1/cases?organizationId=00000000-0000-0000-0000-000000000001'
```

Add and retrieve knowledge for the demo organization:

```bash
curl -X POST http://localhost:8080/api/v1/knowledge/articles \
  -H 'Content-Type: application/json' \
  -d '{
    "organizationId":"00000000-0000-0000-0000-000000000001",
    "title":"Reset a customer password",
    "content":"Password reset links expire after fifteen minutes.",
    "sourceUrl":"https://docs.example.com/passwords"
  }'

curl 'http://localhost:8080/api/v1/knowledge/search?organizationId=00000000-0000-0000-0000-000000000001&query=password%20reset&limit=5'
```

Generate and list recommendations for a case:

```bash
curl -X POST 'http://localhost:8080/api/v1/cases/{caseId}/recommendations?organizationId=00000000-0000-0000-0000-000000000001'

curl 'http://localhost:8080/api/v1/cases/{caseId}/recommendations?organizationId=00000000-0000-0000-0000-000000000001'
```

Generated recommendations are advisory records with `PENDING_REVIEW` status. These endpoints cannot send a response or resolve a case.

Record and retrieve a human review decision using the seeded demo reviewer:

```bash
curl -X POST http://localhost:8080/api/v1/recommendations/{recommendationId}/review \
  -H 'Content-Type: application/json' \
  -d '{
    "organizationId":"00000000-0000-0000-0000-000000000001",
    "reviewerId":"00000000-0000-0000-0000-000000000003",
    "decision":"EDITED",
    "editedResponse":"Please request a new reset link from Account Settings."
  }'

curl 'http://localhost:8080/api/v1/recommendations/{recommendationId}/review?organizationId=00000000-0000-0000-0000-000000000001'
```

Recording a decision does not send the reviewed response or resolve the support case.

Retrieve the deterministic evaluation created after human review:

```bash
curl 'http://localhost:8080/api/v1/recommendations/{recommendationId}/evaluation?organizationId=00000000-0000-0000-0000-000000000001'

curl 'http://localhost:8080/actuator/metrics/caseflow.ai.reviews'
```

Evaluation records connect model validity, citation use, confidence, latency, and the human outcome to an immutable recommendation version. Diagnostic records contain identifiers and operational metadata, never case text, article text, prompts, or reviewed responses.

## Test

```bash
cd backend
mvn test

cd ../frontend
npm run lint
npm test -- --run
npm run build
```

The PostgreSQL integration test runs when Docker is available and is skipped otherwise.

## Production deployment

The production Compose definition keeps PostgreSQL and the backend on a private network. Only the same-origin frontend gateway is published, bound to loopback by default. Place a TLS-terminating reverse proxy in front of that loopback port for public deployment.

```bash
cp .env.production.example .env.production
# Replace POSTGRES_PASSWORD with a long, unique secret.
docker compose --env-file .env.production -f compose.production.yaml up --build -d
```

The production Spring profile requires explicit database settings, validates Flyway migrations, disables destructive Flyway clean, exposes only health management endpoints, and returns no exception details. The gateway adds browser security headers and proxies only `/api` to the backend. Do not commit `.env.production`; use a secret manager or your deployment platform's encrypted environment settings instead.

CI runs backend tests—including PostgreSQL Testcontainers—and frontend lint, tests, and production build for pull requests and updates to `main`.

## Documentation

- [Architecture](docs/architecture.md)
- [AI engineering](docs/AI_ENGINEERING.md)
