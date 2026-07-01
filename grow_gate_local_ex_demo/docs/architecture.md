# GROW-GATE Local EX Architecture

## 핵심 컨셉

GROW-GATE Local EX는 지역 제조기업의 제품 단위 해외진출 준비도를 진단하는 AI Agent 서비스입니다. 사용자는 지역, 업종, 제품명, HS Code, 대상국, 제품 특성, 보유 자료를 입력합니다. 에이전트는 시장성, 규제·인증, 리콜 위험, 특허/FTO, 지원사업을 분석하고 30·60·90일 실행 로드맵을 생성합니다.

## 화면 정보 구조

초기 프로토타입의 단일 대시보드 구조를 실무자 업무 흐름에 맞춰 다중 화면으로 분리했습니다.

| 화면 | 파일 | 역할 |
|---|---|---|
| 홈 | `frontend/index.html` | 담당자 인사, 최근 진단, 오늘 먼저 할 일, 주요 업무 진입 |
| AI 진단 워크스페이스 | `frontend/workspace.html` | 좌측 업무 메뉴 기반 기업·제품 입력, 근거 데이터 연계, XGBoost 예측, 규제·리콜·FTO·지원사업 분석, 리포트 출력 |
| AI 검색 | `frontend/ai-search.html` | 제품 문맥 기반 진단 결과, RAG-ready 근거 문서, 공공데이터 출처 통합 검색 |
| 데이터 파이프라인 | `frontend/data.html` | 화면별 공공데이터 출처, 정규화 테이블, evidence_source 흐름 확인 |
| 실행 로드맵 | `frontend/roadmap.html` | 30·60·90일 업무 보드, 담당자별 산출물, 예측 기반 운영 메모 |

업무 담당자는 홈에서 “새 수출 진단 시작”, “데이터 보기”, “오늘 할 일 보기” 중 하나를 선택합니다. 분석 업무는 `workspace.html`에서 수행하며, 좌측 업무 메뉴로 입력, 근거 데이터 연계, 준비도 추정, 시장성, 규제·인증, 리콜, FTO·지원, 실행 보드, 리포트, 근거 데이터 섹션을 빠르게 이동합니다. 질문형 근거 탐색은 `ai-search.html`에서 별도로 수행합니다. 데이터 검증과 일정 관리는 별도 화면에서 확인하도록 분리했습니다.

## Agent 구성

| Agent | 역할 | 코드 위치 |
|---|---|---|
| PlannerAgent | 입력 제품에 필요한 분석 태스크 결정 | `backend/agents.py` |
| DataConnectorAgent | 무역·규제·리콜·특허·지원사업 데이터 수집 | `backend/agents.py`, `backend/data_connectors.py` |
| AISearchService | 제품 입력값으로 진단 결과, RAG-ready 문서, 공공데이터 출처를 통합 검색 | `backend/ai_search.py` |
| TargetCompanyEnhancement | 피엠그로우 SME 타겟 프로필을 RAG 근거, 고급 프롬프트 팩, 하네스 평가로 변환 | `backend/target_company.py`, `data/target_company_pmgrow.json` |
| ExternalLLMClient | Hugging Face Space, Groq, Gemini 외부 추론 provider를 선택적으로 호출하고 실패 시 규칙 기반 요약으로 fallback | `backend/external_llm.py` |
| MarketScoringAgent | 수입 증가율, 관세, 제품 태그 적합도를 기반으로 수출기회 점수 산정 | `backend/agents.py`, `backend/ml_models.py` |
| RegulationAgent | HS Code, 제품 특성, 대상권역 기반 인증·규제 후보 매칭 | `backend/agents.py`, `backend/ml_models.py` |
| RecallAgent | 제품명·특성 기반 유사 리콜 위험 탐색 | `backend/agents.py`, `backend/ml_models.py` |
| FTORiskAgent | 특허 키워드·IPC·출원밀집도 기반 FTO 위험 산정 | `backend/agents.py`, `backend/ml_models.py` |
| SupportMatchingAgent | 현재 갭 프로필과 지원사업 태그를 비교해 우선순위 추천 | `backend/agents.py`, `backend/ml_models.py` |
| XGBoostPredictionAgent | 90일 수출 실행 가능성, 인증 리드타임, 지원사업 적합 확률, 예비비율 추정 | `backend/agents.py`, `backend/ml_models.py` |
| RoadmapAgent | 30·60·90일 실행 태스크와 산출물 생성 | `backend/agents.py` |
| ReportAgent | 심사위원에게 보여줄 수 있는 자연어 요약 생성 | `backend/agents.py` |
| GuardrailAgent | 전문가 확인 필요성, 출처/버전 표시 원칙 등 안전장치 제공 | `backend/agents.py` |

## 데이터 파이프라인

1. 원천 데이터 수집
   - 무역/수출입 추세
   - 국가·시장 정보
   - 해외 인증·규제 정보
   - 제품안전·리콜 정보
   - 특허·실용신안 정보
   - 지원사업 공고

2. 정규화
   - HS Code prefix
   - 대상국/권역
   - 제품 특성 태그
   - 규제 trigger
   - 리콜 hazard/test/action
   - 지원사업 fit_tags

