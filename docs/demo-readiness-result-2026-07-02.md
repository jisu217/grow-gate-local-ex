# Demo Readiness Result

Date: 2026-07-02 KST

## Local Verification

Command:

```bash
./scripts/verify-local.sh
```

Result: passed.

- Backend tests: passed
- Risk gates: passed
- Frontend production build: passed

## Deployed Endpoint Check

Current reachable HTTPS endpoint:

```text
https://growgate.54.180.56.24.sslip.io
```

Checks:

- `/health`: `200`
- `/`: `200`

The old IP endpoint `http://3.36.60.121` did not accept HTTP/HTTPS connections during this check.

## Demo Data Attempt

Command:

```bash
BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/setup-demo-scenario.sh
```

Created on deployed server:

- Demo account: `demo-pmgrow@growgate.local`
- Product: `PMGrow 리튬이온 배터리팩 20260702-0040`
- Product ID: `c72901d0-e671-4c8a-9323-78ef93f09aa0`
- Diagnosis ID: `1ec0d289-8784-4bbd-86c7-25744cdd2050`
- Diagnosis status: `done`

Observed deployed-server gaps:

- Login response did not set `gg_access_token` HttpOnly cookie. It returned only `accessToken`, so the server appears older than the latest local code.
- Public data collection recorded `PUBLIC_DATA_ENABLED=false` for all five providers.
- Diagnosis response did not include backend `stageCards.s04`, `s05`, `s06`, `s07`, or `knowledgeLoop`.
- `agentTrace.mode` was `spring-local-orchestrator`, not the latest `agent-loop-planning-tool-verification-retry` flow.

Conclusion: the deployed service is reachable and can run the older diagnosis flow, but it is not yet running the latest presentation-hardening code. For the final demonstration, deploy the current branch to Lightsail, set `PUBLIC_DATA_ENABLED=true`, configure provider operation endpoints/API keys where available, then rerun:

```bash
BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/setup-demo-scenario.sh
./scripts/smoke-prod.sh
```

Use the stricter live-public-data gate only after provider keys/endpoints are configured:

```bash
DEMO_REQUIRE_LIVE_PUBLIC_DATA=true \
BASE_URL=https://growgate.54.180.56.24.sslip.io \
./scripts/setup-demo-scenario.sh
```
