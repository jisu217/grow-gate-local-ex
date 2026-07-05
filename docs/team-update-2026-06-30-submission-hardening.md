# 팀 공유 업데이트: 제출용 서비스 대대적 보완

작성일: 2026-06-30
공유 대상: FE, BE, 데이터/기획, 발표/제출 문서 담당
대상 브랜치: `codex/user-scoped-db-signup`
최신 커밋: `9ac2582 feat: harden submission-ready diagnosis service`
대외비 제외: 공모전 전략, 심사 대응 내부 판단, 개인/기관 API key, AWS/GitHub credential

## 1. 한 줄 요약

GROW-GATE Local EX는 기존의 "데모 화면 + 일부 CRUD + mock/fallback" 상태에서, React FE-02와 Spring Boot BE-02가 실제 진단 생성, 진행 상태 갱신, 공공데이터 source trace, 리포트 payload, 권한/감사/보안 보완까지 연결된 제출용 서비스 구조로 대폭 보강됐다.

## 2. 팀원에게 바로 공유할 메시지

아래 문구는 Slack/카톡/노션에 그대로 붙여도 된다.

```text
GROW-GATE Local EX 제출용 메인 앱을 대대적으로 보완했습니다.

핵심 변경은 1) React 진단 화면이 실제 Spring 진단 API를 호출하고, 2) Spring 진단 엔진의 고정 점수/고정 결과를 제거했으며, 3) 관세청·KOTRA·Safety Korea·KIPRIS·지원사업 기반 public-data live/cache/fallback 수집 계층을 추가하고, 4) 진단 결과에 sourceId, provider, matchedKey, scoreContribution, fallbackUsed를 남기도록 한 것입니다.

추가로 polling, PDF/Markdown 리포트 payload, FastAPI agent bridge, API retry/backoff, JWT secret 검증, logout token revocation, 권한 검사, audit pagination, soft delete, 운영 환경변수 정리, 제출용 홈 화면 보강, 공개 문서 정리까지 반영했습니다.

검증은 React build, GitHub Pages base build, Spring Boot test, git diff check까지 통과했습니다. 소스는 origin/codex/user-scoped-db-signup 브랜치에 push되어 있고, 정적 FE 산출물은 origin/gh-pages 브랜치에 올라갔습니다. GitHub Pages URL은 현재 404라 repo Settings > Pages에서 source를 gh-pages로 활성화해야 합니다.

대외비 공모전 전략 문서는 만들지 않았고, 공개 문서에는 구현/검증/남은 리스크만 정리했습니다.
```

## 3. 변경 범위 요약

| 영역 | 이전 상태 | 변경 후 |
| --- | --- | --- |
| 진단 실행 | 진단 버튼이 고정 id 또는 mock 흐름에 가까움 | 실제 `POST /api/diagnosis` 호출 후 반환 id로 이동 |
| 진단 결과 | 고정 점수와 고정 문구 중심 | 제품/HS Code/대상국/지식/인증/FTO/바이어 요구사항/출처 기반 점수 산식 |
| 공공데이터 | 출처명 또는 seed 수준 설명 중심 | provider connector, run/record DB, cache/fallback, source trace 연결 |
| 진행 상태 | 생성 후 자동 갱신 부족 | 진단 생성 후 polling으로 `running/done/failed` 갱신 |
| 리포트 | 메인 진단 결과와 리포트 다운로드 흐름 미흡 | `GET /api/diagnosis/{id}/report` payload 및 FE 다운로드 버튼 |
| FastAPI agent | 보조 프로토타입으로만 존재 | Spring에서 optional analyze/report bridge 사용 가능 |
| API 안정성 | timeout/에러 처리 제한적 | timeout, retry/backoff, 중첩 error message 파싱 |
| 인증/권한 | localStorage 중심, 권한 fallback 과함 | access token 확인, authorities, service layer 권한 검증 강화 |
| 토큰 폐기 | logout 후 기존 token 재사용 가능 | DB hash 기반 logout revocation |
| 감사/지식 로그 | pagination 부족 | audit log pagination API 구조 반영 |
| 제품 삭제 | FK 이력 손상 가능 | soft delete로 감사/진단 이력 보존 |
| 운영 설정 | CORS/log/JPA/public-data 설정 하드코딩 | env 기반 설정과 example 정리 |
| 메인 화면 | AI/3분 진단 메시지 중심 | 공공데이터, 서비스 구현, 운영 근거, 제출 산출물 readiness block 추가 |
| 문서 | 평가 대응 현황이 흩어짐 | 변경 보고서, 완료/미완료 표, 제출 준비도 점검 문서 정리 |

## 4. 주요 구현 상세

### 4.1 Frontend

변경 중심 파일:

- `frontend/FE-02/src/pages/diagnosis/DiagnosisNew.jsx`
- `frontend/FE-02/src/pages/diagnosis/DiagnosisResult.jsx`
- `frontend/FE-02/src/pages/diagnosis/DiagnosisList.jsx`
- `frontend/FE-02/src/services/diagnosisService.js`
- `frontend/FE-02/src/services/apiClient.js`
- `frontend/FE-02/src/pages/auth/Home.jsx`
- `frontend/FE-02/src/pages/auth/Home.module.css`

