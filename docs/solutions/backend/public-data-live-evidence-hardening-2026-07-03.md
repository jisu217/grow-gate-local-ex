# Public Data Live Evidence Hardening - 2026-07-03

## Problem

심사 질문은 "API를 호출했는가"보다 "받은 데이터 필드를 실제로 읽고, 최신성/실패/점수 영향을 화면에서 방어할 수 있는가"에 집중된다. 이전 구조는 fallback 표시는 있었지만 provider별 schema fixture, fallback positive guard, 첫 화면 freshness signal, 뉴스/보고서 feed 수집 경로가 약했다.

## Decision

- 공식 API record는 `live/cache/fallback`으로 유지하고, 수출 뉴스·기사·연구소 보고서는 `external_feed`로 분리한다.
- `external_feed`는 allowlist RSS/Atom/JSON만 수집하고 contribution을 최대 +3점으로 제한한다.
- provider connector는 `fieldMapping`과 fixture 기반 normalization test를 가진다.
- fallback record는 저장 단계에서 positive contribution을 만들 수 없게 clamp한다.
- Source Trust Bar와 S10에는 `lastCollectedAt`, `sourcePeriod`, `liveSuccessRate`, `providerHealth`, failure reason, score waterfall을 노출한다.

## Guardrails

- `ProviderSchemaNormalizationTest`가 KOTRA/Safety/KIPRIS/Bizinfo/export feed fixture를 검증한다.
- `PublicDataCollectionServiceTest.collectForDiagnosisClampsFallbackPositiveContributionBeforePersisting()`가 fallback 양수 기여 저장을 막는다.
- `scripts/check-risk-gates.sh`가 fixture, field mapping, retry API 연결, seed watermark, export feed 설정 문서화를 검사한다.

## Presentation Rule

- `external_feed`는 "수출 뉴스·보고서 최신성 보조 근거"로만 설명한다.
- `fallback`과 `seed`는 live public data evidence로 설명하지 않는다.
- live 사용 증거는 smoke 결과의 `liveCount/cacheCount`, `collectionRunId`, `sourceIds`, Source Trust Bar screenshot으로 제시한다.
