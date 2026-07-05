# Public Data API-Key Collection Verification

Checked at: 2026-07-05

This document lists what has actually been verified for public-data collection. It intentionally does not include real service keys, raw secrets, or unredacted request URLs.

## Summary

| Category | Count | Status |
| --- | ---: | --- |
| API-key based live collection confirmed in this Codex environment | 0 | No provider key was present in the process environment during verification. Do not claim live API proof from this run. |
| Operation endpoint configured and ready for key-based smoke | 5 | KCS trade, KSURE buyer search, KIPRIS Plus, KOMSA MTIS auth status, MOLIT warehouse registry. |
| Local cache/file collection confirmed | 10 | CSV/DOCX/catalog sources loaded successfully with `sourceType=cache`. |
| Raw rows/documents loaded from local cache | 145845 | Sum of cache records reported by `scripts/check_public_data_providers.py --json`. |
| Real API keys stored in repository | 0 | Key leak scan found no pasted KIPRIS/data.go.kr/KOMSA keys. |

## API-Key Live Collection Confirmed

No provider is currently listed as API-key based live collection confirmed.

Reason: the verification shell had no configured provider key environment variables:

- `DATAGO_SERVICE_KEY=false`
- `KIPRIS_PLUS_ACCESSKEY=false`
- `KIPRIS_PLUS_KEY=false`
- `KIPRIS_API_KEY=false`
- `KCS_TRADE_API_KEY=false`
- `MOTIE_IMPORT_REGULATION_API_KEY=false`
- `TRADE_SECURITY_HSK_LINKAGE_API_KEY=false`

Therefore, any provider that needs a key correctly reports `sourceType=empty` and `zeroReason=API_KEY_NOT_CONFIGURED` instead of pretending to be live.

## Operation-Ready, Key Required

These providers have operation endpoints configured or confirmed, but live data collection was not confirmed because the key was not injected into the runtime environment.

| Provider | Runtime provider | Endpoint status | Key env | Current sourceType | zeroReason | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 무역/수출입 통계 | `KCS_TRADE` | operation endpoint configured | `KCS_TRADE_API_KEY` or `DATAGO_SERVICE_KEY` | `empty` | `API_KEY_NOT_CONFIGURED` | Request params are `hsCode`, `cntyCd`, `strtYymm`, `endYymm`. Country-code acceptance still needs live validation. |
| 한국무역보험공사 바이어 검색 | `NOT_RUNTIME_CONNECTED` | operation endpoint captured from data.go.kr screen | `KSURE_BUYER_SEARCH_API_KEY` or `DATAGO_SERVICE_KEY` | `empty` | `API_KEY_NOT_CONFIGURED` | Uses local codebook for country/industry code reference. Not buyer creditworthiness proof. |
| KIPRIS Plus | `KIPRIS_FTO` | operation endpoint confirmed | `KIPRIS_PLUS_ACCESSKEY`, `KIPRIS_PLUS_KEY`, or `KIPRIS_API_KEY` | `empty` | `API_KEY_NOT_CONFIGURED` | Confirmed operation: `특허·실용 공개·등록공보 / 전체검색 - getAdvancedSearch`; key parameter `ServiceKey`; params `inventionTitle`, `astrtCont`, `pageNo`, `numOfRows`, `patent`, `utility`. FTO remains preliminary risk-signal search only. |
| KOMSA MTIS | `NOT_RUNTIME_CONNECTED` | auth status endpoint configured | `MTIS_SERVICE_KEY` or `KOMSA_MTIS_SERVICE_KEY` | `empty` | `API_KEY_NOT_CONFIGURED` | Key/auth status smoke only. Not evidence for export readiness. |
| 국토교통부 물류창고업등록정보 | `NOT_RUNTIME_CONNECTED` | operation endpoint configured | `MOLIT_WAREHOUSE_API_KEY` or `DATAGO_SERVICE_KEY` | `empty` | `API_KEY_NOT_CONFIGURED` | Registry reference only. Avoid unnecessary business-identifying fields in reports. |

## Cache/File Collection Confirmed

These sources were actually read by the local provider smoke without API keys. They are valid cache/file evidence, not live API proof.

