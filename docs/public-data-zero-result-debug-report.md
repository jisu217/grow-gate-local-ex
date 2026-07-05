# Public Data Zero Result Debug Report

Checked at: 2026-07-04

## Overall Conclusion

The current backend does not hide zero-result public data calls with fake data. It persists `live`, `cache`, `fallback`, and optional `external_feed`/`crawl` source types, and this change adds explicit zero-result diagnostics: `rawCount`, `normalizedCount`, `filteredCount`, `zeroReason`, `requestParamsMasked`, `sampleRawKeys`, `httpStatus`, and `zeroResultDiagnostics`.

Most default zero-result cases are not true API "0 rows" responses. They are live-call skips caused by catalog/application URLs or missing API keys. KTL Certinfo is not implemented as a runtime provider. External RSS is disabled until feed URLs are configured.

## Backend Call Flow

Manual public-data collection:

```text
POST /api/public-data/collect
-> PublicDataController.collect
-> PublicDataCollectionService.collectManual
-> PublicDataCollectionService.collectForDiagnosis
-> sorted PublicDataConnector.collect
-> AbstractPublicDataConnector.getJson or provider-specific feed/crawl fetch
-> normalize/extractSignal
-> public_data_collection_runs and public_data_records
-> PublicDataRecordResponse / dashboard / health / proof endpoints
```

Diagnosis agent path:

```text
POST /api/diagnoses
-> DiagnosisController
-> DiagnosisService
-> DiagnosisAsyncProcessor
-> AgentLoop tool collect_public_data
-> PublicDataCollectionService.collectForDiagnosis
-> Connector raw response
-> Normalizer metrics and zeroResultDiagnostics
-> public_data_records
-> Diagnosis result sourceTrace and source cards
```

DB path:

```text
public_data_collection_runs
-> public_data_records.raw_payload
-> public_data_records.normalized_payload
-> public_data_records.source_type/fallback_used/cache_hit
-> proof/health/source APIs
```

## Code Evidence

| Area | File/function | Finding |
| --- | --- | --- |
| Diagnosis agent tool call | `DiagnosisAsyncProcessor.process`, `collect_public_data` | Diagnosis runs public data collection through a bounded `AgentLoop`; the final Gate remains Spring rule scoring and public-data evidence, not LLM override. |
| Source trace exposure | `DiagnosisAsyncProcessor.addPublicDataSources` | Source entries expose `sourceType`, `collectionEvidence`, metrics, contribution, and now zero-result diagnostics. |
| Collection orchestration | `PublicDataCollectionService.collectForDiagnosis` | Uses cache first, then connector call if `public-data.enabled=true`; disabled mode persists fallback records. |
| Common live/fallback guard | `AbstractPublicDataConnector.getJson` | Skips live call when endpoint missing, API key missing, or endpoint is a catalog/application page. |
| Zero diagnostics | `AbstractPublicDataConnector.addZeroDiagnostics` | Adds raw/normalized/filter counts, `zeroReason`, masked params, and sample raw keys. |
| DB persistence | `PublicDataRecord` and SQL schema | Stores raw payload, normalized payload, source type, fallback flag, cache flag, endpoint, confidence, and score contribution. |
| Public response | `PublicDataRecordResponse` | Exposes operation endpoint, API key configured flag, source type, and now zero diagnostics. |

## Provider Findings

