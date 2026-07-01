# GROW-GATE Local EX Demo

**지역 제조기업 수출규제·인증 실행 AI 에이전트** 시연용 프로토타입입니다. 

이 패키지는 실무자가 쓰기 쉬운 다중 화면 정적 프론트엔드와, 같은 분석을 API로 제공하는 FastAPI 백엔드를 함께 포함합니다. 외부 API 키 없이 로컬 샘플 데이터로 실행되며, 실제 사업화 단계에서는 데이터 커넥터 부분을 산업부·무역·해외시장·시험인증·제품안전·특허·지원사업 공공데이터 API로 교체하면 됩니다. 

## 1. 폴더 구조

```text
grow_gate_local_ex_demo/
├─ frontend/
│  ├─ index.html          # 업무 담당자 홈
│  ├─ workspace.html      # AI 진단 워크스페이스
│  ├─ ai-search.html      # 진단/RAG-ready 근거/공공데이터 통합 AI 검색
│  ├─ data.html           # 공공데이터 파이프라인 화면
│  ├─ roadmap.html        # 30·60·90일 실행 로드맵 화면
│  ├─ styles.css          # Apple-light 업무 UI 스타일
│  └─ app.js              # 워크스페이스 로컬 JS 분석 엔진 + API 연동 코드
├─ backend/
│  ├─ app.py              # FastAPI 서버
│  ├─ agents.py           # Planner/Data/Market/Regulation/Recall/FTO/Support/Roadmap Agents
│  ├─ ai_search.py        # AI 검색 서비스
│  ├─ external_llm.py     # Hugging Face/Groq/Gemini 외부 추론 게이트웨이 + fallback
│  ├─ local_demo_db.py    # SQLite 데모 DB seed, RAG/ML/보고서 실행 이력 저장
│  ├─ operational_pipeline.py # Airflow-ready 공공데이터/RAG/ML 실행 파이프라인
│  ├─ product_knowledge.py    # 직원 제품 지식 등록, RAG pack, ML feature 변환
│  ├─ rag_database.py     # Supabase/pgvector-ready RAG DB 계약과 JSON preview
│  ├─ target_company.py   # 중소기업 타겟 RAG, 고급 프롬프트, 하네스 평가
│  ├─ data_connectors.py  # 공공데이터 커넥터 계층 및 ETL preview
│  ├─ ml_models.py        # 시장성, 규제 매칭, 리콜 유사도, FTO, 준비도 점수 모델
│  ├─ schemas.py          # API 입출력 스키마
│  └─ run_pipeline.py     # RAG 문서화용 데이터 파이프라인 미리보기
├─ data/
│  ├─ sample_datasets.json       # 시연용 샘플 데이터
│  ├─ public_data_sources.json   # 화면별 공공데이터 소스/테이블 매핑
│  ├─ target_company_pmgrow.json # 피엠그로우 SME 타겟 기업 RAG seed
│  ├─ grow_gate_demo.db          # 공모전 시연용 SQLite 데모 DB
│  ├─ product_knowledge_seed.json
│  └─ rag_database_preview_seed.json
├─ docs/
│  └─ architecture.md     # 서비스/기술 구조 설명
├─ tests/
│  └─ test_agent.py       # 기본 분석 테스트
├─ requirements.txt
└─ run_backend.py
```

## 2. HTML만 바로 시연하기

`frontend/index.html` 파일을 브라우저에서 열면 홈 화면이 표시됩니다. 업무별 화면은 상단 네비게이션에서 이동합니다.

- `index.html`: 담당자 인사, 최근 진단, 오늘 먼저 할 일, 주요 업무 진입
- `workspace.html`: 좌측 업무 메뉴 기반 제품 입력, AI/XGBoost 진단, 규제·리콜·FTO·지원사업·리포트
- `ai-search.html`: 제품 문맥 기반 AI 검색, RAG-ready 근거, 공공데이터 출처, 다음 액션 확인
- `data.html`: 화면별 공공데이터 출처, 정규화 테이블, 수집 파이프라인
- `roadmap.html`: 30·60·90일 업무 보드와 예측 기반 운영 메모

