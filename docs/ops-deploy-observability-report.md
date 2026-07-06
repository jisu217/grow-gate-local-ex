# Operations, Deploy, Observability Report

Updated: 2026-07-06

## Current Problems

- Root one-command startup was missing. MySQL had `database/docker-compose.yml`, but backend and frontend were not wired into a root production-like compose flow.
- `scripts/verify-local.sh` forced macOS Homebrew OpenJDK 17 through `JAVA_HOME=/opt/homebrew/opt/openjdk@17`.
- Public-data proof existed, but smoke output did not leave a single local `reports/deploy-proof` JSON/Markdown artifact.
- `/health` did not expose enough operator state for profile, version, database, public-data status, and check time.
- Public-data timeout existed in `HttpClientConfig`, but retry/circuit breaker policy was not visible at the endpoint level.
- Logs did not consistently expose a request correlation ID.

## What Changed

- Added root `docker-compose.yml` with `db`, `backend`, and `frontend`.
- Added `backend/BE-02/Dockerfile`, `frontend/FE-02/Dockerfile`, and `frontend/FE-02/nginx.conf`.
- Added root `.env.example` with DB, JWT, public-data timeout/retry/circuit, and provider API key variables.
- Rewrote `scripts/verify-local.sh` to use `JAVA_HOME` only when provided, otherwise check `java -version`, and prefer `./gradlew`.
- Added `scripts/smoke-public-data.sh` and `scripts/smoke-local.sh`.
- Added `CorrelationIdFilter`; responses include `X-Correlation-Id`, and logs include `traceId`.
- Added `/api/public-data/health` configuration status for unauthenticated smoke checks.
- Extended `/health` with `app`, `profile`, `version`, `database`, `publicData`, and `checkedAt`.
- Added public-data connector retry, timeout, circuit breaker state, source type, zero reason, elapsed time, and structured event logging.

## Production Lightsail State

As of 2026-07-06, production runs on an 8GB Lightsail instance created from a manual snapshot. The public URL remains unchanged because the existing Static IP was moved to the new instance.

| Item | Current State |
| --- | --- |
| Production URL | `https://growgate.54.180.56.24.sslip.io/` |
| Static IP | `growgate-static-ip` / `54.180.56.24` |
| Current instance | `growgate-8gb-20260706` |
| Current bundle | `large_3_0`, 8GB RAM, 2 vCPU, 160GB SSD |
| Rollback instance | `growgate-fresh-20260703-2134` |
| Snapshot used | `growgate-pre-8gb-20260706-1611`, state `available` |
| Deployed commit | `40492eb30e04113801fc353d2b74f571201d1663` |

The 2GB instance is intentionally preserved for rollback. Do not delete it or the snapshot without explicit approval. The detailed migration record and rollback command are in [docs/lightsail-8gb-upsize-runbook-2026-07-06.md](lightsail-8gb-upsize-runbook-2026-07-06.md).

## Docker Compose

Run:

```bash
cp .env.example .env
docker compose up --build
```

Open:

- Frontend: `http://localhost:5173`
- Backend health: `http://localhost:8080/health`
- Public-data health: `http://localhost:8080/api/public-data/health`

Default `.env.example` leaves live API keys blank. That is intentional. Providers must report `sourceType=empty`, `apiKeyConfigured=false`, and `zeroReason=API_KEY_NOT_CONFIGURED` or `CATALOG_PAGE_ENDPOINT`; they must not be shown as live proof.

## Smoke And Proof Scripts

```bash
./scripts/smoke-public-data.sh
./scripts/smoke-local.sh
```

Default outputs:

- `reports/deploy-proof/latest-smoke-result.json`
- `reports/deploy-proof/latest-smoke-result.md`

The smoke result records:

- `/health` status, profile, version, database
- `/api/public-data/health` provider status
- `sourceType`: `live`, `cache`, `fallback`, `seed`, `empty`
- `rawCount`, `normalizedCount`, `fallbackUsed`, `zeroReason`
- timeout/retry/circuit policy

## Logs And Trace

Every request gets or preserves an `X-Correlation-Id`. The same value is stored in MDC as `correlationId` and appears in console logs as `traceId=...`.

Public-data provider call logs are JSON messages with:

- `provider`
- `endpointType`
- `sourceType`
- `httpStatus`
- `elapsedMs`
- `rawCount`
- `normalizedCount`
- `fallbackUsed`
- `zeroReason`

