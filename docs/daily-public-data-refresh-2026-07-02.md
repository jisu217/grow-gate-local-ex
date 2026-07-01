# 일별 Public Data Live 수집 파이프라인

## 목적

진단 버튼을 누를 때만 공공데이터를 수집하는 구조를 보완해, 등록된 제품의 HS Code/대상국 기준으로 매일 최신 public data cache를 미리 확보한다. 발표/운영 시에는 `/api/public-data/dashboard`, `/api/public-data/runs`, `/api/public-data/records`에서 live/cache/fallback 비율과 최신 수집 시각을 확인한다.

## 수집 대상

- `products.deleted_at is null`
- `hs_code`가 비어 있지 않음
- `target_country`가 비어 있지 않음
- 기본 최대 30개 제품: `PUBLIC_DATA_DAILY_REFRESH_MAX_PRODUCTS`

## 실행 경로

1. `PublicDataDailyRefreshService`가 매일 `Asia/Seoul 03:20`에 실행된다.
2. 제품별 `hsCode`, `targetCountry`를 정규화한다.
3. 최근 `PUBLIC_DATA_DAILY_REFRESH_FRESHNESS_HOURS` 안에 모든 provider coverage가 있으면 `skipped_fresh`로 스킵한다.
4. fresh coverage가 부족하면 기존 `PublicDataCollectionService.collectForDiagnosis()`를 재사용한다.
5. provider별 connector가 live/cache/fallback을 수집하고 `normalizedPayload`에 metric, confidence, scoreContribution을 저장한다.
6. 결과는 기존 `public_data_collection_runs`, `public_data_records`에 적재된다.

## 수동 실행

로그인 사용자는 본인 제품만 대상으로 실행할 수 있다.

```bash
POST /api/public-data/daily-refresh
```

응답에는 `plannedCount`, `collectedCount`, `skippedFreshCount`, `failedCount`, `successCount`, `fallbackCount`, `cacheHitCount`, 제품별 `status/runId/reason`이 포함된다.

## 운영 설정

| 환경변수 | 기본값 | 설명 |
| --- | --- | --- |
| `PUBLIC_DATA_DAILY_REFRESH_ENABLED` | `true` | 스케줄러 실행 여부 |
| `PUBLIC_DATA_DAILY_REFRESH_CRON` | `0 20 3 * * *` | 일별 실행 cron |
| `PUBLIC_DATA_DAILY_REFRESH_ZONE` | `Asia/Seoul` | 스케줄 기준 시간대 |
| `PUBLIC_DATA_DAILY_REFRESH_MAX_PRODUCTS` | `30` | 한 번에 처리할 최대 제품 수 |
| `PUBLIC_DATA_DAILY_REFRESH_FRESHNESS_HOURS` | `20` | 같은 HS/국가 provider coverage 스킵 기준 |

## 방어 포인트

- live 수집 실패는 숨기지 않고 `fallback` record로 남긴다.
- 중복 수집은 provider coverage 기준으로 줄인다.
- 수동 endpoint는 로그인한 사용자 제품만 대상으로 하므로 다른 사용자 제품을 수집 대상으로 노출하지 않는다.
- 스케줄러는 기존 parser/normalizer/scoreContribution 경로를 재사용하므로 진단 결과와 데이터 의미가 분리되지 않는다.
