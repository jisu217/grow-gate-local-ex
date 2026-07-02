# 2026-07-02 21:00 이후 변경 감사 및 남은 보완

작성 시각: 2026-07-02 22:03 KST  
기준 시각: 2026-07-02 21:00 KST 이후 로컬 working tree 변경  
기준 자료:

- `/Users/jiyongkim/Downloads/grow_gate_codex_gstack_compounds_directive.md`
- `/Users/jiyongkim/Downloads/GROW_GATE_90점_달성_실행계획서.pdf`
- `/Users/jiyongkim/.codex/attachments/f562cf82-78cc-4b05-b2a2-715ad5340c95/pasted-text.txt`

## 결론

21시 이후 핵심 변경은 “AI Agent/RAG/XGBoost 과장” 리스크를 줄이고, 심사위원에게 방어 가능한 **Evidence Workflow**로 재정렬한 것이다. 기존 `RAG context` 중심 명칭을 `source-bound evidence retrieval`로 바꾸고, 진단 결과에 `evidenceRetrieval`, `exportKnowledgeTimeline`, `evidenceGuardrails`를 추가했다. 프론트 S10 화면도 단순 공공데이터 파이프라인이 아니라 출처, top-k evidence chunk, guardrail, timeline을 함께 보여주는 화면으로 확장했다.

다만 90점 실행계획서가 요구하는 모든 항목이 끝난 것은 아니다. 특히 live 공공 API 성공 증거, DB 영속형 external evidence schema, claim/source span verifier, Source Trust Bar, 실제 발표 서버 smoke log는 아직 추가 확인 또는 구현이 필요하다.

## 21시 이후 수정한 것

| 영역 | 수정 내용 | 주요 파일 |
| --- | --- | --- |
| Evidence retrieval 명칭 정리 | `AgentRagContextBuilder`를 `EvidenceContextBuilder`로 전환. `ragContext`를 `evidenceContext`로 변경. `retrievalMode=source-bound-evidence-search`, `claimBoundary`, `sourceBound`, `displayRole` 추가 | `backend/BE-02/src/main/java/com/growgate/agent/service/EvidenceContextBuilder.java`, `AgentInferenceRequest.java`, `AbstractLlmAgentProviderClient.java` |
| 진단 결과 payload 확장 | `evidenceRetrieval`, `exportKnowledgeTimeline`, `evidenceGuardrails` 추가. `ragRetrieval` 제거. public data source에 `publishedAt`, `sourcePeriodStart`, `sourcePeriodEnd`, `eventDate` 전달 | `DiagnosisAsyncProcessor.java`, `DiagnosisDetailResponse.java`, `DiagnosisService.java` |
| Workflow trace source 정직화 | optional FastAPI bridge 출처를 `FASTAPI-AGENT`/`FastAPI Agent`에서 `WORKFLOW_TRACE`로 변경하고 점수 기여도를 0으로 고정 | `DiagnosisAsyncProcessor.java` |
| S10 결과 화면 확장 | `S10 PUBLIC DATA PIPELINE`을 `S10 EVIDENCE WORKFLOW`로 변경. source card, top-k evidence chunk, evidence guardrails, export knowledge timeline 표시 | `frontend/FE-02/src/components/diagnosis/S10Sources.jsx`, `frontend/FE-02/src/styles/global.css` |
| 프론트 mapper 보강 | S10 stage를 `{ pipes, evidenceRetrieval, timeline, guardrails }` 구조로 매핑. `session`에 evidence 관련 필드 추가 | `frontend/FE-02/src/services/diagnosisService.js`, `diagnosisService.mapper.test.js` |
| 지식화 표현 수정 | `AI 지식화`를 `근거 지식화`로 변경. 담당자 승인형 self-loop wiki 문구와 지식화 아이디어 보강 | `KnowledgeDetail.jsx`, `KnowledgeService.java` |
| 과장 표현 방지 gate 강화 | risk gate가 mock 정적 import, presentation mock artifact, evidence workflow 필드, 금지 표현을 검사하도록 확장 | `scripts/check-risk-gates.sh` |
| 문서화 | evidence workflow 재설계 및 검증 결과 문서 추가 | `docs/evidence-workflow-redesign-2026-07-02.md` |

## 21시 이후 보완한 것

