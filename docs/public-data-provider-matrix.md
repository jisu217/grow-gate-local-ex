# Public Data Provider Matrix

Checked at: 2026-07-04

This matrix separates runtime evidence from planned or external API assumptions. `live` means an operation endpoint and API key were configured and the connector completed a call. `fallback` means live collection was skipped or failed. `empty` means a live call can be made but returned no rows. `seed` is not used for these provider calls.

| Provider | Agent | Endpoint | API key env | Request params | raw count | normalized count | sourceType | zeroReason | Next action |
| --- | --- | --- | --- | --- | ---: | ---: | --- | --- | --- |
| KOTRA | market | `${KOTRA_MARKET_API_URL:https://www.data.go.kr/data/15122648/openapi.do}` | `KOTRA_MARKET_API_KEY` or `DATAGO_SERVICE_KEY` | `country`, `keyword`, `serviceKey` | 0 by default | 0 by default | fallback | `CATALOG_OR_APPLICATION_ENDPOINT` or `API_KEY_MISSING` | Replace catalog URL with approved KOTRA operation endpoint and confirm response schema for global supply-chain insight, overseas market news, or country info. |
| 무역/수출입 통계 | market | `${KCS_TRADE_API_URL:http://apis.data.go.kr/1220000/Newtrade/getNewtradeList}` | `KCS_TRADE_API_KEY` or `DATAGO_SERVICE_KEY` | `hsCode`, `cntyCd`, `strtYymm`, `endYymm`, `serviceKey` | 0 without key | 0 without key | fallback if key missing, live/empty if configured | `API_KEY_MISSING` until key is configured | Use HS `850760` as a string. Verify `cntyCd=DE` is accepted by the actual operation, and widen month range if latest month is unpublished. |
| KTL Certinfo | regulation | Not implemented in Spring connector | `KTL_CERTINFO_API_KEY` or `CERTINFO_API_KEY` if a future connector is added | Expected: product keyword and country or region | 0 | 0 | empty | `CONNECTOR_NOT_IMPLEMENTED` | Add a dedicated connector only after confirming whether Certinfo exposes an API, CSV, or public web-only search. Do not route this through MOTIE crawl as if it were Certinfo API. |
| SafetyKorea | recall | `${SAFETY_RECALL_API_URL:https://www.safetykorea.kr/release/openapi}` | `SAFETY_RECALL_API_KEY` or `DATAGO_SERVICE_KEY` | Current code sends `hsCode`, `productName`, `serviceKey` | 0 by default | 0 by default | fallback | `CATALOG_OR_APPLICATION_ENDPOINT` or `API_KEY_MISSING` | Confirm the actual recall operation and whether HS code is supported. If not, switch request params to category/model/recall keyword and date range. |
| KATS 국내리콜정보 | domestic-recall-reference | `${KATS_DOMESTIC_RECALL_API_URL:https://api.odcloud.kr/api/15040696/v1/uddi:f13d1404-717d-4862-871d-b249899ea58f}` | `KATS_DOMESTIC_RECALL_API_KEY` or `DATAGO_SERVICE_KEY` | `page`, `perPage`, `returnType`, `serviceKey` | 0 without key | 0 without key | empty/fallback if key missing, live/empty if configured | `API_KEY_MISSING` until key is configured | Use only as a file-data recall reference. It is not connected to Spring diagnosis scoring and must not be described as latest recall clearance, certification, export readiness, or FTO evidence. |
| KATS 국외리콜공표기관 | recall-agency-reference | `${KATS_FOREIGN_RECALL_AGENCY_API_URL:https://www.data.go.kr/data/15118082/fileData.do}` | `KATS_FOREIGN_RECALL_AGENCY_API_KEY` or `DATAGO_SERVICE_KEY` | `page`, `perPage`, `returnType`, `serviceKey` after actual Swagger resource path is configured | 0 by default | 0 by default | empty/fallback | `SWAGGER_OPERATION_ENDPOINT_NOT_CONFIGURED` until the actual odcloud resource path is configured | Use only as a reference list of foreign recall announcement agencies. It is not connected to Spring diagnosis scoring and must not be described as recall occurrence, certification, export readiness, or FTO evidence. |
| KIPRISPlus | fto | `${KIPRIS_API_URL:https://plus.kipris.or.kr/eng/main/contents.do?menuNo=300024}` | `KIPRIS_API_KEY` or `KIPRIS_PLUS_KEY` | Current code sends `keyword`, `country`, `serviceKey` | 0 by default | 0 by default | fallback | `CATALOG_OR_APPLICATION_ENDPOINT` or `API_KEY_MISSING` | Replace application page with an actual KIPRISPlus Open API endpoint. Confirm access key parameter name and Korean/English query syntax. |
| 기업마당/Bizinfo | support | `${BIZINFO_API_URL:https://www.exportvoucher.com/}` | `BIZINFO_API_KEY` or `DATAGO_SERVICE_KEY` | Current code sends `keyword`, `country`, `serviceKey` | 0 by default | 0 by default | fallback | `CATALOG_OR_APPLICATION_ENDPOINT` or `API_KEY_MISSING` | Replace homepage with operation endpoint. Confirm `pageUnit`, `searchKeyword`, region, industry, and open/closed date filters before limiting to active programs. |
| 외부 뉴스/RSS | news-risk | `${PUBLIC_DATA_EXPORT_EVIDENCE_URLS:}` | none | Feed URL list, keyword/country matching after fetch | 0 by default | 0 by default | fallback | `FEED_URLS_EMPTY` | Configure allowlisted RSS/Atom/JSON URLs. This remains `external_feed`, not official public API proof. |
| KOMSA MTIS | maritime-auth-status | `${MTIS_AUTH_STATUS_API_URL:https://mtisopenapi.komsa.or.kr/uat/api/authStatus.do}` | `MTIS_SERVICE_KEY` or `KOMSA_MTIS_SERVICE_KEY` | `serviceKey` only | 0 by default | 0 by default | empty/fallback | `API_KEY_MISSING` until key is configured | Use only to verify key/auth status. This is not connected to Spring diagnosis scoring and must not be described as maritime public-data evidence until an actual data operation, schema, and terms are confirmed. |
| 국토교통부 물류창고업등록정보 | logistics-warehouse | `${MOLIT_WAREHOUSE_API_URL:http://apis.data.go.kr/1611000/whsinfoview2/WhsInfoList}` | `MOLIT_WAREHOUSE_API_KEY` or `DATAGO_SERVICE_KEY` | `pageNo`, `numOfRows`, `type`, `serviceKey`; optional `sday`, `eday` | 0 without key | 0 without key | empty/fallback if key missing, live/empty if configured | `API_KEY_MISSING` until key is configured | Use only as an explicit logistics registry source after mapping the screen/report need. It is not connected to Spring diagnosis scoring and must not be described as export readiness, certification, or FTO evidence. |

