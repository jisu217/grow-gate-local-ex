# Production Readiness Checklist

Updated: 2026-07-06

| Area | Check | Status | Evidence |
| --- | --- | --- | --- |
| Docker one-command | `docker compose up --build` starts db/backend/frontend | Ready in config, not runtime-verified here | `docker compose config` passed; Docker daemon unavailable locally |
| Env settings | `.env.example` documents DB, JWT, public-data keys, timeout/retry/circuit | Done | `.env.example` |
| DB migration | MySQL init SQL mounted; root compose uses `JPA_DDL_AUTO=update` only for local demo, while manual/prod readiness is `validate` plus managed SQL | Demo-ready, prod migration tool pending | `docker-compose.yml`, `database/mysql/init`, `docs/database-migration-readiness.md` |
| Public data API key | Blank keys do not fail startup | Done | `/api/public-data/health` returns `apiKeyConfigured=false` and `sourceType=empty` |
| Health check | Operator health returns app/profile/version/database/publicData/checkedAt | Done | `GET /health` |
| Public data health | Provider-level status is available without login | Done | `GET /api/public-data/health` |
| Smoke test | Local smoke saves JSON/Markdown proof | Done | `scripts/smoke-public-data.sh`, `scripts/smoke-local.sh` |
| Log/trace | Correlation ID is response header and log MDC field | Done | `X-Correlation-Id`, `traceId=%X{correlationId}` |
| Fallback proof | live/cache/fallback/empty are distinct | Done | connector sourceType and smoke output |
| Deploy proof | Runtime proof file saved after server is reachable | Pending runtime | `reports/deploy-proof/latest-smoke-result.{json,md}` |
| Timeout | Public-data connect/read timeout documented and configurable | Done | `PUBLIC_DATA_CONNECT_TIMEOUT_MS`, `PUBLIC_DATA_READ_TIMEOUT_MS` |
| Retry | Retry attempts and status policy documented | Done | `PUBLIC_DATA_RETRY_MAX_ATTEMPTS`, `408/429/5xx/timeout` |
| Circuit breaker | MVP process-local circuit policy documented | Done | `PUBLIC_DATA_CIRCUIT_FAILURE_THRESHOLD`, `PUBLIC_DATA_CIRCUIT_OPEN_SECONDS` |
| Secret masking | API key/token/password are not printed raw in logs or smoke | Done | log pattern redaction, `apiKeyMasked` |
| Production instance size | Lightsail production server has enough memory for judging/demo traffic | Done | Static IP moved to `growgate-8gb-20260706`, `large_3_0`, 8GB RAM |
| Production rollback | Previous instance remains available for rollback | Done | `growgate-fresh-20260703-2134` preserved; do not delete without approval |
| Production smoke | Public URL verified after 8GB cutover | Done | `BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/smoke-prod.sh` passed |
| SSH firewall cleanup | Temporary migration SSH allowlist removed | Done | 22/tcp restricted to existing 4 CIDRs plus `lightsail-connect` |

## Before A Judge Demo

1. Copy `.env.example` to `.env`.
2. Fill only real approved API keys and operation endpoints. Leave unknown providers blank.
3. Run `docker compose up --build`.
4. Confirm `curl http://localhost:8080/health`.
5. Confirm `curl http://localhost:8080/api/public-data/health`.
6. Run `./scripts/smoke-local.sh`.
7. Show `reports/deploy-proof/latest-smoke-result.md`.
8. If a provider is blank or still points to a catalog page, explain it as `empty`, not live proof.

## Production Server Additions Still Needed

- Store the generated proof files with deployment timestamp and git SHA.
- Replace demo compose `JPA_DDL_AUTO=update` with pinned Flyway/Liquibase migrations for non-demo production.
- Move circuit breaker state out of process memory if multiple backend instances are used.
- Review the rollback 2GB instance and manual snapshot after 24-48 hours; delete only after explicit approval.
- Continue moving builds to local/CI artifacts instead of building Gradle/Vite on the production server.

## Current Production Reference

| Item | Value |
| --- | --- |
| URL | `https://growgate.54.180.56.24.sslip.io/` |
| Static IP | `54.180.56.24` |
| Current instance | `growgate-8gb-20260706` |
| Rollback instance | `growgate-fresh-20260703-2134` |
| Snapshot | `growgate-pre-8gb-20260706-1611` |
| Deployed commit | `40492eb30e04113801fc353d2b74f571201d1663` |