| 지시서/기획서 요구 | 보완 상태 | 설명 |
| --- | --- | --- |
| G3 presentation/production build에서 mock fallback 차단 | 보완 | `diagnosisService.js`, `productService.js`의 정적 mock import 제거 및 lazy import 구조. presentation/prod에서 mock fallback 차단. risk gate와 dist grep로 확인 |
| G5 AI/ML/RAG 과장 표현 제거 | 보완 | 메인 코드 경로에서 `ragRetrieval`, `AgentRagContextBuilder`, `AI 지식화`, `추론 Agent` 표현 제거 또는 gate 대상화 |
| Workflow agent trace와 source-bound guardrail | 부분 보완 | 기존 `AgentLoop`는 유지하고, 결과 payload에 `evidenceGuardrails`를 추가해 unsupported claim count, fallback positive count, official live/cache count를 노출 |
| LLM-Wiki식 source timeline | 부분 보완 | `exportKnowledgeTimeline`을 진단 결과에 추가. sourceId, sourceType, fetchedAt, publishedAt/sourcePeriod, scoreImpact 기반 이벤트 생성 |
| fallback positive contribution 방어 | 부분 보완 | 기존 scoring 정책은 fallback positive를 제한하고, 21시 이후 guardrail count로 추가 노출. 다만 별도 DB constraint나 전용 단위 테스트는 아직 부족 |
| 담당자 승인형 self-loop wiki | 보완 | 마이페이지 탭과 backend 지식 출처 documentName을 `근거 지식화`로 변경. 지식화 후보에 sourcePeriod/fetchedAt, error-book 아이디어 추가 |
| 제출 증거 문서 | 보완 | `docs/evidence-workflow-redesign-2026-07-02.md`에 변경 이유, 새 JSON 필드, 발표 문구, 검증 결과 정리 |

## 기준 자료별 충족도

### grow_gate_codex_gstack_compounds_directive.md 기준

| Compound | 현재 상태 | 21시 이후 반영 | 남은 작업 |
| --- | --- | --- | --- |
| Compound 1 productId Diagnosis E2E Fix | 부분 완료 | 이전 변경에서 productId 조건부 validation과 snapshot 저장 테스트가 들어간 상태 | 실제 배포 서버에서 저장 제품 -> 진단 -> polling -> sources smoke log 필요 |
| Compound 2 Public Data Trust Layer And Live Smoke | 부분 완료 | evidence timeline/guardrail 노출로 trust layer 설명력 보강 | live API key/operation endpoint 기반 성공 record, provider registry의 `agencyType`, `responseHash`, `dataDate`, `expiresAt` 영속화 추가 필요 |
| Compound 3 Presentation Mock Isolation | 완료에 가까움 | mock 정적 import 제거, presentation/prod mock 차단, risk gate/dist grep | 배포 artifact 기준 재검증 필요 |
| Compound 4 Score Contribution Ledger And Source Impact UI | 부분 완료 | S10에 source impact, top-k chunk, guardrail, timeline 추가 | 별도 Source Trust Bar, raw/normalized/final dimension delta waterfall은 아직 부족 |
| Compound 5 Honest AI, Agent Trace, LLM Guardrail | 부분 완료 | RAG/Agent 과장 명칭 제거, `EvidenceContextBuilder`, `evidenceGuardrails` 추가 | LLM claim별 `sourceIds` subset verifier와 prohibited claim drop 테스트는 별도 구현 필요 |
| Compound 6 Confidence Panel, Expert Review, Report Fallback | 부분 완료 | 기존 confidence panel/gate decision 유지, 과장 문구 일부 정리 | PDF 한글 renderer 또는 PDF 버튼 hide 정책 재확인 필요 |
| Compound 7 Evidence Pack, Docs, Reproducibility | 부분 완료 | README, env example, judge defense, field mapping, evidence workflow 문서와 risk gate 보강 | 발표 서버 smoke 결과 문서, screenshot/log artifact, backup video 링크 필요 |

### 90점 실행계획서 기준

| 90점 승부처 | 현재 판단 | 근거 |
| --- | --- | --- |
| Live 공공데이터 증명 | 아직 부족 | 구조와 smoke gate는 있으나, 발표 서버에서 live provider 성공 record를 직접 캡처한 문서는 아직 없음 |
| 진단 생성 안정화 | 보완됨, 배포 검증 필요 | productId 조건부 validation/test가 들어갔고 backend test 통과. 실제 Lightsail E2E smoke는 별도 필요 |
| Mock 격리 | 보완됨 | presentation build에서 mock key grep 미검출, risk gate 통과 |
| 공공데이터 점수 기여도 공개 | 부분 보완 | `metricSummary`, `decisionImpact`, `confidenceAdjustedContribution`, S10 card/timeline은 있음. raw/normalized/final dimension waterfall은 추가 필요 |
| 정직한 AI | 보완됨 | `RAG context` 명칭 제거, `source-bound evidence retrieval`로 변경, `AI 지식화` 제거 |
| 사업화/책임 범위 | 부분 보완 | confidence panel과 전문가 확인 기조는 있으나 review ticket workflow는 아직 부분적 |
| 발표/문서 완성도 | 부분 보완 | evidence docs와 risk gate는 있음. 발표용 smoke log와 캡처 자료는 아직 필요 |

### 첨부 심사평가/재설계안 기준

