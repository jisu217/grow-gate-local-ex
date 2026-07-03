# Public Data Evidence Contract Closure - 2026-07-03

## Problem

공공데이터 connector는 `collectionEvidence`를 만들고 있었지만, 진단 상세 DTO와 `/diagnosis/{id}/sources` DTO가 일부 필드를 버리면 S10 화면에서 live API 증거가 약하게 보인다. 심사 질문은 "백엔드 내부에 값이 있습니까?"가 아니라 "화면에서 operation endpoint, API key configured, collection run id, fetchedAt, metric impact를 확인할 수 있습니까?"에 가깝다.

## Solution

- `DiagnosisDetailResponse.ResultData`가 `sourceTrustSummary`, `scoreContributionLedger`, `publicDataCollectionRunId`, `inputTrace`, Python worker/agent trace, normalized metrics를 그대로 보존한다.
- `DiagnosisSourcesResponse.SourceEntry`가 `collectionEvidence`, `operationEndpoint`, `apiKeyConfigured`, `endpointType`, `fetchedAt`, `metricSummary`, `decisionImpact`, `metrics`, `domainSignals`를 노출한다.
- `DiagnosisService.getDetail()`과 `getSources()`가 result JSON에서 해당 필드를 DTO로 명시적으로 매핑한다.
- frontend mapper는 상세 DTO에 `sourceTrustSummary` 또는 ledger가 없을 때 `/sources` 응답으로 복원한다.
- S10과 출처 상세 화면은 live/cache/fallback/crawl/external_feed를 분리하고, official operation API evidence를 `live/cache + operationEndpoint + apiKeyConfigured` 조합으로만 표시한다.

## Guardrails

- `DiagnosisServiceTest.getDetailPreservesEvidenceContractFieldsFromResultJson()`가 result JSON 필드 보존을 검증한다.
- `DiagnosisServiceTest.getSourcesPreservesCollectionEvidenceForPublicApiProof()`가 `/sources` proof 필드 보존을 검증한다.
- `frontend/FE-02/src/services/diagnosisService.mapper.test.js`가 source proof fallback 복원과 operation/API key count를 검증한다.
- `scripts/check-risk-gates.sh`가 DTO, service mapping, mapper test, presentation proof script를 함께 확인한다.

## Presentation Rule

발표장에서 공식 live 공공 API 증거로 말할 수 있는 record는 다음 조건을 모두 만족해야 한다.

```text
sourceType in [live, cache]
collectionEvidence.operationEndpoint = true
collectionEvidence.apiKeyConfigured = true
collectionRunId exists
fetchedAt/retrievedAt exists
metricSummary or metrics exists
```

`fallback`, `crawl`, `external_feed`, `heuristic`, `agent`는 보조근거 또는 처리 trace로만 설명한다. 특히 뉴스/보고서 feed는 최신성 보조근거이지 산업부 live 공공 API 성공 건수로 세지 않는다.

## Commands

```bash
cd backend/BE-02
JAVA_HOME=/private/tmp/temurin17/Contents/Home GRADLE_USER_HOME=.gradle-home ./gradlew test --tests com.growgate.diagnosis.service.DiagnosisServiceTest --no-daemon

cd frontend/FE-02
node src/services/diagnosisService.mapper.test.js

DIAGNOSIS_ID=<completed-diagnosis-id> \
AUTH_TOKEN=<jwt> \
PRESENTATION_REQUIRE_OPERATION_API=true \
./scripts/presentation-public-data-proof.sh
```