기본 시나리오:

- 지역: 경남
- 제품: 리튬이온 배터리팩
- HS Code: 850760
- 대상국: Germany / EU
- 제품 특성: 배터리, 전기전자, 산업재

`AI Agent 분석 실행` 버튼을 누르면 로컬 JavaScript 분석 엔진이 다음 결과를 생성합니다.

- 수출 준비도 점수
- 국가별 수출기회 카드
- 규제·인증 패스포트
- 유사 리콜 위험 및 시험 전 체크리스트
- 특허/FTO 위험 키워드
- 지원사업 매칭
- XGBoost 기반 90일 수출 실행 가능성, 인증 리드타임, 지원사업 적합 확률, 시험·보완 예비비율 추정
- 30·60·90일 실행 로드맵
- 인쇄 가능한 보고서

## 3. FastAPI 백엔드까지 실행하기

```bash
cd grow_gate_local_ex_demo
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python run_backend.py
```

프론트도 실제 웹앱처럼 HTTP로 띄우려면 별도 터미널에서 다음을 실행합니다.

```bash
python -m http.server 8766 -d frontend
```

그 다음 브라우저에서 아래 주소를 엽니다.

- 프론트: `http://127.0.0.1:8766/data.html`
- FastAPI 문서: `http://127.0.0.1:8000/docs`
- 상태 확인: `http://127.0.0.1:8000/health`

`8765` 포트가 비어 있으면 기존 로컬 시연 포트로도 사용할 수 있습니다. 충돌이 있으면 `8766`을 사용하면 됩니다. FastAPI CORS 기본 허용 목록에는 `127.0.0.1:8765`, `localhost:8765`, `127.0.0.1:8766`, `localhost:8766`이 포함되어 있습니다.

API 엔드포인트:

- `GET /health`
- `GET /datasets`
- `GET /pipeline/preview`
- `GET /pipeline/runs`
- `GET /pipeline/latest-status`
- `POST /db/seed`
- `GET /db/status`
- `POST /simulation/run`
- `GET /public-data/sources`
- `GET /public-data/normalized`
- `GET /rag/schema`
- `GET /target-company/context`
- `POST /public-data/collect`
- `POST /ml/train`
- `GET /product-knowledge`
- `POST /product-knowledge`
- `POST /analyze`
- `POST /ai-search`
- `POST /report`

예시 요청:

```bash
curl -X POST http://127.0.0.1:8000/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "company_name":"경남 배터리테크",
    "region":"경남",
    "industry":"전기전자",
    "product_name":"리튬이온 배터리팩",
    "hs_code":"850760",
    "target_market":"Germany / EU",
    "features":["battery","electronics","industrial"],
    "certifications":["KC","UN38.3"],
    "has_bom":true,
    "has_test_report":true,
    "has_label_draft":true
  }'
```

별도 AI 검색 화면(`frontend/ai-search.html`)은 `POST /ai-search`를 호출합니다.

공모전 데모에서 “실제 사용하는 것처럼” 보여줄 때는 `data.html`의 운영 패널을 사용합니다.

- `데모 DB 시드 생성`: `POST /db/seed`를 호출해 제품 지식, RAG 문서/청크/임베딩 preview, 파이프라인 실행, 모델 버전, 평가 결과를 SQLite DB에 저장합니다.
- `실사용 시뮬레이션 실행`: `POST /simulation/run`을 호출해 DB seed, AI 분석, AI 검색, 보고서 생성, 보고서 품질 검수, 실행 이력 저장까지 한 번에 수행합니다.
- `ML 재학습 실행`: `POST /ml/train`을 호출해 Airflow-ready task 단위의 공공데이터/RAG/ML 파이프라인 이력을 남깁니다.

현재 데모 DB는 `data/grow_gate_demo.db`에 생성됩니다. seed 데이터는 idempotent하게 갱신되며, 분석 보고서와 AI 검색 실행 이력은 실제 운영 로그처럼 누적됩니다.