## Runtime Provider Notes

Current Spring runtime providers are `KCS_TRADE`, `KOTRA_MARKET`, `SAFETY_RECALL`, `KIPRIS_FTO`, and `BIZINFO_SUPPORT` by default. `MOTIE_WEB_CRAWL` and `EXPORT_EVIDENCE_FEED` are conditional connectors controlled by `PUBLIC_DATA_CRAWL_ENABLED` and `PUBLIC_DATA_EXPORT_EVIDENCE_ENABLED`.

The requested KTL Certinfo provider is not represented by a dedicated runtime connector or enum value. It must be reported as not implemented until an actual Certinfo data interface is confirmed and wired.

KATS 국내리콜정보 is represented only in `scripts/check_public_data_providers.py` and `public_data_sources.json` as a not-runtime-connected file-data API source. The official page says newer recall information should be checked through the separate product safety certification and recall API, so this source must not be used as a latest-clearance claim.

KOMSA MTIS `authStatus` is currently represented only in `scripts/check_public_data_providers.py` as a key/auth-status smoke check. Do not put the real service key in `.env.example.presentation`, repository files, logs, screenshots, or report samples.

국토교통부 물류창고업등록정보 is currently represented only in `scripts/check_public_data_providers.py` and `public_data_sources.json` as a not-runtime-connected public data source. The official page confirms `이용허락범위 제한 없음`, but the app must still avoid exposing unnecessary business-identifying fields in reports.

KATS 국외리콜공표기관 is a static reference list of foreign recall announcement agencies. The official page confirms the file-data API metadata and Swagger URL, but the actual odcloud resource path must be copied from Swagger before a live call is treated as `sourceType=live`.

## Proof Commands

Static/direct provider check:

```bash
python3 scripts/check_public_data_providers.py --hs-code 850760 --country DE --product-name "리튬이온 배터리팩"
```

JSON output with masked params:

```bash
python3 scripts/check_public_data_providers.py --json
```

Backend proof endpoints after running a diagnosis or manual collection:

```text
POST /api/public-data/collect
GET /api/public-data/records
GET /api/public-data/health
POST /api/admin/public-data/proof-runs
GET /api/public-data/proof-runs/latest
GET /api/diagnosis/{diagnosisId}/public-data-waterfall
```