Secrets are not logged in raw form. Query parameters matching `serviceKey`, `apiKey`, `key`, `token`, `access_token`, or `client_secret` are redacted, and health/smoke only prints `apiKeyMasked`.

## Timeout, Retry, Circuit Breaker

Defaults are defined in `backend/BE-02/src/main/resources/application.yml`:

- `PUBLIC_DATA_CONNECT_TIMEOUT_MS=3000`
- `PUBLIC_DATA_READ_TIMEOUT_MS=20000`
- `PUBLIC_DATA_RETRY_MAX_ATTEMPTS=2`
- retryable status/errors: `408`, `429`, `5xx`, timeout/resource access failures
- `PUBLIC_DATA_CIRCUIT_FAILURE_THRESHOLD=3`
- `PUBLIC_DATA_CIRCUIT_OPEN_SECONDS=60`

Circuit state is process-local MVP protection. It prevents repeated calls to a provider after consecutive failures, then reopens automatically after the configured window.

## Fallback And Empty Policy

Reason codes:

- `API_KEY_NOT_CONFIGURED`
- `OPERATION_ENDPOINT_NOT_CONFIGURED`
- `CATALOG_PAGE_ENDPOINT`
- `TIMEOUT`
- `HTTP_ERROR`
- `PARSER_ERROR`
- `NORMALIZER_SCHEMA_MISMATCH`
- `EMPTY_LIVE_RESPONSE`
- `RATE_LIMITED`
- `CIRCUIT_OPEN`
- `PUBLIC_DATA_DISABLED`

Policy:

- Missing API keys and catalog/application URLs are `sourceType=empty`, not live and not fallback seed.
- HTTP errors, timeout, parser errors, rate limits, and circuit open are `sourceType=fallback`.
- Empty live responses are `sourceType=empty`.
- Seed/synthetic data must not be marked live.

## Verification Notes

Executed in this environment:

- `bash -n scripts/verify-local.sh`: pass
- `bash -n scripts/smoke-public-data.sh`: pass
- `bash -n scripts/smoke-local.sh`: pass
- `./scripts/check_public_data_providers.py`: pass; blank keys/catalog URLs are reported as `sourceType=empty`
- `docker compose config`: pass
- `npm run test:mappers && npm run build`: pass

Executed against production after 8GB Static IP cutover:

- `curl https://growgate.54.180.56.24.sslip.io/health`: `200`
- `curl https://growgate.54.180.56.24.sslip.io/`: `200`
- `curl https://growgate.54.180.56.24.sslip.io/public-data/daily-refresh`: `200`
- `curl https://growgate.54.180.56.24.sslip.io/diagnosis/57ddd3bd-c8d6-40fd-b25c-978b6f67c8f6`: `200`
- `BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/smoke-prod.sh`: pass
- Smoke output: `stageCards.s04-s07=present`, `publicData.successCount=31`, `fallbackCount=0`, `liveRatio=0.8611`
- Server services after cutover: `nginx`, `growgate-backend`, and `mysql` active; backend local health `200`

Blocked in this environment:

- `docker compose up --build -d`: Docker/Colima daemon socket did not exist at `~/.colima/default/docker.sock`.
- `./scripts/smoke-public-data.sh`: failed at `/health` because backend was not running.
- `./scripts/verify-local.sh`: Java check passed, backend test failed because Gradle could not create its local file-lock socket under the sandbox.
- targeted Gradle test with network completed distribution download once, then later unsandboxed rerun was rejected by policy. No fake backend pass was recorded.

## Remaining Risks

- Real deploy proof still requires running `docker compose up --build` or the production server, then saving `reports/deploy-proof/latest-smoke-result.*`.
- Live provider proof requires real operation endpoints and API keys. Catalog pages and blank keys intentionally stay `empty`.
- Circuit breaker is in-memory MVP state. Multi-instance production should move this to Redis, database, or a gateway-level policy.
- MySQL migrations are still initialized through SQL files plus `JPA_DDL_AUTO=update` in compose. Production should pin migrations through a migration tool before final deployment.
- The previous 2GB Lightsail instance is still running for rollback. Review after 24-48 hours to avoid unnecessary parallel instance cost.
- The 8GB instance reduces operational memory pressure, but server-side Gradle/Vite builds should still be replaced by local/CI artifact deployment where possible.
