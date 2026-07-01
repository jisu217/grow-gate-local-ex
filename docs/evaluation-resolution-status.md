# 첨부 평가 항목 완료/미완료 판정표

작성일: 2026-07-01
대상: 공모전 제출용 메인 앱 `frontend/FE-02` + `backend/BE-02`

## 1. 판정 기준

| 상태 | 의미 |
| --- | --- |
| 완료 | 평가 지적을 코드 또는 문서에서 직접 수정했고, 현재 제출 설명에 포함 가능 |
| 부분 완료 | 핵심 위험은 줄였지만 평가자가 요구한 production 수준까지는 아직 남은 상태 |
| 미완료 | 현재 코드에서 아직 구현되지 않았거나 제출 범위에서 제외해야 하는 상태 |
| 검증 대기 | 코드 변경은 있으나 로컬 환경 제약으로 자동 테스트까지 확인하지 못한 상태 |

## 2. 먼저 인정해야 할 큰 미완료 3개

사용자가 이미 짚은 아래 3개는 현재도 미완료로 보는 것이 맞다.

| 항목 | 현재 상태 | 제출 시 표현 |
| --- | --- | --- |
| 실제 live 공공 API 수집 | 부분 완료 | `PUBLIC_DATA_ENABLED=true`를 기본값으로 전환했고 provider별 live/cache/fallback record와 domain signal을 저장한다. 단, 일부 기관은 실제 operation endpoint/key 발급이 필요 |
| 실제 라벨 기반 XGBoost | 미완료 | 현재는 실제 라벨 학습 모델이 아니라 규칙 기반 점수화 및 데모/프로토타입 ML 코드 |
| pgvector급 RAG | 미완료 | 현재는 운영형 vector DB 검색 RAG가 아니라 문서/출처 기반 근거 추적 수준 |

## 3. 위 3개를 제외한 추가 보완 현황

아래 항목들은 사용자가 말한 3개 외에 남아 있던 미완료/부분 완료 항목이며, 2026-06-30 추가 보완 후의 상태다.

| 남은 항목 | 상태 | 이유 |
| --- | --- | --- |
| Spring 메인 앱과 FastAPI Agent 백엔드의 실제 통합 | 부분 완료 | `APP_FASTAPI_ENABLED=true`일 때 Spring 진단 프로세서가 FastAPI `/analyze`, `/report`를 호출해 결과 JSON에 병합한다. 단, FastAPI 운영 배포/장애 모니터링까지는 아님 |
| 실제 agentic planning/tool/retry loop | 완료 | Spring 진단 경로에 `AgentLoop`를 추가해 plan, tool execution, verifier, retry, fallback, `complete_task`를 실제 실행한다. 단, LLM이 런타임에 임의 도구를 새로 발견하는 open-ended agent는 아님 |
| PDF 리포트 생성의 제출용 연동 | 부분 완료 | React/Spring 진단 상세에서 `/diagnosis/{id}/report` 리포트 다운로드를 연결했다. 현재는 Markdown/PDF-ready report이며 서버 binary PDF 생성은 아님 |
| 진단 진행 상태 polling/WebSocket | 완료 | 진단 상세 페이지가 `pending/running` 상태를 2.5초 간격으로 polling하고 완료 시 자동 갱신한다 |
| 진단 결과 상세 카드의 완전한 API 데이터화 | 완료 | S00~S10 상세 카드를 backend `stageCards` 기반으로 매핑한다. S04~S07도 backend 점수/source trace 기반으로 치환 |
| API 재시도 정책 | 완료 | GET 요청에 기본 retry/backoff를 추가했고 timeout/error parsing과 함께 동작한다 |
| 프론트 인증 저장 방식 개선 | 부분 완료 | 보호 라우트가 `/users/me`로 서버 권한을 재동기화하고 logout token revocation을 추가했다. 단, `localStorage` JWT와 HttpOnly cookie 미전환은 남아 있다 |
| 서버 세션 기반 프론트 권한 동기화 | 완료 | 보호 라우트 진입 시 서버 `/users/me`를 조회해 role/company 정보를 갱신한다 |
| refresh token과 logout token revocation | 부분 완료 | logout 시 access token denylist를 적용했다. refresh token 발급/회전 구조는 아직 없다 |
| Spring Security authorities 정식화 | 완료 | `UserDetailsServiceImpl`이 `ROLE_*`, `TYPE_*` authorities를 구성한다 |
| 지식/감사 로그 pagination | 완료 | `GET /api/products/{productId}/audit-logs?page=&size=` pagination 응답을 추가했다 |
| 제품 조회 N+1 최적화 | 완료 | 제품 목록에서 지식/인증/FTO/바이어 요구사항을 product id batch 조회로 모아 N+1을 줄였다 |
| 시간 포맷/타임존 정책 통일 | 부분 완료 | 운영 설정 일부는 환경변수화했지만 전체 응답 timestamp 정책을 UTC/Asia-Seoul 중 하나로 완전히 통일하지는 않음 |
| `ddl-auto=update` 제거 및 migration 체계 | 부분 완료 | 기본값을 `JPA_DDL_AUTO=validate`로 바꿨다. Flyway/Liquibase 같은 migration 도구 도입은 아직 없음 |
| FastAPI 데모 백엔드 보안/운영화 | 미완료 | FastAPI는 보조 데모로 분리했기 때문에 auth, CORS, 운영 배포, Git bridge hardening을 제출용 수준으로 고치지 않음 |
| Java 17 기반 백엔드 테스트 검증 | 완료 | `/opt/homebrew/opt/openjdk@17`와 workspace Gradle cache로 `./gradlew test` 통과 |

