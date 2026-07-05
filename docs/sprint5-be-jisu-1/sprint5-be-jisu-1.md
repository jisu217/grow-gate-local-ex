# GrowGate Fallback / Timeout / Retry / Circuit Breaker 정리

## 1. Fallback 발생 기준

| 구분 | 발생 기준 | 처리 방식 | Gate 영향 |
|---|---|---|---|
| 공공데이터 HTTP 호출 실패 | timeout, 네트워크 오류, HTTP 408/429/5xx, 파서 오류, 기타 HTTP 오류 | `sourceType=fallback`, `fallbackUsed=true`, `zeroReason/errorMessage` 기록 | 점수를 올릴 수 없음. confidence 하락 및 담당자 확인 항목 |
| 공공데이터 Circuit Open | provider별 연속 실패가 threshold 도달 후 open 상태 | 실제 호출 생략, `CIRCUIT_OPEN` fallback 기록 | 점수를 올릴 수 없음 |
| 공공데이터 설정 미비 | API key 없음, operation endpoint 없음, catalog/application page URL 설정 | live evidence로 보지 않음. `sourceType=empty`로 기록 (empty로 가는 경로 중 config 미비 쪽 사유이며, 자세한 정의는 아래 `empty` sourceType 행 참고. `disabled`라는 sourceType 값은 실제로 존재하지 않음) | live 공공 API 성공 건수로 설명 금지 |
| `PUBLIC_DATA_ENABLED=false` | 공공데이터 수집 비활성 | disabled fallback record 생성 (`sourceType=fallback`으로 하드코딩되며, 다른 트리거들이 거치는 empty/fallback 공통 판정 로직은 우회하는 내부 불일치가 있음), confidence `0.0`, contribution `-1` | 감점/검토 신호 |
| 유효 cache 존재 | 동일 provider/requestKey에 TTL 내 record 존재 | 외부 호출 없이 `sourceType=cache`, `cacheHit=true` | live와 별도 구분하되 성공 근거로 사용 가능 |
| `empty` sourceType | 두 경로로 발생: (1) 위 행의 config 미비, (2) 라이브 호출은 성공했지만 결과가 0건/스키마 불일치(`EMPTY_LIVE_RESPONSE`, `NORMALIZER_SCHEMA_MISMATCH`) | `sourceType=empty`, `zeroReason` 기록. fallback seed나 live proof로 취급하지 않음 | 점수 상승 근거/공식 live 성공 건수로 설명 금지 |
| `crawl` / `external_feed` 보조근거 | MOTIE crawl 또는 export evidence feed가 활성화되어 허용 host/feed에서 자료를 수집 | `sourceType=crawl` 또는 `external_feed`로 공공 API `live/cache/fallback`과 분리 | 공식 operation API live 성공 건수로 설명 금지 |
| Export evidence feed 실패 | feed URL 없음, allowlist 통과 URL 없음, 모든 feed fetch 실패 또는 item 0건 | `sourceType=fallback`, feed 실패 사유와 zero diagnostics 기록 | 보조근거 실패로만 표시, 점수 상승 불가 |
| MOTIE web crawl 실패 | crawl 비활성, URL 없음, allowlist 실패, fetch/parse 실패 또는 수집 결과 없음 | 실패 시 항상 `sourceType=fallback`으로 기록 (empty로는 기록되지 않음), 실패 사유 trace 기록 | 산업부 API live 성공으로 설명 금지 |
| Regional profile fallback | `regional_industry_profiles`에서 region/industry 매칭 없음 | controlled fallback profile과 `matchedSignals`에 fallback 사유 기록 | 공공데이터 fallback은 아니며 지역 보조 프로필로만 사용 |
| FastAPI worker 실패 | `APP_FASTAPI_ENABLED=false`, 호출 실패, 응답 없음, `engineVersion` 불일치 | Spring local evidence artifact 사용 | advisory artifact만 대체, Gate override 없음 |
| FastAPI `/analyze`, `/report` 실패 | 비활성, 호출 실패, 응답 없음 | Spring 규칙 진단 및 Spring report 경로 유지 | Gate override 없음 |
| LLM 보조 실패 | `APP_AGENT_ENABLED=false`, provider 비활성, API key 없음, quota/budget 초과, provider 실패/빈 응답 | `provider=spring-local`, `model=rule-fallback`, `llmFallbackReasons` 기록 | LLM은 advisory only. `llmInfluencedGate=false` |
| Claim verification 실패 | LLM claim이 source id를 참조하지 않거나, 과신 표현 포함, 또는 수치 주장인데 `numericEvidence` 근거 없음 | claim 차단/검증 실패 사유 기록 | 보고서 문구 guardrail, Gate override 없음 |
| Frontend API 실패 | API 실패/timeout 발생 | mock 데이터로 정상 표시하지 않고 error state 또는 상세 응답의 실제 source fallback만 표시 | 실패 은폐 없음 |
| 진단 실행 전체 실패 | 진단 파이프라인 처리 중 미처리 예외(JSON 파싱 오류, DB 오류 등) | 전체 파이프라인을 감싸는 단일 try/catch에서 `DiagnosisStatus.failed`로 전환, retry 없음, 일반화된 오류 메시지 기록 | 결과 생성 없음, 사용자에게는 실패 상태로 노출 |

