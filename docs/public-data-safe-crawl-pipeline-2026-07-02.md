# Public Data Safe Crawl Pipeline - 2026-07-02

## 목적

공식 API operation endpoint가 없거나 보조 정책 근거가 필요한 경우, 공개 웹 페이지를 제한적으로 수집해 Gate 판단 근거에 연결한다. 이 경로는 live 공공 API를 대체하지 않고 `sourceType=crawl`로 분리 표시한다.

## 수집

- 기본값은 비활성: `PUBLIC_DATA_CRAWL_ENABLED=false`
- 활성 조건: `PUBLIC_DATA_CRAWL_ENABLED=true`와 `PUBLIC_DATA_CRAWL_URLS`가 모두 설정되어야 함
- 허용 조건: HTTPS URL, `PUBLIC_DATA_CRAWL_ALLOWED_HOSTS`에 포함된 host, 최대 5페이지
- 기본 allowlist: `www.motie.go.kr`, `motie.go.kr`, `www.kotra.or.kr`, `dream.kotra.or.kr`, `www.safetykorea.kr`, `www.data.go.kr`
- User-Agent: `GrowGateLocalEX/1.0 public-data-crawler`

## 전처리

- HTML `script`, `style`, `noscript` 제거
- HTML tag 제거 및 entity 일부 정규화
- raw HTML 120,000자 제한, page text snippet 3,000자 제한
- API key/token/password 패턴 마스킹
- 이메일/전화번호 패턴 마스킹
- HS Code, 대상국, 제품명 token, 인증/고시/공고/리콜/안전/수출/지원 키워드 hit 계산

## 적재

- 기존 `public_data_collection_runs`, `public_data_records`에 connector 결과로 적재
- provider: `MOTIE_WEB_CRAWL`
- sourceType: `crawl`
- usedFor: `regulation`
- normalizedPayload:
  - `metrics.pageCount`
  - `metrics.successPageCount`
  - `metrics.keywordHits`
  - `metrics.documentHintCount`
  - `metrics.hsCodeHitCount`
  - `collectionEvidence.allowlistHosts`
  - `collectionEvidence.secretMasked`
  - `collectionEvidence.personalInfoMasked`
  - `metricSummary`
  - `decisionImpact`

## 운영 기준

- 크롤링은 공공 API live record로 설명하지 않는다.
- robots.txt와 이용약관은 운영 반영 전 담당자가 확인한다.
- fallback 또는 crawl source는 담당자 검토 보조 근거이며, 자동 의사결정 근거로 단독 사용하지 않는다.
- S10 화면에서 `Crawl` badge로 live/cache/fallback/heuristic과 구분한다.
