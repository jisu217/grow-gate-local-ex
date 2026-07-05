# SAFETY_RECALL KATS Cache-Aside Connector

## Problem

The production public-data proof page showed `SAFETY_RECALL` as `fallback`, `endpoint no`, `key missing`, and `schemaValidation=SKIPPED`. Judges would read that as "recall data is not connected" even though the project already had an official KATS domestic recall CSV.

Root cause: the runtime `SAFETY_RECALL` connector still pointed at the old SafetyKorea guide page. The guide page is not an operation endpoint, so the collection layer correctly refused to count it as API proof and emitted fallback rows.

## Pattern

Use a cache-aside connector for file-data public datasets:

1. Configure the real operation endpoint when available.
2. If an API key exists, call the operation endpoint and persist an API-derived cache with a raw hash and normalized rows.
3. If the API key is missing or the operation call fails, read the official CSV cache.
4. Mark readable official CSV cache as `sourceType=cache`, not `fallback`.
5. Keep `fallback` only for cases where neither live API nor official cache can be read.

## Implementation Notes

- Runtime provider: `SAFETY_RECALL`.
- Public data source: `산업통상부_국가기술표준원_제품안전_국내리콜정보`.
- Operation endpoint: `https://api.odcloud.kr/api/15040696/v1/uddi:f13d1404-717d-4862-871d-b249899ea58f`.
- Classpath CSV cache: `backend/BE-02/src/main/resources/publicdata/kats-domestic-recall-20230809.csv`.
- API-derived cache root: `data/cache/safety_recall/YYYY-MM-DD/`.

The UI should display the cache mode as `cache · CSV cache` and the provider as `산업통상부_국가기술표준원_제품안전_국내리콜정보`. It should not surface CSV cache as an operation/key failure.

## Guardrail

The cache is recall-risk context only. Do not claim latest recall clearance, product safety, certification adequacy, EU compliance, export readiness, or final Gate proof from this dataset alone.

## Verification

Run:

```bash
cd backend/BE-02
./gradlew test --tests com.growgate.publicdata.service.SafetyRecallConnectorTest --tests com.growgate.publicdata.service.ProviderSchemaNormalizationTest --tests com.growgate.publicdata.service.KatsDomesticRecallReportConnectorTest

cd ../../frontend/FE-02
npm run test:mappers
```

Production proof check:

```bash
BASE_URL=https://growgate.54.180.56.24.sslip.io scripts/bootstrap-safety-recall-live.sh
```

Expected keyless result: `SAFETY_RECALL`, `sourceType=cache`, `records=881`, schema `PASS`, fallback reason containing `CSV cache available`.