## 2. Timeout 설정

| 구분 | 기본값 | 설정 키/위치 | 비고 |
|---|---:|---|---|
| Spring 공공데이터/LLM/FastAPI 공용 connect timeout | `3000ms` | `PUBLIC_DATA_CONNECT_TIMEOUT_MS` / `public-data.http.connect-timeout-ms` | `RestTemplate` 공용 설정 |
| Spring 공용 read timeout | `20000ms` | `PUBLIC_DATA_READ_TIMEOUT_MS` / `public-data.http.read-timeout-ms` | 공공데이터 및 동일 `RestTemplate` 사용 경로에 적용 |
| Frontend API fetch timeout | `15000ms` | `VITE_API_TIMEOUT_MS` | timeout 시 사용자에게 API 요청 시간 초과 노출 |
| Demo Python external LLM timeout | `12s` | `GROW_GATE_LLM_TIMEOUT_SECONDS` | demo external LLM gateway 전용 |
| 공공데이터 provider 점검 스크립트 | `10s` | `scripts/check_public_data_providers.py --timeout` | 운영 앱 런타임이 아닌 점검 스크립트 |
| 공공데이터 cache TTL | `24h` | `PUBLIC_DATA_CACHE_TTL_HOURS` / `public-data.cache-ttl-hours` | TTL 안의 동일 requestKey는 외부 호출 대신 cache record 복제 |
| Daily refresh freshness window | `20h` | `PUBLIC_DATA_DAILY_REFRESH_FRESHNESS_HOURS` / `public-data.daily-refresh.freshness-hours` | 최근 수집 자료가 freshness window 안이면 재수집 skip |
| Daily refresh 실행 트리거 | Spring `@Scheduled` cron | `PUBLIC_DATA_DAILY_REFRESH_ENABLED`(기본 `true`), `PUBLIC_DATA_DAILY_REFRESH_CRON`(기본 `0 0 8 * * *`), `PUBLIC_DATA_DAILY_REFRESH_ZONE`(`Asia/Seoul`), `PUBLIC_DATA_DAILY_REFRESH_MAX_PRODUCTS`(기본 `30`) | 매일 지정 시각 자동 실행, freshness window 밖 제품만 대상 |
| 운영 상태 노출 | 현재 설정값 반환 | `/api/public-data/health`, `PublicDataOperationalStatusService` | connect/read timeout, retry max attempts, circuit 설정을 smoke/운영 화면에서 확인 |

## 3. Retry 정책

