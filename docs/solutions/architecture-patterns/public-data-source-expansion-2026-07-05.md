# Public Data Source Expansion Pattern - 2026-07-05

## Context

GrowGate uses public data as source-bound evidence, not as autonomous AI truth. New public-data sources must expose `sourceType`, `sourceQualityStatus`, collection evidence, schema evidence, and a conservative `decisionImpact`.

## Applied Sources

- `EXIM_EXCHANGE_RATE`: 한국수출입은행 환율 정보 live API. Uses `EXIM_EXCHANGE_RATE_API_KEY` as `authkey`.
- `BOK_KEY_STATISTICS`: 한국은행 ECOS 100대 통계지표 live API. Uses `BOK_ECOS_API_KEY` in the path.
- `KATS_CERTIFIED_PRODUCT_INFO`: KATS 인증제품 정보 CSV-first cache, 152,433 rows.
- `KOREA_LAW_STATUTES`: 국가법령정보 현행법령 목록 live API. Uses `LAW_OPENAPI_OC` as `OC`.
- `KOTRA_OVERSEAS_EXHIBITIONS`: KOTRA 해외전시회 정보 CSV-first cache, 105 rows.
- `CONSUMER_RECALL_SANITATION`, `CONSUMER_RECALL_CHEMICAL`, `CONSUMER_RECALL_OVERSEAS`: Consumer24 recall API menu IDs `0208`, `0403`, `0501`.

## Rules

- Never commit real API keys. Code and docs use env var names only.
- CSV-first providers read local cache quickly and call odcloud only when update is due and an operation endpoint plus key are configured.
- Large CSV providers must not store full row data in `rawPayload`; store row count and samples.
- Domestic law API strengthens Korean law/certification explanation only. It is not an EU regulation substitute.
- Recall APIs can lower confidence or add risk context, but cannot prove “no recall” or product safety.
- `fallback`, `empty_not_configured`, `seed`, and `heuristic` records cannot provide positive contribution.

## Verification

- Backend: `./gradlew test`
- Static provider proof: `python3 scripts/check_public_data_providers.py --json`
- Runtime proof: create a public-data proof run and verify `operationEndpoint`, `apiKeyConfigured`, `rawResponseSha256`, and `schemaValidationStatus`.
