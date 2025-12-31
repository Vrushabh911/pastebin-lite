# Pastebin Lite

Spring Boot Pastebin clone. Passes all automated tests.

## Local Run

mvn clean spring-boot:run

H2 Console: http://localhost:8080/h2-console

## Persistence
- **Local**: H2 in-memory database
- **Production**: PostgreSQL (Railway deployment)

## Endpoints (PDF EXACT)
- `GET /api/healthz` → `{"ok":true}`
- `POST /api/pastes` → `{"id":"uuid","url":"https://domain/pid"}`
- `GET /api/pastes/{id}` → `{"content":"...","remainingViews":4,"expiresAt":"..."}`
- `GET /` → Create UI form
- `GET /{pid}` → HTML paste view

## Testing
Supports `TESTMODE=1` + `x-test-now-ms` header for deterministic TTL testing.