```bash
curl -X POST http://127.0.0.1:8000/db/seed
curl http://127.0.0.1:8000/db/status
curl -X POST http://127.0.0.1:8000/simulation/run
```

시뮬레이션 결과는 기본 입력값 기준으로 준비도 점수, 검색 품질 전략, 보고서 검수 결과를 반환합니다. 검수 항목은 AI 요약, 근거 context, 30·60·90일 로드맵, 유의사항, 검색 품질 신호, 준비도 추정 포함 여부입니다.

AI 진단 워크스페이스에는 `Pmgrow Corporation(피엠그로우)`을 중소기업 타겟 사례로 둔 Agent 고도화 패널이 포함됩니다. 백엔드 분석 결과의 `raw_signals`에는 다음 패키지가 함께 반환됩니다.

- `target_company_profile`: 공개 기업정보 기반 SME 컨설팅 프로필
- `target_company_rag`: 기업 seed, 규제 RAG, 리콜 RAG 상위 근거
- `advanced_prompt_pack`: 시스템/개발자/사용자/context 메시지와 출력 계약
- `harness_report`: 근거 연결, 프롬프트 인젝션 방어, SME 정렬, 출처 추적성, 실행성 평가
- `llm_inference`: 외부 무료/오픈모델 추론 provider 상태와 fallback 여부

기본값은 외부 추론을 사용하지 않는 `disabled`입니다. 발표 안정성을 위해 외부 provider가 꺼져 있거나 실패하면 기존 규칙 기반 Agent 요약이 그대로 사용됩니다.

```bash
# Hugging Face Space 또는 ZeroGPU 계열 데모 API
export GROW_GATE_LLM_PROVIDER=hf_space
export GROW_GATE_HF_SPACE_URL="https://YOUR_SPACE.hf.space/agent"
export GROW_GATE_HF_TOKEN="선택_비공개_SPACE일때만"

# GroqCloud Free Tier 계열
export GROW_GATE_LLM_PROVIDER=groq
export GROQ_API_KEY="..."
export GROW_GATE_GROQ_MODEL="llama-3.1-8b-instant"

# Gemini Free Tier 계열
export GROW_GATE_LLM_PROVIDER=gemini
export GEMINI_API_KEY="..."
export GROW_GATE_GEMINI_MODEL="gemini-2.0-flash"
```

공모전 발표 문구:

> 상용 유료 LLM API에만 의존하지 않고, Hugging Face Space/ZeroGPU, GroqCloud, Gemini Free Tier 같은 외부 추론 provider를 선택적으로 붙일 수 있습니다. 외부 무료 자원의 대기열, 한도, cold start가 발생하면 GROW-GATE의 규칙 기반 Agent가 자동 fallback되어 시연 안정성을 확보합니다.

```bash
curl -X POST http://127.0.0.1:8000/ai-search \
  -H "Content-Type: application/json" \
  -d '{
    "query":"미국 전자제품 FCC 인증 근거",
    "scope":"diagnosis",
    "product_input":{
      "company_name":"경남 배터리테크",
      "region":"경남",
      "industry":"전기전자",
      "product_name":"리튬이온 배터리팩",
      "hs_code":"850760",
      "target_market":"United States",
      "features":["electronics","digital"],
      "certifications":["KC"],
      "has_bom":true,
      "has_test_report":true,
      "has_label_draft":true
    }
  }'
```

## 4. 데이터 파이프라인 미리보기

```bash
python -m backend.run_pipeline
```

실행하면 두 개의 빌드 파일이 생성됩니다.

- `data/build/rag_documents_preview.json`: 규제·리콜 데이터를 RAG/벡터DB에 넣기 쉬운 문서 형태로 정규화
- `data/build/public_data_normalized.json`: 화면별 공공데이터 소스에서 수집한 원천 데이터를 `trade_signal`, `country_profile`, `recall_case`, `patent_signal`, `support_program`, `evidence_source`, `api_call_log` 형태로 정규화

