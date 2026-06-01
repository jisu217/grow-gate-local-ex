# GROW-GATE Local EX Architecture

## 핵심 컨셉

GROW-GATE Local EX는 지역 제조기업의 제품 단위 해외진출 준비도를 진단하는 AI Agent 서비스입니다. 사용자는 지역, 업종, 제품명, HS Code, 대상국, 제품 특성, 보유 자료를 입력합니다. 에이전트는 시장성, 규제·인증, 리콜 위험, 특허/FTO, 지원사업을 분석하고 30·60·90일 실행 로드맵을 생성합니다.

## 화면 정보 구조

초기 프로토타입의 단일 대시보드 구조를 실무자 업무 흐름에 맞춰 다중 화면으로 분리했습니다.

| 화면 | 파일 | 역할 |
|---|---|---|
| 홈 | `frontend/index.html` | 담당자 인사, 최근 진단, 오늘 먼저 할 일, 주요 업무 진입 |
| AI 진단 워크스페이스 | `frontend/workspace.html` | 좌측 업무 메뉴 기반 기업·제품 입력, 데이터 호출, XGBoost 예측, 규제·리콜·FTO·지원사업 분석, AI 문맥 검색, 리포트 출력 |
| 데이터 파이프라인 | `frontend/data.html` | 화면별 공공데이터 출처, 정규화 테이블, evidence_source 흐름 확인 |
| 실행 로드맵 | `frontend/roadmap.html` | 30·60·90일 업무 보드, 담당자별 산출물, 예측 기반 운영 메모 |

업무 담당자는 홈에서 “새 수출 진단 시작”, “데이터 보기”, “오늘 할 일 보기” 중 하나를 선택합니다. 분석 업무는 `workspace.html`에서 수행하며, 좌측 업무 메뉴로 입력, 데이터 호출, ML 예측, 시장성, 규제·인증, 리콜, FTO·지원, 실행 보드, 리포트, 근거 데이터 섹션을 빠르게 이동합니다. 데이터 검증과 일정 관리는 별도 화면에서 확인하도록 분리했습니다.

## Agent 구성

| Agent | 역할 | 코드 위치 |
|---|---|---|
| PlannerAgent | 입력 제품에 필요한 분석 태스크 결정 | `backend/agents.py` |
| DataConnectorAgent | 무역·규제·리콜·특허·지원사업 데이터 수집 | `backend/agents.py`, `backend/data_connectors.py` |
| RAG Evidence Layer | 제품·국가·HS Code 메타데이터 필터와 텍스트 유사도로 근거 문서 검색 | `backend/rag.py` |
| AISearchService | 제품/run 문맥, RAG 근거, 공공·외부 데이터 카탈로그를 통합 검색하고 다음 액션을 제안 | `backend/ai_search.py` |
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
   - XGBoost 수집 카탈로그 41개 후보: 공공데이터 21개, 외부데이터 20개

2. 정규화
   - HS Code prefix
   - 대상국/권역
   - 제품 특성 태그
   - 규제 trigger
   - 리콜 hazard/test/action
   - 지원사업 fit_tags
   - XGBoost feature_candidates, model_targets, join_keys, leakage_guardrail

3. 인덱싱
   - 규제 문서: Regulation RAG용 문서로 변환
   - 리콜 문서: 유사 리콜 검색용 문서로 변환
   - 특허 문서: FTO 키워드와 IPC 근거 문서로 변환
   - 지원사업 문서: 조건 매칭 및 신청 초안 생성용 문서로 변환

4. 분석
   - Market score
   - Regulation passport
   - Recall similarity
   - FTO congestion
   - Support matching
   - Readiness score
   - XGBoost/Gradient Boosting prediction
   - 모델별 피처 매핑: 90일 파일럿 실행, 인증·시험 리드타임, 지원사업 선정 적합, 시험·보완 예비비용, 통합 Feature Importance

5. 산출물
   - 홈/AI 진단/데이터/로드맵 화면
   - 실행 로드맵
   - JSON 결과
   - 인쇄 가능한 보고서

