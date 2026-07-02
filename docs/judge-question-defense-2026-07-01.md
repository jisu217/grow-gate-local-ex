# 심사 질문 20개 방어 답변표

작성일: 2026-07-01

| # | 질문 | 강화된 답변 | 근거 |
| --- | --- | --- | --- |
| 1 | 산업통상부 산하 공공데이터를 실제로 사용했습니까? | 산업부 직접 산하 API만으로 한정하지 않고 관세청, KOTRA, SafetyKorea/KATS 계열, KIPRIS, Bizinfo를 provider로 분리했다. key와 operation endpoint가 있으면 `live`, 없으면 `fallback`으로 저장한다. | `publicdata/service/*Connector.java` |
| 2 | 배포 서버에서 최근 수집한 관세청 API 원문을 보여줄 수 있습니까? | `/api/public-data/records`가 사용자 scope 내 raw/normalized record를 반환한다. smoke script가 public-data runs와 count를 출력한다. | `scripts/smoke-prod.sh`, `PublicDataRecordResponse` |
| 3 | 관세청 데이터의 어떤 필드가 시장성 점수에 들어갑니까? | `importAmountUsd`, `exportAmountUsd`, `yoyGrowthPct`, `recordCount`를 파싱하고 이 metric으로 contribution/confidence를 계산한다. | `KcsTradeConnectorTest` |
| 4 | KOTRA connector가 왜 `usedFor="support"`입니까? | 수정 완료. KOTRA market connector는 `usedFor="market"`이며 국가/제품 mention, article count, growth signal을 시장성에 반영한다. | `KotraMarketConnector.java` |
| 5 | SafetyKorea 리콜 건수는 어디서 파싱합니까? | `recallCount`, `highRiskCount`, `recordCount`를 normalized payload metrics에 저장하고 recall score contribution에 반영한다. | `SafetyRecallConnector.java` |
| 6 | KIPRIS 특허 검색 결과 몇 건이 FTO 점수에 반영됩니까? | `patentCount`, `claimsCount`, `productMentions`를 metrics로 저장하고 patent count가 높을수록 FTO contribution을 낮춘다. | `KiprisFtoConnector.java` |
| 7 | 이 점수 가중치는 누가 검증했습니까? | 수출 실무 검토 순서 기준 rubric으로 문서화했고 10개 제품 시나리오로 기대 점수대와 위험 방향성을 검증한다. 전문가 sign-off 표를 별도로 받는다. | `docs/scoring-rubric-validation-2026.md`, `docs/user-validation-2026.md` |
| 8 | XGBoost 모델 파일과 학습 데이터는 어디 있습니까? | 메인 앱은 XGBoost를 주장하지 않는다. UI는 readiness scoring으로 바꿨고 실제 라벨 ML은 후속 과제로 분리했다. | `S03PredLab.jsx`, `diagnosisSteps.js` |
| 9 | RAG 검색에서 top-k chunk를 보여주세요. | `ragRetrieval.topK`에 `chunkId`, `score`, `sourceId`, `usedSentence`가 저장된다. 현재는 pgvector가 아니라 DB chunk 기반 BM25-lite retrieval이다. | `AgentRagContextBuilder`, `DiagnosisAsyncProcessor` |
| 10 | Agent가 실패하면 어떤 계획을 다시 세웁니까? | `AgentLoop`가 collect/score/roadmap/FastAPI/LLM/report/source verification 계획을 만들고, tool verifier 실패 시 재시도 후 deterministic fallback을 기록한다. | `DiagnosisAsyncProcessor.AgentLoop` |
| 11 | FastAPI Agent가 배포 서버에서 켜져 있습니까? | 기본은 optional bridge다. `APP_FASTAPI_ENABLED=true`일 때만 `/analyze`, `/report`를 호출하고, 비활성 시 Spring 결과로 degrade한다. | `FastApiAgentClient`, `agentTrace` |
| 12 | S04 시장성 화면은 backend API 값입니까? | 예. `stageCards.s04~s07`을 backend에서 생성하고 frontend는 해당 값을 매핑한다. S04~S07 mock fallback import를 제거했다. | `DiagnosisAsyncProcessor`, `diagnosisService.js` |
| 13 | 이 리포트는 PDF입니까? | 제출 시연 기본 산출물은 UTF-8 Markdown 바이어 대응 메모다. 한글 폰트 임베딩이 검증되지 않은 PDF endpoint는 제거했고, PDF는 후속 font QA gate를 통과한 뒤에만 다시 노출한다. | `DiagnosisController`, `docs/pdf-rendering-strategy-2026-07-02.md` |
| 14 | 사용자 A가 사용자 B의 public data runs를 볼 수 있습니까? | scope 제한을 추가했다. 개인은 본인 run만, 회사 사용자는 같은 company membership 사용자 run만 조회한다. | `PublicDataCollectionService.scopeUserIds()` |
| 15 | 로그아웃한 JWT는 서버 재시작 후에도 무효입니까? | 예. 로그아웃 토큰은 원문이 아니라 SHA-256 hash로 `revoked_tokens`에 저장되어 서버 재시작 후에도 차단된다. | `TokenRevocationService`, `RevokedToken` |
| 16 | XSS로 localStorage token이 털리면 어떻게 합니까? | 기존 Bearer 호환은 유지하지만 로그인 시 HttpOnly cookie도 내려주고 필터가 cookie token을 우선 인증 경로로 처리한다. 제출 서버는 `APP_AUTH_COOKIE_SECURE=true`로 운영한다. | `AuthController`, `JwtAuthenticationFilter`, `apiClient.js` |
| 17 | 발표장 인터넷이 느리면 Gradle/npm 빌드가 됩니까? | `scripts/verify-local.sh`, GitHub Actions CI, `BUILD.md`로 재현 명령을 고정했다. 완전 오프라인 빌드는 dependency cache가 필요하다. | `BUILD.md`, `.github/workflows/ci.yml` |
| 18 | 사용자 테스트 결과가 있습니까? | 3 persona, 10 제품 시나리오, 전/후 시간 측정표, 전문가 sign-off template을 준비했다. 실제 인터뷰 완료 주장은 별도 증빙 확보 후만 한다. | `docs/user-validation-2026.md` |
| 19 | 공공데이터 fallback 비율은 몇 %입니까? | run마다 `successCount`, `fallbackCount`, `cacheHitCount`가 있고 smoke script가 이를 출력한다. | `public_data_collection_runs`, `scripts/smoke-prod.sh` |
| 20 | 기존 수출 컨설팅/체크리스트와 다른 점은 무엇입니까? | 제품별 점수 산식, sourceType trace, provider metric, LLM 보조 추론, 담당자 지식 루프가 한 흐름으로 연결된다. | `docs/submission-95-final-hardening.md` |

## 남은 한계

- 실제 API key와 operation endpoint가 없는 provider는 `fallback`으로 남는다.
- 실제 라벨 기반 XGBoost와 pgvector embedding RAG는 제출 주장 범위에서 제외한다.
- refresh token rotation과 Redis revocation cache는 상용화 보안 과제로 남아 있다.
