# CaseFlow

CaseFlow is a human-in-the-loop AI support platform. It classifies incoming cases, retrieves organization knowledge, drafts grounded recommendations, and keeps a human reviewer in control of every customer-facing action.

## Support case intake

The first vertical slice provides:

- a Spring Boot REST API for creating, reading, and listing support cases;
- deterministic baseline category and priority assignment;
- PostgreSQL persistence with tenant-safe organization/customer relationships;
- validation, RFC 9457 problem responses, health endpoints, and automated tests.

## Run locally

Requirements: Java 21+, Maven 3.6.3+, Docker, and Docker Compose.

```bash
docker compose up -d postgres
cd backend
mvn spring-boot:run
```

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

## Test

```bash
cd backend
mvn test
```

The PostgreSQL integration test runs when Docker is available and is skipped otherwise.

## Documentation

- [Architecture](docs/architecture.md)
- [AI engineering](docs/AI_ENGINEERING.md)