3. 인덱싱
   - 규제 문서: Regulation RAG용 문서로 변환
   - 리콜 문서: 유사 리콜 검색용 문서로 변환
   - 지원사업 문서: 조건 매칭 및 신청 초안 생성용 문서로 변환
   - 타겟 기업 문서: 공개 기업정보, 중소기업 실행 제약, 수출 후보 제품군을 Agent 프롬프트용 seed 문서로 변환

4. 분석
   - Market score
   - Regulation passport
   - Recall similarity
   - FTO congestion
   - Support matching
   - Readiness score
   - XGBoost/Gradient Boosting prediction
   - SME Target RAG retrieval
   - Advanced prompt pack assembly
   - Optional external LLM inference through Hugging Face Space, Groq, or Gemini
   - Harness evaluation for evidence grounding, prompt injection defense, SME fit, source traceability

5. 산출물
   - 홈/AI 진단/데이터/로드맵 화면
   - 실행 로드맵
   - JSON 결과
   - 인쇄 가능한 보고서

## API 응답 구조

`POST /analyze`는 다음 필드를 반환합니다.

- `readiness_score`
- `readiness_level`
- `score_breakdown`
- `markets`
- `regulations`
- `recalls`
- `patents`
- `support_programs`
- `roadmap`
- `ml_predictions`
- `ai_summary`
- `guardrails`
- `raw_signals`

`raw_signals`에는 Agent 내부 추론 감사용 값이 포함됩니다.

- `target_company_profile`: 피엠그로우 중소기업 컨설팅 프로필
- `target_company_rag`: 기업 seed, 규제, 리콜 문서의 top-k 검색 결과
- `advanced_prompt_pack`: 실제 LLM 연결 시 사용할 시스템/개발자/사용자/context 메시지와 출력 계약
- `harness_report`: 근거 연결, 프롬프트 인젝션 방어, SME 정렬, 출처 추적성, 실행성 점검 결과
- `llm_inference`: 외부 추론 provider, 모델명, 응답 상태, fallback 여부, latency/error

## 외부 추론 Provider

기본 설정은 `GROW_GATE_LLM_PROVIDER=disabled`입니다. 이 상태에서는 네트워크 호출 없이 규칙 기반 `ReportAgent` 요약이 사용됩니다. 외부 무료/오픈모델 추론을 시연할 때만 provider를 켭니다.

| Provider | 환경변수 | 용도 |
|---|---|---|
| Hugging Face Space | `GROW_GATE_LLM_PROVIDER=hf_space`, `GROW_GATE_HF_SPACE_URL` | Space/ZeroGPU 계열 오픈소스 LLM 데모 API |
| GroqCloud | `GROW_GATE_LLM_PROVIDER=groq`, `GROQ_API_KEY`, `GROW_GATE_GROQ_MODEL` | Llama/Gemma 계열 빠른 데모 추론 |
| Gemini | `GROW_GATE_LLM_PROVIDER=gemini`, `GEMINI_API_KEY`, `GROW_GATE_GEMINI_MODEL` | 무료 API 한도 기반 문장 생성 실험 |

외부 호출이 실패하거나 quota/cold start/timeout이 발생하면 `llm_inference.used_fallback=true`로 기록하고 기존 deterministic 요약을 반환합니다.

`POST /ai-search`는 별도 AI 검색 화면의 통합 검색 응답입니다.

- `answer`: 제품 문맥 기준 자연어 검색 요약
- `results`: 진단 요약, 규제 근거, 로드맵, 지원사업, 데이터 출처 카드
- `evidence_documents`: 규제·리콜 RAG-ready 문서 상위 결과
- `data_sources`: 공공데이터 출처 후보
- `suggested_actions`: 다시 검색, 데이터 명세 이동, 로드맵 이동 등의 화면 액션

`GET /target-company/context`는 기본 타겟 기업의 RAG, 프롬프트 팩, 하네스 리포트를 빠르게 확인하는 진단/QA용 엔드포인트입니다.

## 보안 구조

| 영역 | 적용 내용 |
|---|---|
| CORS | 기본 허용 origin을 `127.0.0.1`, `localhost`, 로컬 파일 시연용 `null`로 제한하고 credentials 전송을 비활성화 |
| 보안 헤더 | API 응답에 `X-Content-Type-Options`, `Referrer-Policy`, `X-Frame-Options`, `Cache-Control` 적용 |
| 입력 검증 | `ProductInput`에서 HS Code, 문자열 길이, 제품 특성 allow-list, 인증 목록 길이 검증 |
| Live 수집 | `GROW_GATE_ENABLE_LIVE_COLLECTION=1`이 없으면 live 공공데이터 수집 요청 차단 |
| 외부 URL | HTTPS와 허용된 공공데이터 호스트만 호출하고, private/localhost 주소로 해석되는 호스트는 거부 |
| 프론트 렌더링 | API/로컬 분석 결과를 HTML에 삽입할 때 escaping 적용, CSP 메타 정책 적용, 리포트 팝업 opener 분리 |

## 실제 사업화 시 추가해야 할 것

- 공공데이터 API별 인증키 관리와 호출량 제한 대응
- 데이터 수집 시각, 출처 URL, 문서 버전, 신뢰도 점수 표시
- 인증/규제 답변의 근거 문서 인용
- 전문가 상담 연계 워크플로우
- 기업별 데이터 저장, 이력 관리, 권한 관리
- 지원사업 신청서 초안 자동 생성
- PDF 리포트 생성과 기관 관리자 대시보드
