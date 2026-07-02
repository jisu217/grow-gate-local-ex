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
PUBLIC_DATA_EXPORT_EVIDENCE_ENABLED=false
PUBLIC_DATA_EXPORT_EVIDENCE_URLS=
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
- `sourcePeriod`
- `liveSuccessRate`
- `providerHealth`
- `dashboardSuccessCount`
- `dashboardFallbackCount`
- `sourceIds`

If `SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true`, the script fails when no live/cache record or collection run id is captured.

## Presentation Evidence Pack

Capture these before the presentation:

- `live-public-data-smoke-evidence.json`
- diagnosis result first screen with Source Trust Bar
- S10 Sources screen with score contribution waterfall ledger and provider failure reasons
- public data dashboard showing live/cache/fallback ratio
- `/api/diagnosis/{id}/report` Markdown payload proof

## Export News/Report Feed

수출 뉴스, 기사, 연구소 보고서 수집은 공식 공공 API score를 대체하지 않는다. `PUBLIC_DATA_EXPORT_EVIDENCE_ENABLED=true`와 allowlist RSS/Atom/JSON URL을 넣으면 `EXPORT_EVIDENCE_FEED` provider가 기존 `public_data_records`에 `sourceType=external_feed`로 저장한다.

권장 운영 방식:

- 검증된 RSS/Atom/JSON endpoint만 `PUBLIC_DATA_EXPORT_EVIDENCE_URLS`에 쉼표로 등록한다.
- allowlist host에 없는 URL은 수집하지 않는다.
- feed contribution은 최대 +3점으로 제한하고 Source Trust Bar에서 live/cache 공공데이터와 별도 count로 표시한다.
- 발표에서는 “뉴스·보고서 최신성 보조 근거”라고 설명하고 “공식 공공데이터 API live 성공” 증거로 말하지 않는다.
