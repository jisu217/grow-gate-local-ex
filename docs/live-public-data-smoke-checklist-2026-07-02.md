# Live Public Data Smoke Checklist - 2026-07-02

## Required Server Environment

```bash
PUBLIC_DATA_ENABLED=true
REQUIRE_LIVE_PUBLIC_DATA=true
SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true
KCS_TRADE_API_URL=http://apis.data.go.kr/1220000/Newtrade/getNewtradeList
KCS_TRADE_API_KEY=...
KOTRA_MARKET_API_URL=...
KOTRA_MARKET_API_KEY=...
SAFETY_RECALL_API_URL=...
SAFETY_RECALL_API_KEY=...
KIPRIS_API_URL=...
KIPRIS_API_KEY=...
BIZINFO_API_URL=...
BIZINFO_API_KEY=...
```

## Command

```bash
BASE_URL=https://your-domain.example \
SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true \
SMOKE_CORS_ORIGIN=https://your-domain.example \
./scripts/smoke-prod.sh
```

## Evidence File

The smoke script writes:

```text
/tmp/growgate-smoke/live-public-data-smoke-evidence.json
```

It captures:

- `diagnosisId`
- `collectionRunId`
- `liveCount`
- `cacheCount`
- `fallbackCount`
- `dashboardSuccessCount`
- `dashboardFallbackCount`
- `sourceIds`

If `SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true`, the script fails when no live/cache record or collection run id is captured.

## Presentation Evidence Pack

Capture these before the presentation:

- `live-public-data-smoke-evidence.json`
- diagnosis result first screen with Source Trust Bar
- S10 Sources screen with score contribution ledger
- public data dashboard showing live/cache/fallback ratio
- `/api/diagnosis/{id}/report` Markdown payload proof