반영 내용:

- `diag-001` 고정 이동 제거.
- 저장 제품 선택 또는 직접 입력 기준으로 실제 진단 생성 API 호출.
- 진단 생성 후 결과 화면에서 polling으로 완료 상태 갱신.
- source trace, score contribution, fallback 여부를 UI 데이터로 매핑.
- 리포트 다운로드 버튼에서 backend report payload 호출.
- API 실패 시 mock 성공처럼 숨기지 않고 error state 전파.
- 홈 첫 화면에 제출 readiness block 추가.

### 4.2 Backend

변경 중심 파일:

- `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java`
- `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisService.java`
- `backend/BE-02/src/main/java/com/growgate/publicdata/**`
- `backend/BE-02/src/main/java/com/growgate/security/**`
- `backend/BE-02/src/main/java/com/growgate/knowledge/**`
- `backend/BE-02/src/main/java/com/growgate/product/**`

반영 내용:

- 고정 진단 점수 제거.
- `market`, `regulation`, `recall`, `fto`, `support`, `overall` 6축 점수 산출.
- public-data 수집 run/record entity, repository, service, controller 추가.
- provider connector 추가:
  - `KcsTradeConnector`
  - `KotraMarketConnector`
  - `SafetyRecallConnector`
  - `KiprisFtoConnector`
  - `BizinfoSupportConnector`
- source trace에 `sourceId`, `provider`, `matchedKey`, `usedFor`, `scoreContribution`, `fallbackUsed`, `cacheHit`, `collectionRunId`, `errorMessage` 반영.
- transaction commit 이후 async 진단 실행.
- FastAPI optional bridge 추가.
- JWT secret 검증 강화, token revocation service 추가.
- product soft delete와 권한 검사 보강.
- audit log pagination 보강.

### 4.3 Database

변경 파일:

- `database/mysql/init/01_schema.sql`
- `database/sqlite/schema.sql`

추가/변경:

- `public_data_collection_runs`
- `public_data_records`
- public-data 조회용 index
- product soft delete 관련 컬럼

## 5. 공공데이터/API key 정책

실제 API key는 repository에 넣지 않았다. key는 발급 후 환경변수로 주입한다.

| Provider | 설정 key | endpoint 상태 |
| --- | --- | --- |
| 관세청 수출입무역통계 | `KCS_TRADE_API_KEY` 또는 `DATAGO_SERVICE_KEY` | 기본 operation endpoint 반영 |
| KOTRA 글로벌 공급망 | `KOTRA_MARKET_API_KEY` 또는 `DATAGO_SERVICE_KEY` | 공식 카탈로그 URL 기본값, 승인 후 operation endpoint 교체 필요 |
| Safety Korea/KATS | `SAFETY_RECALL_API_KEY` 또는 `DATAGO_SERVICE_KEY` | 공식 안내 URL 기본값, operation endpoint 교체 필요 |
| KIPRIS Plus | `KIPRIS_PLUS_ACCESSKEY`, `KIPRIS_PLUS_KEY`, 또는 `KIPRIS_API_KEY` | `특허·실용 공개·등록공보`의 `getAdvancedSearch` operation endpoint와 `ServiceKey` 파라미터 확인 완료. key 주입 후 live smoke 필요 |
| 기업마당/수출바우처 | `BIZINFO_API_KEY` 또는 `DATAGO_SERVICE_KEY` | 공식 포털 기본값, operation endpoint 교체 필요 |

중요한 방어 로직:

- 공식 카탈로그/신청 페이지 URL은 HTML을 API 응답처럼 저장하지 않는다.
- operation endpoint가 아니면 fallback record에 사유를 남긴다.
- API key가 없거나 호출이 실패해도 진단은 실패시키지 않고, source trace에 fallback 이유를 표시한다.

## 6. 문서 산출물

팀원이 확인해야 할 문서:

| 문서 | 용도 |
| --- | --- |
| `docs/submission-update-report.md` | 전체 변경사항과 보완 내용 상세 |
| `docs/evaluation-resolution-status.md` | 첨부 평가서 기준 완료/부분완료/미완료 판정 |
| `docs/submission-readiness-audit.md` | 제출 전면 점검, 배포/GitHub 상태, 남은 리스크 |
| `docs/superpowers/specs/2026-06-30-live-public-data-collection-design.md` | live 공공데이터 수집 설계 |
| `docs/superpowers/plans/2026-06-30-live-public-data-collection.md` | live 공공데이터 구현 계획 |
| `docs/solutions/architecture-patterns/live-public-data-collection-layer.md` | public-data collection layer 패턴 정리 |
| `docs/solutions/backend/user-scoped-db-persistence.md` | 사용자별 DB 저장/권한 보완 내역 |

새 팀 공유 문서:

- `docs/team-update-2026-06-30-submission-hardening.md`

## 7. 실행 방법

### Backend

```bash
cd backend/BE-02
export DB_PASSWORD=your_local_db_password
export JWT_SECRET=$(openssl rand -base64 32)
export JPA_DDL_AUTO=update
export PUBLIC_DATA_ENABLED=false
./gradlew bootRun
```