| Provider | Call path | Current 0-result cause | Code/config evidence | Fix status | Remaining risk |
| --- | --- | --- | --- | --- | --- |
| KOTRA | `DiagnosisAsyncProcessor -> PublicDataCollectionService -> KotraMarketConnector -> AbstractPublicDataConnector` | Default URL is the data.go.kr catalog page, not the approved operation endpoint. If key is missing, call is skipped before HTTP. | `application.yml` defaults `KOTRA_MARKET_API_URL=https://www.data.go.kr/data/15122648/openapi.do`; `AbstractPublicDataConnector.isCatalogOrApplicationPage` treats `data.go.kr/.../openapi.do` as catalog. | Added `CATALOG_OR_APPLICATION_ENDPOINT`, counts, masked params. No fake data added. | Need approved operation URL and sample raw response. Current params are only `country` and `keyword`, so actual KOTRA operation may require different names, paging, or date parameters. Parser is generic JSON and may produce `NORMALIZER_SCHEMA_MISMATCH_OR_UNSUPPORTED_SCHEMA` if the operation returns nested schema not covered by metrics. |
| 무역/수출입 통계 | `DiagnosisAsyncProcessor -> PublicDataCollectionService -> KcsTradeConnector -> AbstractPublicDataConnector` | Default endpoint looks like an operation endpoint, but key is empty unless `KCS_TRADE_API_KEY` or `DATAGO_SERVICE_KEY` is configured. | `KcsTradeConnector.collect` sends `hsCode`, `cntyCd`, `strtYymm`, `endYymm`; `PublicDataCollectionService.normalizeHsCode` preserves `850760` as digits; `normalizeCountry` maps Germany/DE/독일 to `DE`. | Added diagnostics. Script prints latest 12-month query range and key status. | Must confirm the official operation accepts `cntyCd=DE`; some Korean public APIs use numeric or Korean country codes. Latest month can be unpublished, so 0 rows may require wider `endYymm` or yearly aggregation. |
| KTL Certinfo | No dedicated runtime path | Provider is absent. There is no `KTL_CERTINFO` enum, connector, parser, repository-specific field, or config block. | `PublicDataProvider` has no Certinfo value. `MOTIE_WEB_CRAWL` is generic crawl and must not be described as Certinfo API. | Documented as `CONNECTOR_NOT_IMPLEMENTED`. Script reports `NOT_IMPLEMENTED` unless future Certinfo env vars are configured. | Need official confirmation whether Certinfo provides API, CSV, or only web search. Product keyword "리튬이온 배터리팩" may need category/synonym mapping before useful matches. Germany/EU filter must not be too strict. |
| SafetyKorea | `DiagnosisAsyncProcessor -> PublicDataCollectionService -> SafetyRecallConnector -> AbstractPublicDataConnector` | Default URL is an Open API guide/page and is treated as catalog/application, so live call is skipped. | `application.yml` defaults `SAFETY_RECALL_API_URL=https://www.safetykorea.kr/release/openapi`; `SafetyRecallConnector.collect` sends `hsCode` and `productName`. | Added diagnostics. No live success claimed. | Need actual recall operation endpoint and required params. SafetyKorea recall search may not support HS code; category/model/recall keyword/date range may be required. Current parser counts recall/high-risk terms generically. |
| KIPRISPlus | `DiagnosisAsyncProcessor -> PublicDataCollectionService -> KiprisFtoConnector -> AbstractPublicDataConnector` | Operation endpoint and key parameter are confirmed, but live collection still reports `API_KEY_NOT_CONFIGURED` until `KIPRIS_PLUS_ACCESSKEY`, `KIPRIS_PLUS_KEY`, or `KIPRIS_API_KEY` is injected. | `application.yml` defaults `KIPRIS_API_URL` to `http://plus.kipris.or.kr/kipo-api/kipi/patUtiModInfoSearchSevice/getAdvancedSearch`; connector sends `inventionTitle`, `astrtCont`, `pageNo`, `numOfRows`, `patent`, `utility`, and `ServiceKey`. | Updated connector and tests to use the confirmed KIPRIS Plus `getAdvancedSearch` operation. No live success is claimed without a configured key. | Authentication scope, provider-side rate limits, operation-level storage/redistribution conditions, and keyword/IPC/CPC tuning still need live validation. |
| 기업마당/Bizinfo | `DiagnosisAsyncProcessor -> PublicDataCollectionService -> BizinfoSupportConnector -> AbstractPublicDataConnector` | Default URL is `https://www.exportvoucher.com/`, not a Bizinfo operation endpoint. | `application.yml` defaults `BIZINFO_API_URL=https://www.exportvoucher.com/`; connector sends only `keyword` and `country`. | Added diagnostics. No live success claimed. | Need actual Bizinfo operation endpoint and params such as `pageUnit`, `searchKeyword`, region, industry, and date/open status. If only open programs are requested, 0 rows may be a filter issue rather than no support programs. |
| 외부 뉴스/RSS | `PublicDataCollectionService -> ExportEvidenceFeedConnector` when enabled | Disabled by default and URL list is empty. | `ExportEvidenceFeedConnector` is conditional on `PUBLIC_DATA_EXPORT_EVIDENCE_ENABLED=true`; `PUBLIC_DATA_EXPORT_EVIDENCE_URLS` defaults empty. | Added fallback zero reasons: `FEED_URLS_EMPTY`, `FEED_URL_NOT_ALLOWLISTED`, `FEED_RAW_COUNT_ZERO`, `FEED_FETCH_FAILED`. | Feed URLs, redirect/TLS/user-agent behavior, duplicate removal, and date filters need live validation. This source remains `external_feed`, not official public API proof. |

## Parser and Filter Findings