## 4. 평가 Top 10 기준 완료 현황

| # | 평가 지적 | 현재 판정 | 처리 내용 |
| --- | --- | --- | --- |
| 1 | 진단 버튼이 실제 API를 호출하지 않음 | 완료 | `DiagnosisNew.jsx`에서 실제 `runDiagnosis()` 호출 후 반환된 `diagnosisId`로 이동 |
| 2 | Spring 진단 결과가 하드코딩 | 완료 | 고정 `72` 점수 제거, 제품/HS/국가/인증/FTO/바이어/출처 기반 규칙 점수화 적용 |
| 3 | React/Spring과 FastAPI 데모가 분리됨 | 부분 완료 | Spring에서 FastAPI `/analyze`, `/report` optional bridge를 추가. 단, FastAPI 운영 배포는 별도 |
| 4 | 공공데이터가 알고리즘에 실제 반영되지 않음 | 부분 완료 | public data live/cache/fallback record와 domain signal을 수집해 source trace와 점수에 반영. 단, 일부 실제 API key/operation endpoint는 외부 발급 필요 |
| 5 | mock/fallback이 실패를 숨김 | 완료 | `VITE_ENABLE_MOCK=false` 기본값으로 실패를 error state로 노출 |
| 6 | 멤버십이 없으면 admin 처리 | 완료 | `orElse(UserRole.admin)` 제거, 개인 owner/회사 멤버십 기준으로 권한 정리 |
| 7 | 제품 삭제가 FK를 깨뜨릴 수 있음 | 완료 | 물리 삭제 대신 `deleted_at`, `deleted_by` 기반 soft delete 적용 |
| 8 | XGBoost가 synthetic data 기반 | 완료 | UI/문서에서 XGBoost 표현을 제거하고 `Rule rubric · LLM assisted`로 구현 수준에 맞춤. 실제 라벨 ML은 후속 과제로 유지 |
| 9 | RAG가 vector retrieval 수준이 아님 | 부분 완료 | pgvector 주장은 하지 않고 DB context injection + knowledge loop로 범위를 명확화. vector retrieval은 후속 과제로 유지 |
| 10 | README와 실제 코드 수준 불일치 | 완료 | 제출용 메인 앱 범위, FastAPI 보조 성격, 남은 한계를 README와 보완 보고서에 반영 |

## 5. 프론트엔드 세부 항목

