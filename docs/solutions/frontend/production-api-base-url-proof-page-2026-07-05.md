---
title: Production API Base URL Guard for Public Data Proof Pages
date: 2026-07-05
category: docs/solutions/frontend
module: frontend/FE-02
problem_type: ui_bug
component: tooling
symptoms:
  - "Production public data page showed the collection-log load error even though backend APIs returned 200."
  - "The deployed frontend bundle contained a local Vite API URL from the build machine."
root_cause: config_error
resolution_type: code_fix
severity: high
tags: [vite, production, api-base-url, public-data, proof-page]
---

# Production API Base URL Guard for Public Data Proof Pages

## Problem

The judge-facing daily public data page can fail in production if a local build embeds `VITE_API_URL=http://localhost:8080`. Backend smoke tests may still pass because the server is healthy, while real browsers try to call their own localhost and show a network error.

## Symptoms

- `/api/public-data/*` endpoints return 200 from curl against production.
- Browser UI shows `수집 로그를 불러오지 못했습니다`.
- Login API works directly, but the deployed browser bundle behaves as if the API is unavailable.
- The built asset contains the local Vite env value from the deploy machine.

## What Didn't Work

- Checking only backend health endpoints was insufficient. It did not catch browser-side base URL resolution.
- Rebuilding without changing frontend API base handling was unsafe because future local `.env` values could be embedded again.

## Solution

`frontend/FE-02/src/services/apiClient.js` resolves the API base at runtime:

- Keep explicit localhost API URLs for local development pages.
- On non-localhost hosts, ignore configured localhost API URLs and use the deployed origin with `/api`.
- Preserve explicit relative `/api` configuration.

`frontend/FE-02/src/pages/publicdata/DailyPublicData.jsx` also uses settled requests so one failing endpoint does not blank the whole proof page when another proof source is available.

`frontend/FE-02/src/pages/auth/Login.jsx` includes a demo account fill action to prevent judge confusion from using the wrong demo password.

## Why This Works

Vite replaces `import.meta.env` values at build time. A local `.env` can therefore leak into a production artifact even when nginx and the backend are correctly configured. Runtime host checks close that gap by treating localhost API URLs as valid only when the current browser host is also local.

The public data page is a proof surface. It should degrade to the latest proof data when a non-critical dashboard request is empty or fails, instead of presenting a generic network failure.

## Prevention

- Frontend mapper tests must cover production-host base URL resolution.
- Production browser smoke must open `/public-data/daily-refresh`, confirm the error banner is absent, and verify proof counts are visible.
- Production browser smoke must use the login UI and reach `/mypage`, not only call `/api/auth/login`.
- Do not trust built JS merely because backend curl smoke passes.

## Verification

Commands run after the fix:

```bash
cd frontend/FE-02
npm run test:mappers
npm run build
BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/smoke-prod.sh
```

Browser verification captured:

- `/private/tmp/growgate-public-data-final2.png`
- `/private/tmp/growgate-login-demo-final2.png`

## Related Issues

- `docs/solutions/frontend/diagnosis-overview-kpi-clarity-2026-07-05.md`
- `docs/solutions/architecture-patterns/source-bound-evidence-agent-workflow-2026-07-05.md`
