# Codex Prompt 08 — Public Data Live Proof 강화

목표:
공공데이터 심사 질문 “실제 live API record 몇 건입니까?”에 S10과 `/api/public-data/health`로 답한다.

작업:
1. `.env.example`에 provider별 `*_OPERATION_API_URL`, `*_API_KEY`, `*_ALLOWED_HOSTS`를 명시한다.
2. catalog/신청 페이지 URL이면 `operationEndpoint=false`와 fallback reason 표시.
3. `raw_response_hash` 필드 추가 또는 normalizedPayload.collectionEvidence에 저장.
4. health response에 `liveProofChecklist` 추가:
   - provider
   - sourceType
   - operationEndpoint
   - apiKeyConfigured
   - fetchedAt
   - collectionRunId
   - rawResponseHash
   - metricSummary
5. S10 Proof Card에 live/cache/fallback/seed를 분리 표시한다.
6. synthetic seed는 liveProofChecklist에 포함하되 `proofEligible=false`로 표시한다.

수용 조건:
- MOTIE 관련 provider 2건 이상 live/cache+operationEndpoint+apiKeyConfigured이면 `motieLiveEvidenceReady=true`.
- seed/fallback/crawl/external_feed는 official live proof로 카운트하지 않는다.