관리자 화면의 **ML 재학습 실행**은 `POST /ml/train`을 호출합니다. 이 엔드포인트는 공공데이터 수집, 검증, 정규화, RAG 청크 생성, ML feature 생성, XGBoost-ready 학습, 평가, artifact 발행을 하나의 실행 이력으로 기록합니다. 각 단계는 `collect_public_sources`, `validate_raw_data`, `normalize_documents`, `build_rag_chunks`, `build_ml_features`, `train_xgboost`, `evaluate_model`, `publish_model_artifacts`로 분리되어 Airflow DAG로 전환할 수 있습니다.

RAG 저장소는 현재 `data/build/rag_database_preview.json` JSON fallback으로 생성되며, `GET /rag/schema`에서 Supabase Postgres + pgvector로 옮길 테이블 계약을 확인할 수 있습니다.

공모전용 저장소는 `SQLite + JSON fallback`을 기본값으로 둡니다. 운영 고도화 단계에서는 같은 테이블 계약을 Supabase Postgres + pgvector로 이관해 `rag_documents`, `rag_chunks`, `rag_embeddings`, `pipeline_runs`, `model_versions`, `analysis_reports`, `search_runs`를 장기 축적할 수 있습니다.

기본값은 발표 안정성과 보안을 위해 seed 데이터로 실행합니다. 실제 API 호출은 명시적으로 허용한 경우에만 동작합니다. 필요한 키와 live 수집 허용 플래그를 환경 변수로 설정한 뒤 백엔드 API에서 `POST /public-data/collect?use_live=true`를 호출합니다.

```bash
export GROW_GATE_ENABLE_LIVE_COLLECTION=1
export DATAGO_SERVICE_KEY="공공데이터포털_일반인증키"
export KIPRIS_PLUS_KEY="KIPRIS_PLUS_인증키"
curl -X POST "http://127.0.0.1:8000/public-data/collect?use_live=true"
```

## 5. 보안 가드

- FastAPI CORS는 기본적으로 `127.0.0.1`, `localhost`, 로컬 파일 시연용 `null` origin만 허용합니다. 운영 배포에서는 `GROW_GATE_CORS_ORIGINS`에 허용 origin을 쉼표로 지정합니다.
- `POST /public-data/collect?use_live=true`는 `GROW_GATE_ENABLE_LIVE_COLLECTION=1`이 없으면 차단됩니다.
- 공공데이터 live 호출은 HTTPS와 허용된 공공데이터 호스트만 사용하며, private/localhost 주소로 해석되는 URL은 거부합니다.
- API 입력값은 HS Code, 제품 특성, 문자열 길이, 인증 목록 길이를 검증합니다.
- 프론트엔드는 CSP 메타 정책과 HTML escaping을 적용하고, 인쇄용 팝업은 `opener`를 분리합니다.

## 6. 화면별 데이터 연결 기준

- S04 수출기회 대시보드: 관세청 수출입무역통계, KOTRA 공급망 인사이트
- S05 규제·인증 패스포트: 국가기술표준원 Safety Korea 인증·리콜 데이터, 로컬 규칙 테이블
- S06 리콜 위험 Copilot: 국가기술표준원 제품 안전인증 및 리콜 정보
- S07 특허/FTO + 지원사업: KIPRIS Plus 특허 키워드, 수출바우처/해외규격인증/지역TP 공고
- S08~S09 로드맵/리포트: 위 근거를 `evidence_source`로 추적해 PDF/JSON에 포함

## 7. 실제 서비스 확장 포인트