| 영역 | 평가 지적 | 현재 판정 | 처리 내용 |
| --- | --- | --- | --- |
| `DiagnosisNew` | `runDiagnosis` 미호출, `diag-001` 고정 이동 | 완료 | 실제 API 호출, 결과 id 이동으로 변경 |
| `DiagnosisNew` | 제품명/HS Code/대상국 검증 부족 | 완료 | 제품명 필수, HS 6~10자리 숫자, 대상국 alias 정규화 추가 |
| `diagnosisService` | API 실패 시 mock fallback | 완료 | mock fallback은 `VITE_ENABLE_MOCK=true`일 때만 허용 |
| `DiagnosisResult` | 없는 `diag-001`도 mock으로 정상 표시 가능 | 완료 | 기본 mock fallback 비활성화로 실제 실패가 드러나도록 변경 |
| `DiagnosisResult` | 단계별 상세 결과가 실제 산출물인지 불명확 | 완료 | S00~S10을 backend stageCards 기반으로 매핑하고 S04~S07 mock 기본 주입 제거 |
| `DiagnosisResult` | 진단 완료까지 자동 갱신 없음 | 완료 | `pending/running` 상태 polling 추가 |
| `DiagnosisResult` | 리포트 다운로드 없음 | 부분 완료 | Markdown/PDF-ready 리포트 다운로드 추가. 서버 binary PDF는 남음 |
| `DiagnosisList` | `running` 상태 없음 | 완료 | 진행 중 상태 표시 추가 |
| `apiClient` | timeout 없음 | 완료 | 기본 15초 timeout 추가 |
| `apiClient` | `/api/api` 중복 가능 | 완료 | base URL 정규화 추가 |
| `apiClient` | 서버 에러 메시지 파싱 부족 | 완료 | 중첩 `error.message` 파싱 |
| `apiClient` | GET에도 `Content-Type` 부착 | 완료 | body 없는 GET에는 content-type 생략 |
| `apiClient` | retry/abort 정책 부족 | 완료 | GET retry/backoff와 timeout 적용 |
| `ProtectedRoute` | localStorage user만 확인 | 완료 | token 확인 후 `/users/me`로 서버 권한 재동기화 |
| `usePermission` | mock/localStorage 권한 의존 | 부분 완료 | 보호 라우트에서 갱신된 서버 권한을 사용. hook 자체는 동기 local cache를 읽음 |
| `Mypage` | 재진단이 실제 id를 쓰지 않음 | 완료 | 실제 진단 API 응답 id로 이동 |
| `Mypage` | 로컬 점수 조작 | 완료 | 로컬 조작 제거 |
| `productService` | 제품/지식 조회 실패를 mock으로 숨김 | 완료 | 기본 mock fallback 비활성화 |
| `productService` | 만료 인증 상태 버그 | 완료 | 만료/임박 상태 계산 보정 |
| `productService` | `knowledgeStatus`, `missingFields` undefined | 완료 | 계산 필드 생성 로직 추가 |

## 6. 백엔드 세부 항목

| 영역 | 평가 지적 | 현재 판정 | 처리 내용 |
| --- | --- | --- | --- |
| `DiagnosisAsyncProcessor` | 점수/출처/문구 하드코딩 | 완료 | 입력과 제품 지식 기반 규칙 점수 및 source trace 생성 |
| `DiagnosisAsyncProcessor` | 실제 공공 API 데이터 미사용 | 부분 완료 | `PublicDataCollectionService`로 live/cache/fallback record와 provider별 metric signal을 수집하고 source trace/점수에 반영. 단, 심사 환경에 실제 API key/operation endpoint가 없으면 `fallback`으로 기록 |
| `DiagnosisAsyncProcessor` | FastAPI agent 미연동 | 부분 완료 | `APP_FASTAPI_ENABLED=true`일 때 FastAPI `/analyze`, `/report` 호출 및 fallback trace 기록 |
| `DiagnosisService` | `productId`와 `inputData` 정책 없음 | 완료 | 동시 전달 거부, 직접 입력 필수값 검증 |
| `DiagnosisService` | description 누락 | 완료 | `inputData` snapshot에 description 저장 |
| `DiagnosisService` | 비동기 race 가능 | 완료 | transaction commit 이후 async processor 실행 |
| `CreateDiagnosisRequest` | validation 부족 | 완료 | nested validation, productName/hsCode 필수, HS pattern, description size 추가 |
| `ProductService` | admin fallback | 완료 | 권한 fallback 제거 |
| `ProductService` | delete FK 위험 | 완료 | soft delete 적용 |
| `ProductService` | N+1 가능성 | 완료 | 제품 id batch 조회로 연관 지식 데이터를 모아 조회 |
| `ProductService` | timestamp 포맷 혼재 | 부분 완료 | 전체 시간 정책 통일은 미완료 |
| `KnowledgeService` | admin fallback | 완료 | 권한 fallback 제거 |
| `KnowledgeService` | invalid trustGrade 무시 | 완료 | 잘못된 trustGrade는 400 처리 |
| `KnowledgeService` | URL 검증 없음 | 완료 | URL 형식 검증 추가 |
| `KnowledgeService` | riskLevel 대소문자 취약 | 완료 | lower-case 입력 허용 |
| `KnowledgeService` | audit pagination 없음 | 완료 | `page`, `size`, `totalElements`, `totalPages` 응답 추가 |
| `AuthService` | 가입 type typo가 personal로 처리 | 완료 | `personal|company` 외 값 거부 |
| `AuthService` | 기업 필수값/사업자번호 검증 부족 | 완료 | 회사명, 사업자번호, 업종 및 10자리 사업자번호 검증 |
| `AuthService` | logout token revocation 없음 | 부분 완료 | access token denylist 적용. refresh token revocation은 미구현 |
| `SignupRequest` | password/type/company 검증 부족 | 완료 | DTO/service 검증 강화 |
| `JwtTokenProvider` | 짧은 secret padding | 완료 | 32 bytes 미만 secret 거부, Base64 secret 지원 |
| `JwtTokenProvider` | refresh/revocation/role claim 부족 | 부분 완료 | logout denylist와 authorities 구성은 완료. refresh token/role claim은 미구현 |
| `SecurityConfig` | CORS hardcoded | 완료 | 허용 origin 환경변수화 |
| `SecurityConfig` | CSRF/localStorage JWT 위험 | 미완료 | HttpOnly cookie/CSRF 구조 미전환 |
| `GlobalExceptionHandler` | 500 stack logging 부족 | 완료 | unhandled exception logging 추가 |
| `application.yml` | show-sql/debug log 하드코딩 | 완료 | 환경변수 기반 설정 추가 |
| `application.yml` | `ddl-auto=update` 운영 위험 | 부분 완료 | 기본값을 `validate`로 변경. migration 도구 도입은 미완료 |