| 요구 | 현재 상태 | 설명 |
| --- | --- | --- |
| “근거 지식화 기반 수출 의사결정 Workflow Trace 서비스”로 포지셔닝 | 보완 | README, S10, backend payload, 마이페이지 문구를 이 방향으로 정리 |
| Source Registry & Raw Store | 부분 | 기존 public data record/run 구조는 있으나, 별도 `external_source_document` 영속 schema는 아직 없음 |
| Claim / Metric Extractor | 부분 | provider parser와 normalized payload 기반 metric은 존재. claim/source span 단위 extractor는 아직 없음 |
| Temporal Knowledge Compiler | 부분 | `exportKnowledgeTimeline`은 생성하지만 compiled wiki page/log/error-book 영속화는 아직 없음 |
| In-memory Evidence Retrieval | 보완 | `EvidenceContextBuilder`가 DB 지식/public data/accepted loop chunk를 top-k로 반환 |
| Verifiable Workflow Agent Trace | 부분 | `AgentLoop` plan/tool/verifier/retry/fallback trace는 있음. UI 전용 AgentTracePanel은 아직 제한적 |
| error-book.md 방식 | 문서/아이디어 수준 | 지식화 아이디어와 guardrail 문서화는 했지만 `error-book.md` 파일/서비스는 아직 없음 |

## 현재 검증 결과

21시 이후 변경에 대해 확인한 결과:

| 검증 | 결과 |
| --- | --- |
| `./scripts/check-risk-gates.sh` | 통과 |
| `cd frontend/FE-02 && npm run test:mappers` | 통과 |
| `cd frontend/FE-02 && npm run build` | 통과 |
| `VITE_APP_MODE=presentation VITE_ENABLE_MOCK=false VITE_PRESENTATION_MODE=true npm run build` | 통과 |
| presentation dist grep | `CURRENT_SESSION`, `KPI_DATA`, mock stage key, `AI 지식화`, `추론 Agent` 미검출 |
| `cd backend/BE-02 && ./gradlew test --no-daemon` | 통과 |
| dev server HTTP smoke | `/diagnosis/diag-empty` 200 |
| Playwright screenshot QA | macOS sandbox Mach port 권한으로 차단 |

참고: `find -newermt '2026-07-02 21:00:00'` 기준으로 `.gradle` 파일도 수정됐지만, 이는 test 실행으로 생긴 build metadata라 의도 변경에서 제외한다.

## 아직 수정이 필요한 것

### P0: 발표 전 반드시 확인

1. **Lightsail/배포 서버 live public data smoke**
   - `PUBLIC_DATA_ENABLED=true`
   - provider별 endpoint/key 설정
   - `REQUIRE_LIVE_PUBLIC_DATA=true`
   - smoke 결과에 `diagnosisId`, `collectionRunId`, `liveCount`, `cacheCount`, `fallbackCount`, `sourceIds` 캡처

2. **Source Trust Bar**
   - 결과 상단에 run id, createdAt, live/cache/fallback count, MOTIE-related contribution summary를 바로 표시
   - 지금은 S10에서 자세히 볼 수 있지만, 첫 화면 방어력은 아직 약함

3. **Score Contribution Ledger 완성**
   - rawMetric, normalizedMetric, rawContribution, confidence, adjustedContribution, finalScoreDelta를 table/waterfall로 표시
   - 현재는 `metricSummary`, `decisionImpact`, confidence-adjusted contribution 중심

4. **LLM claim verifier 테스트**
   - sourceId 없는 claim 제거
   - 존재하지 않는 sourceId 제거
   - 임의 수치 claim 제거
   - “FTO 문제 없음” 같은 확정 문장 제거

### P1: 90점권 방어력 강화

1. **external evidence schema**
   - `external_source_document`, `evidence_claim`, `temporal_event`, `knowledge_page`, `agent_trace_step` 등은 아직 설계안 수준
   - 지금 구현은 기존 diagnosis result JSON과 public data record 기반

2. **compiled wiki/log/error-book**
   - `log.md`, `error-book.md`, country/hs/industry wiki page 생성은 아직 없음
   - 단기적으로는 `docs/evidence-workflow-redesign-2026-07-02.md`와 마이페이지 지식화 후보로 설명 가능

3. **PDF 전략 정리**
   - Markdown report는 안정적
   - PDF는 한국어 renderer/폰트 검증 또는 발표에서 숨기는 정책 필요

4. **발표 evidence pack**
   - smoke-prod 실제 결과 파일
   - source detail screenshot
   - fallback/live/cache dashboard screenshot
   - 5분 시연 시나리오 캡처

## 팀 공유용 요약

오늘 21시 이후 작업은 “기능 추가”보다 “심사 방어 가능한 구조로 명칭과 증거를 정리”한 작업이다. 가장 큰 변화는 RAG/AI 표현을 제거하고, backend result와 S10 UI를 `Evidence Workflow` 중심으로 바꾼 점이다. 이제 심사위원이 “RAG냐?”, “AI가 판단하냐?”, “fallback이 점수 올리냐?”, “top-k 근거 보여달라”고 물으면 S10 화면과 result JSON으로 답할 수 있다.

하지만 live 공공데이터 성공 record와 발표 서버 smoke 결과는 별도 산출물로 꼭 남겨야 한다. 이 부분이 없으면 코드 구조는 좋아졌어도 “실제 live 공공데이터 활용” 질문에서 다시 약해진다.