live 공공데이터 수집을 켤 때:

```bash
export PUBLIC_DATA_ENABLED=true
export DATAGO_SERVICE_KEY=issued_public_data_key
export KIPRIS_PLUS_KEY=issued_kipris_key
```

### Frontend

```bash
cd frontend/FE-02
npm install
npm run dev
```

기본 접속:

```text
http://localhost:5173/
```

## 8. 검증 결과

수행된 검증:

| 검증 | 결과 |
| --- | --- |
| `npm run build` | 통과 |
| `npm run build -- --base=/grow_gate_local_ex/` | 통과 |
| `./gradlew test` with Java 17 | 통과 |
| `git diff --check` | 통과 |
| GitHub source branch push | 완료 |
| GitHub static deployment branch push | 완료 |

환경 이슈:

- 2026-07-05 이전에는 macOS sandbox에서 Playwright Chromium 실행이 `MachPortRendezvous` 권한 문제로 실패했다.
- 2026-07-05 기준 `frontend/FE-02`에 Playwright dev dependency와 Chromium runtime을 설치했고, sandbox 밖 권한으로 `playwright-ok 149.0.7827.55` headless 실행을 확인했다.
- Vite dev server도 sandbox 안에서는 포트 바인딩이 제한되어, sandbox 밖 실행으로 확인했다.
- AWS profile session이 만료되어 S3 배포는 진행하지 못했다.

## 9. GitHub 및 배포 상태

| 항목 | 상태 |
| --- | --- |
| Source branch | `origin/codex/user-scoped-db-signup` |
| Source commit | `9ac2582 feat: harden submission-ready diagnosis service` |
| Static deployment branch | `origin/gh-pages` |
| GitHub Pages URL | `https://jiyongkim615.github.io/grow_gate_local_ex/` |
| GitHub Pages 현재 응답 | 404 확인됨 |
| 필요한 조치 | GitHub repo Settings > Pages에서 source를 `gh-pages` branch로 활성화 또는 전파 완료 대기 |
| AWS S3 배포 | `aws login --profile kbds-happyloop-invoke-agent` 인증 필요 |

## 10. 역할별 확인 요청

### Frontend 담당

- 홈 hero readiness block이 모바일/데스크톱에서 어색하지 않은지 확인.
- 진단 생성, 목록, 결과, 리포트 버튼 flow 확인.
- API 실패 시 mock fallback이 켜지지 않는 제출 모드 확인.

### Backend 담당

- `PUBLIC_DATA_ENABLED=false` 기본 모드에서 fallback record가 의도대로 저장되는지 확인.
- 실제 발급 key로 `KCS_TRADE` live 호출 smoke test.
- KOTRA/Safety/KIPRIS operation endpoint 확보 후 env 교체.
- token revocation은 DB hash 기반으로 보강됨. refresh token rotation과 Redis cache는 운영 고도화 항목으로 별도 판단.

### 데이터/기획 담당

- public-data provider 5종 설명이 실제 출처/활용 목적과 일치하는지 확인.
- `scoreContribution` 설명이 사용자에게 과장으로 보이지 않는지 확인.
- 실제 라벨 기반 ML 모델과 pgvector 기반 대규모 문서 검색은 아직 제외 범위임을 문서 표현에서 유지.

### 발표/제출 문서 담당

- 공개 문서는 구현 근거 중심으로 사용.
- 대외비 공모전 전략은 별도 비공개 채널에서만 관리.
- GitHub Pages 404가 해소되기 전에는 배포 URL을 최종 제출 링크로 쓰지 않기.

## 11. 남은 작업

| 우선순위 | 작업 | 이유 |
| --- | --- | --- |
| P0 | GitHub Pages source 설정 확인 | `gh-pages`는 push됐지만 URL은 404 |
| P0 | AWS login 후 실제 S3 배포 여부 결정 | 현재 AWS session expired |
| P1 | 공공데이터포털/KIPRIS 실제 API key 발급 및 env 주입 | live 수집 smoke test 필요 |
| P1 | KOTRA/Safety/KIPRIS operation endpoint 확보 | 현재 일부는 공식 카탈로그 URL만 기본값 |
| P1 | 제출용 시연 계정/시드 데이터 정리 | 심사자가 안정적으로 진단 flow를 볼 수 있어야 함 |
| P2 | Playwright 시각 QA 스크립트 확대 | 패키지/Chromium 실행은 준비됨. 진단 결과 핵심 화면별 자동 screenshot/assertion 추가 필요 |

## 12. 공유 시 주의 사항

- API key, AWS credential, GitHub token은 공유 문서나 repository에 넣지 않는다.
- `PUBLIC_DATA_ENABLED=true`는 key와 operation endpoint가 준비된 환경에서만 사용한다.
- `gh-pages` 브랜치 push는 정적 FE 배포 산출물이며, Spring backend API까지 배포된 것은 아니다.
- 현재 문서는 구현/검증 공유용이며, 대외비 공모전 전략 문서가 아니다.
