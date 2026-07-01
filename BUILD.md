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

## Production Smoke Test

```bash
BASE_URL=http://3.36.60.121 ./scripts/smoke-prod.sh
```

The smoke script checks health, unauthenticated 401, signup/login, `/api/users/me`, product create, diagnosis create/polling, sources, PDF report download, and public-data run counts.
