# SAFETY_RECALL KATS Domestic Recall Proof

## Why This Replaces The Old Provider

`SAFETY_RECALL` previously pointed at the SafetyKorea public guide page, so production proof rows showed `source=fallback`, `endpoint no`, `key missing`, and `schemaValidation=SKIPPED`.

The runtime provider now uses `산업통상부_국가기술표준원_제품안전_국내리콜정보` as the recall dimension source:

- official page: `https://www.data.go.kr/data/15040696/fileData.do`
- Swagger/OAS: `https://infuser.odcloud.kr/oas/docs?namespace=15040696/v1`
- default operation endpoint: `https://api.odcloud.kr/api/15040696/v1/uddi:f13d1404-717d-4862-871d-b249899ea58f`

## Environment

Preferred:

```bash
MOTIE_SAFETY_RECALL_API_KEY=...
MOTIE_SAFETY_RECALL_OPERATION_ENDPOINT=https://api.odcloud.kr/api/15040696/v1/uddi:f13d1404-717d-4862-871d-b249899ea58f
MOTIE_SAFETY_RECALL_CSV_RESOURCE=classpath:publicdata/kats-domestic-recall-20230809.csv
MOTIE_SAFETY_RECALL_CACHE_ROOT=data/cache/safety_recall
```

Backward-compatible:

```bash
SAFETY_RECALL_API_KEY=...
SAFETY_RECALL_API_URL=https://api.odcloud.kr/api/15040696/v1/uddi:f13d1404-717d-4862-871d-b249899ea58f
DATAGO_SERVICE_KEY=...
```

Secrets stay in env only. Do not commit keys.

## Cache Modes

| Mode | `sourceType` | Proof meaning |
| --- | --- | --- |
| `live` | `live` | API key and operation endpoint were configured and the API call returned rows. |
| `cache-from-live` | `cache` | A previous successful API call wrote `data/cache/safety_recall/YYYY-MM-DD/raw_response.json`, `safety_recall_normalized.csv`, and `metadata.json`. The UI labels this as `API 수집 캐시`. |
| `csv snapshot` | `cache` | The packaged KATS CSV is used as a conservative cache safety net. This is not fallback and should not be displayed as seed data. |
| `fallback` | `fallback` | API failed and no CSV/API-derived cache was available. |

## Bootstrap

After configuring the key on a running backend:

```bash
BASE_URL=https://growgate.54.180.56.24.sslip.io \
GROWGATE_BOOTSTRAP_EMAIL=admin@growgate.com \
GROWGATE_BOOTSTRAP_PASSWORD=... \
SAFETY_RECALL_BOOTSTRAP_PRODUCT="리튬이온 배터리팩" \
scripts/bootstrap-safety-recall-live.sh
```

The script logs in, runs `/api/public-data/collect`, creates an admin proof run, and prints the `SAFETY_RECALL` proof row.

## Expected Proof Row

For live:

- provider: `SAFETY_RECALL`
- provider name in UI: `산업통상부_국가기술표준원_제품안전_국내리콜정보`
- source: `live · API 직접 수집`
- API proof: `endpoint yes / key set`
- records: `> 0`
- raw response hash: `sha256:...`
- schema validation: `PASS`
- fallback reason: `-`

For CSV cache safety net:

- source: `cache · CSV cache`
- schema validation: `PASS`
- fallback reason column: `CSV cache 사용 가능 · fallback 아님`
- diagnosis linkage source: `cache`, not `fallback`

## Judge Checklist

- `/public-data/daily-refresh` does not show `SAFETY_RECALL` as fallback when the CSV cache is available.
- `SAFETY_RECALL` has `recordCount > 0`.
- `schemaValidationStatus=PASS`.
- `rawResponseSha256` is present.
- If using API-derived cache, the source badge says `API 수집 캐시`.
- Report language says this is domestic recall reference evidence, not proof of no recall, no hazard, certification validity, EU compliance, FTO clearance, or final Gate ownership.