## 제품 등록 및 업데이트 파이프라인

워크스페이스에서 FastAPI 백엔드를 켜면 화면 입력값은 즉시 분석되지 않고 다음 순서로 처리됩니다.

1. `POST /products`
   - 제품 마스터, HS Code, 대상국, 제품 특성, BOM/시험성적서/라벨 등 문서 준비 상태를 등록합니다.
   - 응답에는 `product_id`와 `document_readiness_score`가 포함됩니다.

2. `POST /analysis/run`
   - 등록된 `product_id`를 기준으로 Market, Regulation, Recall, FTO, Support, Roadmap Agent를 실행합니다.
   - run 결과는 `run_id`로 저장되며, 화면은 `run.result`를 렌더링합니다.

3. `GET /analysis/{run_id}`
   - 저장된 진단 결과를 다시 조회합니다. 향후 PDF, 알림, 이력 비교가 이 ID를 기준으로 연결됩니다.

4. `POST /ai-search`
   - `query`, `scope`, `product_id`, `analysis_run_id`를 받아 현재 제품/run 문맥에서 진단 결과, RAG evidence, XGBoost 데이터 카탈로그를 검색합니다.
   - 응답은 `answer`, `results`, `evidence_documents`, `data_sources`, `suggested_actions`, `guardrails`를 포함합니다.
   - `scope=diagnosis`는 현재 AI 진단 근거를 우선하고, `scope=data_catalog`는 외부/공공 데이터 후보를 우선하며, `scope=global`은 두 영역을 함께 검색합니다.

5. `POST /updates/impact`
   - 규제, 리콜, 특허, 지원사업 데이터 변경 이벤트가 들어오면 대상국, HS Code prefix, 제품 특성으로 영향받는 제품을 찾습니다.
   - 영향 제품이 있으면 `recommended_action`을 `reanalyze`로 반환해 재진단 큐에 넣을 수 있게 합니다.

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

`regulations`, `recalls`, `patents`, `support_programs`의 각 항목은 `evidence_documents`를 포함합니다. 화면은 이 값을 근거 문서 배지로 표시하고, `raw_signals.rag_context`는 섹션별 RAG 검색 결과 묶음을 보관합니다.

`POST /ai-search`는 진단 화면의 AI 검색 패널이 사용하는 통합 검색 응답입니다.

- `results`: 분석 run, 제품 마스터, RAG evidence, 데이터 출처, 로드맵, 지원사업을 같은 카드 계약으로 반환
- `evidence_documents`: 규제·리콜·특허·지원사업 RAG 문서 상위 결과
- `data_sources`: XGBoost 수집 카탈로그와 공공데이터 출처 후보
- `suggested_actions`: 근거 섹션 이동, 제품 재분석, 데이터 명세 화면 이동, 로드맵 확인 등의 화면 액션

`GET /xgboost-data/catalog`는 `xgboost_data_collection_catalog.xlsx`에서 정규화한 데이터 명세를 반환합니다.

- `sources`: 41개 공공/외부 데이터 수집 후보
- `feature_mappings`: XGBoost 화면 카드 5개와 연결되는 피처/라벨/누수 방지 기준
- `collection_plan`: raw → staging → feature store → label → train set → model registry → 운영 갱신 8단계
- `internal_labels`: 외부데이터만으로 만들 수 없는 7개 내부 라벨 요구사항
- `session_playbooks`: 엑셀 변경, 외부 데이터 수집, 공공데이터 강화 작업을 별도 세션으로 공유하기 위한 복사형 프롬프트와 완료 기준

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
- `/ai-search`의 lexical ranking을 임베딩/vector DB, 사용자별 분석 이력, 클릭 로그 기반 reranking으로 확장
- 전문가 상담 연계 워크플로우
- 기업별 데이터 저장, 이력 관리, 권한 관리
- 업데이트 이벤트 기반 재분석 큐와 알림
- 지원사업 신청서 초안 자동 생성
- PDF 리포트 생성과 기관 관리자 대시보드