| Provider | Runtime provider | sourceType | rawCount | normalizedCount | zeroReason | Use limit |
| --- | --- | --- | ---: | ---: | --- | --- |
| MOTIE 반도체디스플레이 산업 동향 | `NOT_RUNTIME_CONNECTED` | `cache` | 22 | 22 |  | Annual industry trend reference only. |
| MOTIE 제품 리콜처분현황 | `NOT_RUNTIME_CONNECTED` | `cache` | 4 | 4 | `CSV_CACHE_STALE_API_KEY_NOT_CONFIGURED` | Aggregate recall-disposition statistics only. |
| MOTIE 제품안전인증 신규등록현황 | `NOT_RUNTIME_CONNECTED` | `cache` | 4 | 4 |  | Aggregate safety-cert registration trend only. |
| MOTIE 금속표준용어 | `NOT_RUNTIME_CONNECTED` | `cache` | 1 | 1 |  | Terminology dictionary/knowledge source only. |
| 관세청 HS/관세율/운송비 reference | `KCS_TARIFF_LOGISTICS` | `cache` | 1377 | 1377 |  | HS/tariff/logistics reference only; not legal tariff certainty or freight quote. |
| KATS 국내리콜정보 | `NOT_RUNTIME_CONNECTED` | `cache` | 881 | 881 | `CSV_CACHE_STALE_API_KEY_NOT_CONFIGURED` | Recall reference list only; not latest recall clearance. |
| KATS 인증제품 첨부파일 | `NOT_RUNTIME_CONNECTED` | `cache` | 121525 | 121525 | `CSV_CACHE_STALE_API_KEY_NOT_CONFIGURED` | Attachment path/name reference only. |
| KATS 인증제품 제조공장 | `NOT_RUNTIME_CONNECTED` | `cache` | 21871 | 21871 | `CSV_CACHE_STALE_API_KEY_NOT_CONFIGURED` | Manufacturer name/country reference only. |
| KATS 제품안전 민원접수 | `NOT_RUNTIME_CONNECTED` | `cache` | 145 | 145 |  | Complaint trend reference with redaction/minimization. |
| KATS 국외리콜공표기관 | `NOT_RUNTIME_CONNECTED` | `cache` | 15 | 15 |  | Static foreign recall agency reference list only. |

## KIPRIS Plus Catalog Verification

KIPRIS Plus free-data catalog collection was verified separately from live patent/FTO operation collection.

| Item | Result |
| --- | --- |
| Script | `python3 scripts/collect_kipris_plus_catalog.py --start-page 1 --end-page 13 --output data/public/kipris_plus_catalog_pages_1_13.json` |
| Source type | `cache` |
| Pages collected | 13 |
| HTTP statuses | `200` for collected pages |
| API key stored | `false` |
| Operation smoke | not attempted because `KIPRIS_PLUS_ACCESSKEY` was not configured |
| Legal/FTO use | Catalog metadata only. Not infringement, validity, freedom-to-operate, or sales/export permission proof. |

## Commands Used

```bash
python3 scripts/check_public_data_providers.py --json
python3 scripts/collect_kipris_plus_catalog.py --start-page 1 --end-page 13 --output data/public/kipris_plus_catalog_pages_1_13.json
./gradlew test --tests com.growgate.publicdata.service.KiprisFtoConnectorTest --tests com.growgate.publicdata.service.MotieImportRegulationConnectorTest --tests com.growgate.publicdata.service.TradeSecurityHskLinkageConnectorTest --tests com.growgate.publicdata.service.TradeSecurityItemKeywordConnectorTest --tests com.growgate.publicdata.service.PublicDataCollectionServiceTest --no-daemon
./scripts/verify-local.sh
```

## How To Add A Provider To The Live-Confirmed List

1. Inject the provider key through a local shell, CI secret, or server secret store. Do not commit it.
2. Run the provider smoke.
3. Confirm `operationEndpoint=true`, `apiKeyConfigured=true`, `sourceType=live`, and `rawCount` or `normalizedCount` from the operation response.
4. Store only redacted evidence: provider name, endpoint type, sourceType, counts, checked date, masked parameter names, and raw response hash if needed.
5. Update this document. Do not copy raw service keys, full unredacted URLs, or raw personal/business-sensitive payloads.
