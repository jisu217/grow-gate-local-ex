# GROW-GATE Local EX Demo

**지역 제조기업 수출규제·인증 실행 AI 에이전트** 시연용 프로토타입입니다. 

이 패키지는 실무자가 쓰기 쉬운 다중 화면 정적 프론트엔드와, 같은 분석을 API로 제공하는 FastAPI 백엔드를 함께 포함합니다. 외부 API 키 없이 로컬 샘플 데이터로 실행되며, 실제 사업화 단계에서는 데이터 커넥터 부분을 산업부·무역·해외시장·시험인증·제품안전·특허·지원사업 공공데이터 API로 교체하면 됩니다. 

## 1. 폴더 구조

```text
grow_gate_local_ex_demo/
├─ frontend/
│  ├─ index.html          # 업무 담당자 홈
│  ├─ workspace.html      # AI 진단 워크스페이스
│  ├─ data.html           # 공공데이터 파이프라인 화면
│  ├─ roadmap.html        # 30·60·90일 실행 로드맵 화면
│  ├─ styles.css          # Apple-light 업무 UI 스타일
│  └─ app.js              # 워크스페이스 로컬 JS 분석 엔진 + API 연동 코드
├─ backend/
│  ├─ app.py              # FastAPI 서버
│  ├─ agents.py           # Planner/Data/Market/Regulation/Recall/FTO/Support/Roadmap Agents
│  ├─ data_connectors.py  # 공공데이터 커넥터 계층 및 ETL preview
│  ├─ ml_models.py        # 시장성, 규제 매칭, 리콜 유사도, FTO, 준비도 점수 모델
│  ├─ schemas.py          # API 입출력 스키마
│  └─ run_pipeline.py     # RAG 문서화용 데이터 파이프라인 미리보기
├─ data/
│  ├─ sample_datasets.json       # 시연용 샘플 데이터
│  └─ public_data_sources.json   # 화면별 공공데이터 소스/테이블 매핑
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

서버가 실행되면 브라우저에서 `frontend/workspace.html`을 열고 **FastAPI 백엔드 사용** 체크박스를 켠 뒤 분석을 실행합니다.

API 엔드포인트:

- `GET /health`
- `GET /datasets`
- `GET /pipeline/preview`
- `GET /public-data/sources`
- `GET /public-data/normalized`
- `POST /public-data/collect`
- `POST /analyze`
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

## 4. 데이터 파이프라인 미리보기

```bash
python -m backend.run_pipeline
```

실행하면 두 개의 빌드 파일이 생성됩니다.

- `data/build/rag_documents_preview.json`: 규제·리콜 데이터를 RAG/벡터DB에 넣기 쉬운 문서 형태로 정규화
- `data/build/public_data_normalized.json`: 화면별 공공데이터 소스에서 수집한 원천 데이터를 `trade_signal`, `country_profile`, `recall_case`, `patent_signal`, `support_program`, `evidence_source`, `api_call_log` 형태로 정규화

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
- `backend/data_connectors.py`: 분석 Agent가 사용할 데이터 facade와 RAG 문서화 payload 제공
- `backend/ml_models.py`: 현재 규칙 기반 점수를 학습형 모델, 임베딩 유사도, 랭킹 모델로 교체
- `backend/agents.py`: ReportAgent와 RegulationAgent에 RAG/LLM 답변 생성 및 출처 표시 추가
- `frontend/app.js`: 공공데이터 출처, 문서 버전, 전문가 상담 연결, PDF 생성 기능 추가
- `data/sample_datasets.json`: 제품군별 규제/인증/리콜/지원사업 데이터 확장

## 8. 발표 시연 멘트

> 이 서비스는 수출 유망국을 단순 추천하는 서비스가 아니라, 지역 제조기업이 실제로 해외시장에 들어가기 위해 필요한 규제·인증·리콜·특허·지원사업 실행순서를 AI가 30·60·90일 로드맵으로 짜주는 서비스입니다.

## 9. 진행 현황

### 1차 스프린트 (2026-05-31)

- 다중 화면 정적 프론트엔드(홈·워크스페이스·데이터·로드맵) 구성 완료
- FastAPI 백엔드 및 Planner/Data/Market/Regulation/Recall/FTO/Support/Roadmap Agent 파이프라인 구현
- 공공데이터 커넥터 계층과 seed 기반 ETL preview, RAG 문서화 파이프라인 구성
- XGBoost(또는 Gradient Boosting fallback) 기반 예측/추정 레이어 적용
- 로컬 샘플 데이터 기반 end-to-end 시연 시나리오 동작 확인
