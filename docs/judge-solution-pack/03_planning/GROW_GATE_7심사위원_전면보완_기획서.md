# GROW-GATE 7명 심사위원 지적사항 전면 보완 기획서

## 0. 핵심 원칙
첫 보완 포인트는 실제 고객/기업 데이터 부재입니다. 다만 본 패키지는 실제 고객 데이터를 위장하지 않고, 실제 업무 흐름을 모사한 **합성 PoC 데이터**를 먼저 투입합니다. 합성 데이터는 `sourceType=seed`, `synthetic=true`, `synthetic_demo_not_real_customer` 라벨을 유지하고, 실제 공공 API live proof나 실제 고객 계약/인터뷰로 발표하지 않습니다.

## 1. 이번 보강의 한 줄 정의
GROW-GATE를 “중소 제조기업 3개 페르소나 기반 고신뢰 합성 데이터 + 공공데이터 live proof + ML 예측/SHAP 해석 + source-grounded Agent 보고서” 구조로 바꿔, 7명의 심사위원이 지적한 공공데이터·AI·사업화·UX·시연 재현성 약점을 한 번에 막습니다.

## 2. 투입할 3개 합성 기업
| 기업 | 지역/업종 | 제품/HS | 대상국 | 기준 Gate | 핵심 보완 포인트 |
|---|---|---|---|---|---|
| 창원에너지팩 주식회사 [합성] | 경남 창원 / 이차전지 부품 | 산업용 리튬이온 배터리팩 / 850760 | Germany/EU | HOLD | CE/RoHS/REACH, FTO medium, 리콜/운송시험 |
| 부산마린컨트롤 주식회사 [합성] | 부산 강서 / 조선기자재 | 선박용 스마트 전동밸브 액추에이터 / 848180 | United States | CONDITIONAL_GO | UL/FCC/NRTL, 선급 pre-check, 미국 신규 바이어 |
| 대구메디센서 주식회사 [합성] | 대구 수성 / 디지털헬스 | 웨어러블 체온·활력징후 모니터 / 902519 | Japan | STOP | PMDA, QMS, 개인정보, FTO high |

## 3. 심사위원별 해결 매핑
### 1) 산업부 정책/공모전 적합성
- 지역·업종·산단·지원기관 매칭을 `regional_industry_profiles` 테이블로 분리합니다.
- 합성 지역 프로필은 seed로 표시하고, 실제 제출 전 공식 지역/산업 공공데이터로 교체 가능하게 `public_source_name`, `source_period`, `refresh_cycle_hours`를 둡니다.
- S10에는 `seed`와 `live/cache`를 분리 표시합니다.

### 2) 공공데이터 활용성
- 합성 데이터는 public API proof가 아닙니다. `operationEndpoint=false`, `apiKeyConfigured=false`, `endpointType=synthetic-not-live`를 고정합니다.
- Codex는 실제 provider endpoint 승인 후 `/api/public-data/health`와 S10 Proof Card에 `live/cache`, `operationEndpoint`, `apiKeyConfigured`, `raw_response_hash`를 표시하도록 구현합니다.
- 공공데이터 기여는 `rule_score_baseline`, `public_data_delta`, `final_score`로 분리합니다.

### 3) AI/ML/Agent 기술
- Agent: 보고서 초안, 문서 분류, 감정분석, 고급 검색, SHAP 해석 보완을 담당합니다.
- ML: Gate 분류, 지연일수 예측, 견적 검토시간 예측, 지원사업 매칭 분류를 담당합니다.
- SHAP: ML 예측 결과의 top feature를 산출하고, Agent가 source-bound evidence와 결합해 사람이 읽는 설명으로 보완합니다.
- LLM은 `llmInfluencedGate=false` 정책을 유지합니다.

### 4) 백엔드/API/보안
- `agent_trace_events`, `ml_prediction_runs`, `synthetic_data_registry`, `regional_industry_profiles`를 추가합니다.
- one-command demo는 seed 적용, API health, public data retry, diagnosis result 확인까지 자동화합니다.
- synthetic flag와 secret redaction을 테스트합니다.

### 5) 프론트엔드/UX
- `LoadDemoScenarioButton`으로 3개 기업 시나리오를 즉시 선택하게 합니다.
- S10에 “seed는 live proof가 아님” 경고와 실제 live proof checklist를 표시합니다.
- S11 또는 S10 Agent Trace 탭에 agent timeline과 ML/SHAP card를 추가합니다.

### 6) 사업화/현장성
- 3개 기업 페르소나별 바이어 RFQ, 보유문서, 누락문서, 견적 전 업무 보드를 제공합니다.
- 실제 사업화 검증은 합성 데이터가 아니라 `company_validation_interviews`로 별도 수집합니다.
- ROI 수치는 `estimateType=rule_based_assumption_on_synthetic_data`, `confidence=low_until_real_customer_validation`로 표시해 과장 리스크를 줄입니다.

### 7) 발표/시연/문서 완성도
- 최종 제출 ZIP에서 legacy/mock/artifact를 제거하고 `SUBMISSION_SCOPE.md`를 둡니다.
- 발표 데모 계정에는 3개 기업·제품·진단 결과가 seed되어 3분 안에 보여줄 수 있습니다.
- AI/ML/RAG 표현은 실제 구현 수준으로 제한합니다.

## 4. AI 서비스 설계안
### Agent 담당 기능
| Agent | 역할 | 출력 |
|---|---|---|
| DocumentClassifierAgent | 위키/정책/상품문서 자동 분류 | 문서 유형, 제품 연결, missing field |
| SentimentAgent | 바이어 RFQ의 긴급도·압박·불확실성 분석 | urgency, negotiation risk, due date pressure |
| AdvancedSearchAgent | 제품 지식·공공데이터·문서 chunk 고급 검색 | topK evidence, source ids |
| ReportAgent | 보고서/초안/바이어 회신문 작성 | Markdown report, buyer reply draft |
| ShapInterpretationAgent | SHAP 결과를 업무 언어로 보완 | 원인 설명, 보완 액션, limitation |
| VerifierAgent | 출처 없는 claim 차단 | unsupported claim warning |

### ML 담당 기능
| 모델 | 입력 | 출력 | 해석 |
|---|---|---|---|
| GateClassifier | 인증 gap, FTO risk, fallback count, 문서 완성도, 납기압박 | GO/CONDITIONAL_GO/HOLD/STOP | Tree SHAP top features |
| DelayRegressor | 인증 gap, 대상국, FTO risk, 배송기한, 바이어 복잡도 | delay_risk_days | SHAP waterfall |
| QuoteHoursRegressor | 문서 수, 누락 수, 바이어 긴급도 | quote_review_hours | SHAP summary |
| SupportMatcher | 지역·업종·지원기관·인증 gap | support_match_label | SHAP bar |

## 5. 구현 Definition of Done
- 3개 합성 기업 seed SQL 적용 후 `/diagnosis` 목록에서 3개 진단 결과가 보입니다.
- 화면 어디에도 합성 데이터를 live 공공데이터로 표시하지 않습니다.
- `ml_training_synthetic_cases.csv`로 ML 학습 스크립트가 돌아가고 모델 artifact와 SHAP json이 생성됩니다.
- Agent는 SHAP 값을 읽어 보고서 초안과 바이어 회신문에 설명을 보완합니다.
- 실제 live 공공데이터는 별도 API key/endpoint 설정 후 S10 Proof Card에서만 live/cache로 표시됩니다.