## 7. 문서/제출 설명에서 완료된 것

| 항목 | 상태 | 내용 |
| --- | --- | --- |
| 메인 앱 범위 정리 | 완료 | 제출용 메인을 `frontend/FE-02` + `backend/BE-02`로 명확히 표기 |
| FastAPI 과장 표현 완화 | 완료 | FastAPI는 보조 정적/로컬 데모로 설명 |
| 공공데이터 수준 명확화 | 완료 | API key 기반 live 수집, cache, fallback record, sourceType, domain signal, 점수 기여도로 표현 |
| 변경사항 보고서 추가 | 완료 | [submission-update-report.md](submission-update-report.md)에 구현 변경사항 정리 |
| 한계 공개 | 완료 | 실제 API key/운영 모니터링, 라벨 ML, pgvector RAG는 후속 과제로 분리 |

## 8. 검증 현황

| 검증 | 결과 | 비고 |
| --- | --- | --- |
| React build | 통과 | `frontend/FE-02`에서 `npm run build` 성공 |
| Spring compile | 통과 | Java 17 + workspace Gradle cache로 `./gradlew compileJava` 성공 |
| Spring backend test | 통과 | Java 17 + workspace Gradle cache로 `./gradlew test` 성공 |
| whitespace check | 통과 | `git diff --check` 성공 |
| 하드코딩 `diag-001` 제거 확인 | 통과 | mock seed 외 실제 이동 경로에서 제거 |
| admin fallback 제거 확인 | 통과 | `orElse(UserRole.admin)` 제거 확인 |

## 9. 제출 관점 결론

현재는 “데모 화면 + 일부 CRUD + mock/fallback”이라는 가장 큰 비판을 상당 부분 줄였다. 특히 진단 시작, 점수 산정, source trace, 권한, soft delete, mock 격리, README 정합성은 제출 설명에 완료 항목으로 적을 수 있다.

다만 “모든 점수가 항상 실시간 공공 API만으로 계산되는 AI/RAG 서비스”라고 주장하면 아직 방어가 어렵다. 제출 표현은 아래처럼 잡는 것이 안전하다.

- 완료로 말할 수 있는 것: React/Spring 메인 앱의 실제 진단 run 생성, 입력 기반 규칙 진단, API key 기반 live 공공데이터 수집 구조, cache/fallback record, sourceType/domain signal, S00~S10 stageCards API화, 권한/삭제/검증/문서 정합성 보완
- 아직 말하면 안 되는 것: API key/operation endpoint 없이도 항상 live 데이터가 수집된다는 주장, 실제 라벨 기반 XGBoost 예측, pgvector급 RAG 검색, LLM이 임의 도구를 동적으로 발견하는 open-ended agent, token revocation 기반 보안 완성
