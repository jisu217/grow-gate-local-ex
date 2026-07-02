# Fresh DB, Report, and Security Readiness Hardening

작성일: 2026-07-03
브랜치: `codex/main-daily-public-data-refresh`

## 문제

심사위원이 fresh clone 기준으로 `docker compose up` 후 백엔드가 바로 뜨는지, 보고서 산출물이 실제로 고객에게 줄 수 있는지, CSRF/rate-limit 보안 설명이 가능한지 물을 수 있었다.

구체적인 리스크는 세 가지였다.

- Docker init schema와 entity가 다르면 `spring.jpa.hibernate.ddl-auto=validate`에서 시작 실패
- ASCII-only minimal PDF가 한글을 `?`로 치환해 보고서 품질이 낮아짐
- CSRF와 로그인 rate limit이 코드에는 있지만 테스트/문서가 부족해 운영 타당성 답변이 약함

## 해결

### 1. Docker init schema를 entity 기준으로 정렬

`database/mysql/init/01_schema.sql`의 `public_data_records`에 entity가 기대하는 컬럼을 추가했다.

- `confidence DOUBLE PRECISION`
- `source_type VARCHAR(20)`

`database/mysql/verify.sql`도 public-data 테이블과 두 컬럼을 직접 확인하도록 확장했다.

### 2. Fresh DB 검증 스크립트 추가

`scripts/verify-fresh-db.sh`는 별도 Docker Compose project와 기본 포트 `33306`을 사용해 clean MySQL을 띄운다. 이후 backend jar를 만들고 `JPA_DDL_AUTO=validate`로 실행한 뒤 `/health`가 뜨는지 확인한다.

기존 로컬 DB volume은 사용하지 않고 종료 시 임시 volume을 삭제한다.

### 3. PDF를 제출 경로에서 제거하고 Markdown으로 통일

`/api/diagnosis/{id}/report.pdf` endpoint와 ASCII-only `minimalPdf()`를 제거했다. 제출 시연 기본 산출물은 `/api/diagnosis/{id}/report`의 UTF-8 Markdown 바이어 대응 메모다.

PDF는 한글 font embedding과 visual QA가 끝난 뒤 별도 기능으로 다시 도입한다.

### 4. CSRF/rate-limit 테스트와 threat model 문서화

`CsrfCookieFilterTest`가 double-submit cookie/header 동작을 검증한다.

`LoginAttemptRateLimiterTest`가 email normalization, 실패 횟수 block, 성공 시 reset을 검증한다.

`docs/security-csrf-rate-limit-threat-model-2026-07-03.md`에는 다음을 명시했다.

- stateless JWT cookie라 Spring session CSRF 대신 custom double-submit filter 사용
- 제출 서버 rate limit은 단일 인스턴스 in-memory
- 다중 인스턴스 상용 배포 전 Redis/shared TTL store로 교체 필요

### 5. 프론트 env alias 지원

`apiClient.js`가 `VITE_API_URL`과 `VITE_API_BASE_URL`을 모두 지원한다. 제출 템플릿과 코드 env 이름이 달라 API base URL이 무시되는 문제를 막는다.

## Guardrails

`scripts/check-risk-gates.sh`가 아래 회귀를 막는다.

- Docker init schema에 `confidence`, `source_type` 누락
- fresh DB verifier 누락
- unverified PDF endpoint/claim 재도입
- `VITE_API_BASE_URL` 지원 누락
- CSRF/rate-limit 테스트 누락
- in-memory rate limit 범위 문서 누락

## 검증

- `./scripts/check-risk-gates.sh`
- `cd backend/BE-02 && ./gradlew test --no-daemon`
- `cd frontend/FE-02 && npm run build`
- `node frontend/FE-02/src/services/diagnosisService.mapper.test.js`

`./scripts/verify-fresh-db.sh`는 현재 로컬 Docker daemon이 꺼져 있어 실행 전 daemon check에서 중단됐다. Docker Desktop 또는 Colima를 켠 뒤 같은 명령을 실행하면 clean DB validate startup을 확인한다.