| Check | Result |
| --- | --- |
| Actual operation endpoint vs catalog URL | KIPRISPlus and KCS default to operation-looking URLs. KOTRA, SafetyKorea, and Bizinfo still default to catalog/application/home pages until approved operation endpoints are configured. |
| API key/env configured | No repository secret is present. Keys must come from environment. Script reports `apiKeyConfigured=false` when absent. |
| Request params vs official spec | KIPRISPlus `getAdvancedSearch` request params are aligned with the official detail page. KOTRA, SafetyKorea, Bizinfo, and some data.go.kr providers still need provider-specific live validation. |
| HS Code handling | Backend strips non-digits, so `850760` remains a string-like digit value in request and DB. |
| Country code handling | Backend maps Germany/독일/DE to `DE`; official APIs may need another code format. |
| Date range | KCS uses latest 12 completed months. If latest public data lags, this can still return 0 and should be widened during live validation. |
| Raw 200 but raw count 0 | Not observed locally because live keys/endpoints are not configured. Script will report `LIVE_RESPONSE_NO_RECORDS` when it happens. |
| Raw count exists but normalized count 0 | New `NORMALIZER_SCHEMA_MISMATCH_OR_UNSUPPORTED_SCHEMA` indicates parser/schema mismatch. |
| Filtering removes all rows | Current Java connectors do not apply a separate post-normalization filter. `filteredCount` equals `normalizedCount` until a filter is added. |
| Duplicate removal removes all rows | No duplicate removal is present in these Java connectors. |
| Timeout/retry/circuit breaker/fallback | No explicit retry/circuit breaker in common connector. `RestClientException` becomes fallback with sanitized reason. |
| Cache/seed/fallback shown as live | Cache is cloned with `sourceType=cache`; fallback remains fallback. Seed is not used in these provider connectors. |

## Changes Made

| File | Change |
| --- | --- |
| `backend/BE-02/src/main/java/com/growgate/publicdata/service/AbstractPublicDataConnector.java` | Added common zero-result diagnostics, masked params, sample raw keys, and zero reason classification. |
| `backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataCollectionService.java` | Added disabled-mode zero diagnostics. |
| `backend/BE-02/src/main/java/com/growgate/publicdata/service/ExportEvidenceFeedConnector.java` | Added external feed fallback zero reasons. |
| `backend/BE-02/src/main/java/com/growgate/publicdata/service/MotieWebCrawlConnector.java` | Added crawl fallback zero reasons. |
| `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java` | Exposes zero diagnostics in diagnosis source trace. |
| `backend/BE-02/src/main/java/com/growgate/publicdata/dto/PublicDataRecordResponse.java` | Exposes zero diagnostics in public-data record response. |
| `backend/BE-02/src/test/java/com/growgate/publicdata/service/AbstractPublicDataConnectorTest.java` | Added unit coverage for live, empty, and missing-key zero diagnostics. |
| `scripts/check_public_data_providers.py` | Added Provider-by-Provider direct diagnostic script with masked params. |
| `docs/public-data-provider-matrix.md` | Added current provider matrix. |

## Validation Plan

Run focused backend tests:

```bash
cd backend/BE-02
./gradlew test --tests 'com.growgate.publicdata.service.*'
```

Run full backend tests if time allows:

```bash
cd backend/BE-02
./gradlew test
```

Run provider diagnostic script:

```bash
python3 scripts/check_public_data_providers.py --json
```

## Judge Demo Proof

Use this sequence during a demo:

1. Run `python3 scripts/check_public_data_providers.py --json` to show endpoint/key/sourceType/zeroReason without leaking keys.
2. Trigger `POST /api/public-data/collect` for HS `850760`, country `DE`, product `리튬이온 배터리팩`.
3. Open `GET /api/public-data/records` and show `sourceType`, `operationEndpoint`, `apiKeyConfigured`, `rawCount`, `normalizedCount`, and `zeroReason`.
4. Open `GET /api/public-data/health` to show live/cache/fallback counts.
5. If live keys are not configured, explicitly say "환경 미구성" or "operation endpoint 미확정", not "실시간 연동 성공".

## Items Still Requiring Real External Setup

| Provider | Needed before claiming live success |
| --- | --- |
| KOTRA | Approved operation endpoint, service key, response sample, operation-specific parser. |
| KCS trade | Service key, country code confirmation, official response sample for HS `850760`/Germany. |
| KTL Certinfo | Confirm data interface and add connector/parser/tests. |
| SafetyKorea | Actual recall operation endpoint and supported search fields. |
| KIPRISPlus | Actual Open API endpoint, access key parameter name, query syntax, response sample. |
| Bizinfo | Actual operation endpoint and active/all-program filter strategy. |
| RSS | Allowlisted feed URLs and live TLS/redirect validation. |