- `backend/ml_models.py`: XGBoost 예측/추정 레이어. macOS에 `libomp`가 없으면 시연 안정성을 위해 동일 feature contract의 Gradient Boosting fallback을 사용
- `backend/public_data_pipeline.py`: 공공데이터 API 호출, seed fallback, 화면별 정규화, 수집 로그 생성
- `backend/local_demo_db.py`: SQLite 기반 제품 지식, RAG, ML, 보고서, 검색 실행 이력 저장
- `backend/operational_pipeline.py`: Airflow-ready task 단위의 공공데이터/RAG/ML 파이프라인
- `backend/product_knowledge.py`: 직원이 등록한 제품 지식을 Agent RAG 근거와 ML feature로 변환
- `backend/rag_database.py`: Supabase/pgvector 이관 가능한 RAG 저장소 계약
- `backend/data_connectors.py`: 분석 Agent가 사용할 데이터 facade와 RAG 문서화 payload 제공
- `backend/target_company.py`: 실제 중소기업 타겟 프로필을 RAG seed, 프롬프트 팩, 하네스 평가로 변환
- `backend/external_llm.py`: Hugging Face Space, Groq, Gemini 추론을 선택적으로 호출하고 실패 시 fallback
- `backend/ml_models.py`: 현재 규칙 기반 점수를 학습형 모델, 임베딩 유사도, 랭킹 모델로 교체
- `backend/agents.py`: ReportAgent와 RegulationAgent에 RAG/LLM 답변 생성 및 출처 표시 추가
- `frontend/app.js`: 공공데이터 출처, 문서 버전, 전문가 상담 연결, PDF 생성 기능 추가
- `data/sample_datasets.json`: 제품군별 규제/인증/리콜/지원사업 데이터 확장

## 8. 발표 시연 멘트

> 이 서비스는 수출 유망국을 단순 추천하는 서비스가 아니라, 지역 제조기업이 실제로 해외시장에 들어가기 위해 필요한 규제·인증·리콜·특허·지원사업 실행순서를 AI가 30·60·90일 로드맵으로 짜주는 서비스입니다.

## 9. 진행 현황

### 2026-05-31 주요 변경사항

- 기존 좌측 사이드바 구조를 줄이고, 상단 네비게이션과 단계형 흐름으로 변경했습니다.
- 첫 화면에서 `GROW-GATE Local EX` 서비스 정체성이 잘 보이도록 Hero 영역을 개선했습니다.
- AI 진단 전/후 상태가 구분되도록 KPI, 결과 패널, 로드맵 상태 전환을 추가했습니다.
- 하단에서 데이터 커넥터, 준비도 추정, 규제 매트릭스, 리콜 Copilot, FTO/지원사업, 30·60·90 실행 보드, AI 리포트, 공공데이터 파이프라인을 바로 확인할 수 있게 구성했습니다.

### 2026-06-02 주요 변경사항

- AI 검색 전용 화면과 추천/검색 품질 신호를 추가했습니다. 검색 결과는 의도 감지, 제품 문맥, 근거 신뢰도, 다양화 랭킹을 함께 표시합니다.
- 직원이 제품 지식을 등록하고 업데이트하는 `제품 지식 관리` 화면을 추가했습니다. 등록 데이터는 AI Agent의 RAG 근거와 ML feature 사전 지식으로 사용됩니다.
- `data.html`에 데모 DB 시드 생성, 실사용 시뮬레이션 실행, ML 재학습 실행 패널을 추가했습니다.
- `SQLite` 기반 데모 DB를 추가해 제품 지식, RAG 문서/청크, 임베딩 preview, 파이프라인 실행, 모델 평가, AI 검색, 분석 보고서를 실제로 저장합니다.
- 실제 중소기업 컨설팅 방향성에 맞춰 피엠그로우 SME 타겟 RAG, 고급 프롬프트 팩, 하네스 평가를 Agent raw signal에 연결했습니다.
- AI 진단 화면은 로그인 후 단계형 진단 흐름으로 정리하고, 한글 줄바꿈 깨짐과 “데이터 호출” 문구를 실무자 친화적인 표현으로 수정했습니다.
- FastAPI + 정적 프론트 서버 조합으로 `http://127.0.0.1:8766/data.html`에서 실제 배포형 데모처럼 사용할 수 있게 정리했습니다.
