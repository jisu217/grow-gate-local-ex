# Codex Prompt 13 — Multi-Agent Public Data Harness

목표:
공공데이터를 여러 agent가 공유하되, 최종 Gate는 deterministic rule score와 risk/fallback 조건만 소유한다. Agent는 출처가 있는 설명, 누락 근거 탐지, 보고서 초안, 재수집 계획을 돕는다.

역할:
1. PublicDataCollectorAgent
   - Spring `PublicDataCollectionService`와 provider connector만 호출한다.
   - `sourceType`, `collectionEvidence`, `operationEndpoint`, `apiKeyConfigured`, `metricSummary`, `decisionImpact`를 기록한다.
   - CSV fast path와 API refresh 조건을 구분한다.
2. EvidenceCuratorAgent
   - `public_data_sources.json`, public-data records, knowledge documents를 source-bound chunk로 정리한다.
   - source id 없는 claim은 report 후보에서 제외한다.
3. ClaimVerifierAgent
   - 보고서 문장마다 `sourceIds`, `sourceType`, `claimBoundary`를 확인한다.
   - fallback/seed/external_feed를 official live proof로 카운트하지 않는다.
4. LoopEngineerAgent
   - 실패 원인을 `API_KEY_NOT_CONFIGURED`, `CSV_CACHE_STALE_API_KEY_NOT_CONFIGURED`, `EMPTY_LIVE_RESPONSE`, `SCHEMA_MISMATCH`처럼 재수집 가능한 항목으로 분류한다.
   - 담당자 승인 전 self-loop wiki 항목을 확정 지식으로 승격하지 않는다.
5. ReportAgent
   - 경영진/심사위원용 요약은 “Rule-based Gate + source-bound evidence”로 표현한다.
   - LLM/Agent가 Gate를 바꿨다는 표현을 금지한다.

공공데이터 사용 규칙:
- `live`: operation endpoint + API key + usable records.
- `cache`: 첨부 CSV/PDF/HWPX/DOCX 또는 이전 수집 record.
- `fallback`: 호출 실패, key 없음, endpoint 미구성, schema mismatch.
- `seed`: 합성 데모. public API proof 아님.
- KICOX runtime provider `KICOX_OPERATION_RATE`와 `KICOX_FACTORY_REGISTRATION_BY_REGION_INDUSTRY`는 시장/지역 aggregate 참고 신호이며 특정 기업 생산능력, 수요, 입주 가능성, 공급 안정성, 최종 Gate를 확정하지 않는다.
- MOTIE/KSURE runtime provider `MOTIE_TRADE_REMEDY_OCCURRENCE`, `MOTIE_TRADE_REMEDY_FTA_LIST`, `KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE`는 CSV fast path + update-due API refresh 참고 신호이며 품목 제소, 관세율/원산지, 바이어 신용도, 보험 인수, 최종 Gate를 확정하지 않는다.
- FTA PDF와 KEA HWPX는 지식화 context이며 점수/Gate proof로 쓰지 않는다.

하네스 수용 조건:
- `scripts/check_public_data_providers.py --json`에서 KICOX 두 provider가 `cache` 또는 key 구성 시 `live`로 설명 가능해야 한다.
- `scripts/check_public_data_providers.py --json`에서 MOTIE 무역구제/FTA 목록 및 KSURE 연체율 provider가 `cache`, `stale cache`, 또는 key 구성 시 `live`로 설명 가능해야 한다.
- `scripts/check-risk-gates.sh`가 provider enum, connector, test, source catalog, 문서 matrix, prompt를 모두 확인해야 한다.
- 진단 detail payload는 `evidenceRetrieval`, `evidenceGuardrails`, `sourceTrace`, `llmFallbackReasons`, `gateDecision`, `claimVerifications`를 보존해야 한다.
- `gateDecision.llmInfluencedGate=false`가 유지되어야 한다.
- fallback/source-less claim이 positive contribution 또는 확정 문장으로 승격되면 실패 처리한다.