| 구분 | 기본 retry | Retry 조건 | 실패 후 처리 |
|---|---:|---|---|
| 공공데이터 HTTP GET | `max-attempts=2` | 네트워크 접근 실패, timeout 계열 `ResourceAccessException`, HTTP `408`, `429`, `5xx` | fallback record 생성 |
| Spring AgentLoop 도구 실행 | 주요 외부/선택 도구 `2회` | verifier 실패 또는 exception | deterministic fallback 사용 및 trace 기록 |
| FastAPI worker | `2회` | 응답 없음, 호출 실패, `engineVersion` 검증 실패 | Spring local artifact 사용 |
| FastAPI `/analyze`, `/report` | `2회` | 응답 없음 또는 호출 실패 | Spring 규칙 진단/report 유지 |
| LLM provider chain | AgentLoop 기준 `2회` | provider chain 실패 또는 provider 없음 | `spring-local rule-fallback` |
| Frontend GET 요청 | `2회` | timeout, HTTP `408`, `429`, `5xx` | 최종 error state 노출 |
| Python worker retry plan | 실제 재호출 아님 | live/cache evidence 없음 또는 fallback 존재 | `retryPlan`에 권고 backoff `[30, 120, 600]` 기록 |
| Daily refresh 재수집 | freshness window 밖인 제품만 수집 | 최근 수집 자료가 있으면 skip, 오래됐거나 없는 제품은 provider 수집 실행 | `skippedFreshCount`, `collectedCount`, `failureReasons`로 결과 노출 |
| Public data 수동 재수집 | 사용자 요청 시 `/api/public-data/collect` 호출 | cache TTL이 유효하면 cache 우선, 없으면 connector 호출 | run 단위 `successCount`, `fallbackCount`, `cacheHitCount` 기록 |
| Mock fallback 재시도 | 없음 | frontend service에서 mock fallback 제거 | 실패를 mock 성공으로 바꾸지 않고 실제 에러/trace 확인 |

## 4. Circuit Breaker 정책

| 구분 | 값 |
|---|---|
| 적용 대상 | 공공데이터 provider별 HTTP connector |
| 상태 저장 | JVM memory 내 provider별 static circuit map |
| 실패 threshold | `3`회 |
| open 유지 시간 | `60s` |
| 설정 키 | `PUBLIC_DATA_CIRCUIT_FAILURE_THRESHOLD`, `PUBLIC_DATA_CIRCUIT_OPEN_SECONDS` |
| 실패 카운트 증가 조건 | retry 소진 후 HTTP/timeout 계열 실패 |
| 성공 시 처리 | provider circuit state 제거 |
| open 상태 호출 | 실제 HTTP 호출하지 않고 `CIRCUIT_OPEN` fallback 반환 |
| 주의 | 프로세스 재시작 시 circuit state는 초기화됨 |
| 다중 인스턴스 한계 | process-local memory 기반이라 instance 간 circuit state 공유 안 됨 |
| 운영 고도화 방향 | 다중 backend 운영 시 Redis, DB, gateway-level circuit breaker로 이전 필요 |

## 5. SourceType 설명 원칙

| 구분 | 설명 |
|---|---|
| 공식 공공 API 성공 근거 | `sourceType=live/cache`, `operationEndpoint=true`, `apiKeyConfigured=true` 조합으로만 설명 |
| 공식 live proof 제외 | `fallback`, `empty`, `crawl`, `external_feed`, `heuristic`, `agent`, `seed`, `user_input`, `internal`, `direct_input` |
| `fallback` | HTTP 실패, timeout, rate limit, parser error, circuit open 등 호출 실패 trace |
| `empty` | key/endpoint/catalog URL/empty response처럼 live proof가 성립하지 않는 상태 |
| `crawl` / `external_feed` | 보조 최신성/문서 근거. 공식 operation API live 성공으로 카운트하지 않음 |
| `heuristic` / `agent` | 규칙/워크플로우 trace. 최종 Gate 소유권 없음 |
| `user_input` / `internal` / `direct_input` | 진단 evidence source 목록에서 사용자 입력값, 내부 계산값, 지식문서 없는 직접 입력 스냅샷을 표시. live proof 아님 |
| 주의 | `ProductKnowledge.sourceType`(예: `internal_test_report`)은 자유 텍스트 도메인 컬럼으로, 위의 enum성 evidence `sourceType` 값들과 키 이름은 같지만 다른 개념 — 혼동 주의 |
