## 공통 사전 과제

프로젝트 루트 README와 각 폴더 구조 확인을 통한 `FE-02`, `BE-02`, `database`, `data` 폴더 역할 정리

확인 과정에서 [backend/BE-02/README.md](/Users/jisu/grow_gate_local_ex/backend/BE-02/README.md)의 Project Structure에 실제 코드에는 존재하지만 설명이 빠져 있던 `membership/`, `health/` 항목 추가 및 회사 구성원 목록 조회 API(`/api/companies/me/members`)의 README 회사 API 목록 보강

> 실행 확인 자료로 프론트엔드 빌드, 백엔드 테스트, SQLite 테스트 DB 생성 결과 화면 첨부

### 1. 프론트엔드(FE-02)

역할: 화면 데이터 표시 및 사용자 입력의 백엔드 API 전달   
기술 스택: React + Vite
- `main.jsx`: React 앱의 브라우저 화면 마운트 진입점
- `App.jsx`: 앱 전체 라우터 및 페이지 흐름 관리
- `mock/`: 백엔드 연동 전 임시 데이터
- `services/`: API 호출 추상화 계층
- `hooks/`: 사용자 role 기반 액션 권한 확인
- `components/`: 재사용 컴포넌트
- `pages/`: 화면 단위 컴포넌트

### 2. 백엔드(BE-02)

역할: 프론트엔드 요청 API 제공 및 주요 비즈니스 로직 처리   
기술 스택: Spring Boot 3.4.4
- `main/resources/`: Spring Boot 설정 파일 위치
- `auth/`: 인증 처리
- `user/`: 사용자 정보 조회 및 수정
- `company/`: 회사 정보 조회 및 수정
- `product/`: 제품 목록 조회, 생성, 삭제 등 제품 기본 관리
- `knowledge/`: 수출 관련 지식베이스 관리
- `diagnosis/`: AI 진단 실행 및 결과 관리
- `membership/`: 회사 구성원/멤버십 엔티티 및 팀원 목록 조회 API
- `health/`: 서버 상태 확인 API 엔드포인트
- `security/`: JWT 인증, 로그인 사용자 확인, 보호 API 접근 통제
- `config/`: 애플리케이션 공통 설정
- `common/`: 공통 DTO 및 converter
- `exception/`: 예외 처리

### 3. database

역할: 백엔드 데이터 저장소 구조 준비 및 검증   
MySQL: Spring Boot 백엔드 운영/연동용 DB
- `01_schema.sql`: MySQL 스키마 생성
- `02_seed.sql`: MySQL 샘플 데이터 입력
- `verify.sql`: MySQL 검증 쿼리   

SQLite: 빠른 스키마 확인 및 로컬 테스트용 DB

### 4. data

역할: 진단 결과 및 분석용 근거 데이터와 처리 스크립트 관리
- `sample_trade.csv`, `sample_exchange.csv`, `sample_kotra_news.csv`: 무역, 환율, KOTRA 뉴스 샘플 데이터
- `economic_pipeline.py`: 경제/무역 데이터 수집 및 mock 데이터 생성
- `merge_samples.py`: 샘플 데이터 통합
- `train_predict_model.py`: 통합 데이터 기반 모델 학습/예측
- `app_streamlit.py`: 데이터 화면 확인용 Streamlit 앱

### 5. 실행 확인

#### FE-02 빌드 확인

- 실행 기준: `frontend/FE-02`에서 `npm run build`
- 확인 내용: Vite 프로덕션 빌드 성공 및 `dist/` 파일 생성
- 성공 기준: `built in ...` 출력

![FE-02 npm run build 성공 화면](./1.%20프론트엔드(FE-02).png)

#### BE-02 테스트 확인

- 실행 기준: `backend/BE-02`에서 `./gradlew test`
- 확인 내용: Spring Boot 백엔드 컴파일 및 테스트 통과
- 성공 기준: `BUILD SUCCESSFUL` 출력

![BE-02 ./gradlew test 성공 화면](./2.%20백엔드(BE-02).png)

#### SQLite 테스트 DB 확인

- 실행 기준: 프로젝트 루트에서 `python3 database/sqlite/build_sqlite_test_db.py`
- 확인 내용: SQLite 테스트 DB 생성 및 테이블별 row count 출력
- 성공 기준: `created=...growgate_be02_test.db`, `row_counts:` 출력

![SQLite 테스트 DB 생성 성공 화면](./3.%20database.png)
