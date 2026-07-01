# Build and Verification

## Prerequisites

- Java 17
- Node.js 20
- npm

## Backend

```bash
cd backend/BE-02
./gradlew test --no-daemon
```

## Frontend

```bash
cd frontend/FE-02
npm ci
npm run build
```

## One-command Local Verification

```bash
./scripts/verify-local.sh
```

`verify-local.sh` now also runs `scripts/check-risk-gates.sh`. The risk gate fails fast when a change reintroduces one of the presentation risks:

- public data disabled by default
- S04-S07 frontend mock stage data wired into the diagnosis service
- missing report download error handling
- access token persistence in localStorage
- public-data global run/record queries
- production smoke test missing CORS, cookie auth, stage card, or public-data checks

## Production Smoke Test

```bash
BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/smoke-prod.sh
```

The smoke script checks health, unauthenticated 401, signup/login, `/api/users/me`, product create, diagnosis create/polling, sources, PDF report download, and public-data run counts.

Optional stricter checks:

```bash
SMOKE_CORS_ORIGIN=https://your-domain.example \
SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true \
BASE_URL=https://your-domain.example \
./scripts/smoke-prod.sh
```

- `SMOKE_CORS_ORIGIN` verifies the deployed API CORS preflight headers for browser API calls.
- `SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true` fails if the run records no live public-data success. Use it only on the presentation server after API keys and operation endpoints are configured.

## Demo Scenario Setup

```bash
BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/setup-demo-scenario.sh
```

This creates a complete presentation scenario through the deployed API: signup/login, product creation, manager-verified knowledge, certifications, FTO, buyer requirements, source documents, public-data collection, diagnosis polling, source trace retrieval, PDF download, and one approved self-loop knowledge item.

Use HTTPS after the domain is attached:

```bash
BASE_URL=https://your-domain.example ./scripts/setup-demo-scenario.sh
```

Require at least one live public-data success on a fully configured presentation server:

```bash
DEMO_REQUIRE_LIVE_PUBLIC_DATA=true BASE_URL=https://your-domain.example ./scripts/setup-demo-scenario.sh
```
