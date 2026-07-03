# Live Public Data Collection Design

작성일: 2026-06-30

## 목표

GROW-GATE Local EX의 제출용 메인 앱인 Spring `BE-02`가 실제 공공 API 수집 레이어를 갖도록 한다. API key가 있으면 live 호출을 수행하고, API key가 없거나 외부 API가 실패하면 실패를 숨기지 않고 cache/fallback/source trace에 명시한다.

## 범위

이번 범위는 실제 운영 API 키가 없는 로컬/심사 환경에서도 컴파일·테스트 가능한 실무형 수집 구조를 만드는 것이다.

- Spring `BE-02`에 `publicdata` 모듈 추가
- provider별 connector 인터페이스와 구현 추가
- live 수집 run/record DB 저장
- 진단 엔진에서 live/cache record를 조회하고 점수 및 source trace에 반영
- 운영 API로 수집 실행/이력/record 조회 제공
- MySQL/SQLite schema와 README/상태 문서 갱신

## 비범위

- 공공데이터포털 API key 발급 자동화
- 실제 배포 환경 secret manager 연결
- 모든 provider의 실제 응답 필드 완전 정규화
- live API 장애 모니터링/알림
- pgvector 기반 대규모 문서 검색 또는 실제 라벨 ML

## 아키텍처

Spring 메인 앱에 `publicdata` 패키지를 추가한다.

```text
publicdata/
├── controller/PublicDataController.java
├── dto/*.java
├── entity/PublicDataCollectionRun.java
├── entity/PublicDataRecord.java
├── enums/PublicDataProvider.java
├── repository/*.java
└── service/
    ├── PublicDataCollectionService.java
    ├── PublicDataConnector.java
    ├── AbstractPublicDataConnector.java
    ├── KcsTradeConnector.java
    ├── KotraMarketConnector.java
    ├── SafetyRecallConnector.java
    ├── KiprisFtoConnector.java
    └── BizinfoSupportConnector.java
```

`PublicDataCollectionService`는 connector들을 orchestration한다. 입력은 `hsCode`, `country`, `productName`이고, 출력은 provider별 normalized record와 collection run이다.

## 데이터 흐름

1. 진단 생성 후 `DiagnosisAsyncProcessor`가 제품/입력값에서 `hsCode`, `targetCountry`, `productName`을 추출한다.
2. `PublicDataCollectionService.collectForDiagnosis()`를 호출한다.
3. 서비스는 provider별 cache를 먼저 조회한다.
4. cache가 TTL 내에 있으면 record를 재사용한다.
5. cache가 없고 provider가 enabled이면 connector가 live API를 호출한다.
6. API key가 없거나 호출 실패 시 fallback record를 만들고 `fallbackUsed=true`, `errorMessage`를 남긴다.
7. 진단 결과의 `sources`에 provider, endpoint, requestKey, recordId, scoreContribution, fallbackUsed, errorMessage를 남긴다.

## API key 설정

환경변수는 provider별로 분리한다.

| 변수 | 의미 |
| --- | --- |
| `PUBLIC_DATA_ENABLED` | live 수집 전체 on/off |
| `PUBLIC_DATA_CACHE_TTL_HOURS` | cache 재사용 시간 |
| `DATAGO_SERVICE_KEY` | 공공데이터포털 공통 인증키 alias |
| `KCS_TRADE_API_URL` / `KCS_TRADE_API_KEY` | 관세청 수출입무역통계. 기본 URL은 `http://apis.data.go.kr/1220000/Newtrade/getNewtradeList` |
| `KOTRA_MARKET_API_URL` / `KOTRA_MARKET_API_KEY` | KOTRA 시장/뉴스. 기본 URL은 공식 카탈로그이며 활용신청 후 operation endpoint로 교체 |
| `SAFETY_RECALL_API_URL` / `SAFETY_RECALL_API_KEY` | Safety Korea 제품안전/리콜. 기본 URL은 공식 안내이며 operation endpoint로 교체 |
| `KIPRIS_API_URL` / `KIPRIS_API_KEY` | KIPRIS Plus. `KIPRIS_PLUS_KEY` alias 지원 |
| `BIZINFO_API_URL` / `BIZINFO_API_KEY` | 기업마당/수출바우처 지원사업 |

공식 카탈로그/신청 페이지 URL은 출처와 신청 위치를 보존하기 위한 기본값이다. connector는 이 URL을 실제 API로 오인해 호출하지 않고 fallback record에 operation endpoint 설정 필요 사유를 남긴다.

## 오류 처리

- API key 없음: `status=SKIPPED`, fallback record 저장
- HTTP timeout/5xx: retry 후 `status=FAILED`, fallback record 저장
- 응답 파싱 실패: raw payload 저장 후 normalized fallback 저장
- 진단은 실패시키지 않고, source trace에 fallback 이유를 노출

## 테스트 전략

- connector는 `MockRestServiceServer`로 API key 있음/없음, 성공/실패 응답을 검증
- collection service는 cache hit, cache miss, fallback 생성, run summary를 검증
- diagnosis processor는 public data record가 source trace와 점수에 반영되는지 단위 검증
- 전체 검증 명령:
  - `JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH GRADLE_USER_HOME=.gradle-home ./gradlew test`
  - `npm run build`

## 제출 표현

완료 후에는 “실제 live 공공 API 수집 구조와 provider별 key 기반 호출, cache/fallback/source trace를 구현했다”고 설명할 수 있다. 다만 API key가 제공되지 않은 심사 환경에서는 fallbackUsed가 true가 될 수 있으므로, “항상 live 데이터가 수집된다”가 아니라 “live 수집 가능 구조와 실패 투명성을 갖췄다”고 표현한다.
