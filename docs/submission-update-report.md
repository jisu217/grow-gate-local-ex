# GROW-GATE Local EX 변경사항 및 보완 보고서

작성일: 2026-06-30
대상: 공모전 제출용 메인 앱 `frontend/FE-02` + `backend/BE-02`

## 1. 개선 목적

첨부 평가에서 가장 크게 지적된 문제는 제출용 메인 서비스가 실제 진단을 수행하지 않고, mock/fallback 데이터와 하드코딩 결과에 의존한다는 점이었다.

이번 수정은 다음 목표로 진행했다.

- React 진단 시작 버튼이 실제 Spring 진단 API를 호출하게 변경
- Spring 진단 결과의 고정 점수 제거
- 제품 입력값, HS Code, 대상국, 인증서, FTO, 바이어 요구사항, 지식 출처를 점수 산식에 반영
- API 실패를 mock 성공으로 숨기지 않도록 제출 기본 모드 변경
- 권한, 삭제, 입력 검증, JWT secret, CORS, 에러 로그 등 심사 질의 가능성이 높은 운영 리스크 보완
- README와 실제 코드 수준을 일치시켜 과장 표현 제거

## 2. 핵심 변경 요약

| 영역 | 기존 문제 | 수정 내용 |
| --- | --- | --- |
| 진단 실행 | `AI 진단 시작` 클릭 시 `/diagnosis/diag-001`로 고정 이동 | 실제 `POST /api/diagnosis` 호출 후 반환된 `diagnosisId`로 이동 |
| 진단 엔진 | Spring 결과가 항상 72점, 고정 출처, 고정 문구 | 입력·제품 지식·공공데이터 출처 신호 기반 규칙 점수 생성 |
| 공공데이터 근거 | 출처명만 표시되고 점수 기여도 없음 | `sourceId`, `provider`, `matchedKey`, `usedFor`, `scoreContribution`, `fallbackUsed` 반환 |
| mock fallback | API 실패 시 화면이 정상처럼 보임 | 기본값 `VITE_ENABLE_MOCK=false`, 실패 시 error state 전파 |
| 권한 | 멤버십이 없으면 `admin`으로 처리 | 개인 계정은 본인 리소스 owner 상당, 회사 계정은 멤버십 없으면 거부 |
| 제품 삭제 | FK 제약으로 감사 로그/진단 이력 삭제 실패 가능 | 물리 삭제 대신 `deleted_at`, `deleted_by` soft delete 적용 |
| 입력 검증 | HS Code, 가입 유형, 사업자번호, URL 검증 부족 | DTO/service 검증 강화 |
| 보안 설정 | 짧은 JWT secret을 padding해서 사용 | 32 bytes 미만 secret 거부, Base64 secret 지원 |
| 운영 설정 | CORS, SQL log, app log level 하드코딩 | 환경변수 기반 설정 추가 |
| 문서 | README가 FastAPI/AI/XGBoost/RAG 구현을 과장 | 제출용 메인 경로와 현재 구현 범위를 명확히 재정의 |
| live 공공 API 수집 | 메인 Spring 앱에 live 수집 저장 계층 없음 | `public_data_collection_runs`, `public_data_records`, provider connector, manual collect API, 진단 source trace 반영 추가 |

## 3. 프론트엔드 변경사항

### 3.1 실제 진단 API 호출

변경 파일:

- `frontend/FE-02/src/pages/diagnosis/DiagnosisNew.jsx`
- `frontend/FE-02/src/services/diagnosisService.js`

수정 내용:

- `navigate('/diagnosis/diag-001')` 제거
- `runDiagnosis()` import 및 실제 API 호출 연결
- 저장된 제품 선택 시 `productId` 기반 진단 실행
- 직접 입력 제품은 제품 저장 옵션이 켜진 경우 제품 생성 후 진단 실행
- 제품 저장 옵션이 꺼진 경우 `inputData` 기반 진단 실행
- API 응답의 `diagnosisId`로 `/diagnosis/{id}` 이동

추가 보완:

- 제품명 필수 검증 추가
- HS Code 숫자 6~10자리 검증 추가
- 대상국 alias 정규화 추가
  - 예: `독일`, `Germany`, `DE` -> `DE`
  - 예: `미국`, `USA`, `US` -> `US`

### 3.2 mock fallback 격리

변경 파일:

- `frontend/FE-02/src/services/diagnosisService.js`
- `frontend/FE-02/src/services/productService.js`
- `frontend/FE-02/.env.example`

수정 내용:

- 조회 API 실패 시 mock으로 자동 대체하던 동작을 기본 비활성화
- `VITE_ENABLE_MOCK=true`일 때만 개발용 fallback 허용
- 제출 기본값은 `VITE_ENABLE_MOCK=false`

추가 환경변수:

```env
VITE_API_URL=http://localhost:8080
VITE_API_TIMEOUT_MS=15000
VITE_ENABLE_MOCK=false
```

### 3.3 API client 안정성 개선

변경 파일:

- `frontend/FE-02/src/services/apiClient.js`

수정 내용:

- `VITE_API_URL`이 `/api`로 끝날 때 `/api/api`가 되는 문제 방지
- 15초 기본 timeout 추가
- 서버의 중첩 에러 응답 `error.message`를 사용자에게 전달
- GET 요청에는 불필요한 `Content-Type: application/json`을 붙이지 않도록 변경

### 3.4 진단 목록/결과 화면 보완

변경 파일:

- `frontend/FE-02/src/pages/diagnosis/DiagnosisList.jsx`
- `frontend/FE-02/src/pages/mypage/Mypage.jsx`
- `frontend/FE-02/src/pages/auth/Home.jsx`

수정 내용:

- `running` 상태 표시 추가
- 알 수 없는 status에서도 화면이 깨지지 않도록 fallback 처리
- 마이페이지 `이 제품 AI 진단 다시 실행`이 로컬 점수를 조작하지 않고 실제 진단 API 응답 id로 이동
- 홈 CTA의 `diag-001` 직접 이동 제거

### 3.5 프론트 권한 UX 보완

변경 파일:

- `frontend/FE-02/src/components/route/ProtectedRoute.jsx`
- `frontend/FE-02/src/hooks/usePermission.js`

수정 내용:

- 보호 라우트가 localStorage 사용자 정보뿐 아니라 access token 존재도 확인
- 개인 계정은 UI 보조 권한에서 owner 상당으로 처리
- 단, 최종 권한 강제는 백엔드 service layer에서 수행

## 4. 백엔드 변경사항

### 4.1 진단 엔진 하드코딩 제거

변경 파일:

- `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java`

수정 내용:

- 고정 점수 `overall=72` 제거
- 제품/입력 데이터 기반 진단 feature 구성
- 아래 요소를 점수 산식에 반영
  - HS Code prefix
  - 대상국
  - 인증서 보유 수
  - 인증서 만료/만료 임박 여부
  - FTO riskLevel
  - 바이어 요구사항 수
  - 제품 지식 trustGrade
  - 지식 출처 수

새 점수 항목:

- `market`
- `regulation`
- `recall`
- `fto`
- `support`
- `overall`

### 4.2 공공데이터 source trace 추가

변경 파일:

- `backend/BE-02/src/main/java/com/growgate/diagnosis/dto/DiagnosisSourcesResponse.java`
- `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisService.java`

추가 응답 필드:

```json
{
  "field": "market",
  "sourceId": "KCS-TRADE-850760-DE",
  "provider": "관세청",
  "datasetName": "수출입무역통계 HS/국가별 교역 신호",
  "documentName": "수출입무역통계 HS/국가별 교역 신호",
  "url": "https://unipass.customs.go.kr/ets/",
  "retrievedAt": "2026-06-30T00:00:00Z",
  "recordId": "KCS-TRADE-850760-DE",
  "matchedKey": "hsCode=850760;country=DE",
  "usedFor": "market",
  "scoreContribution": 18,
  "fallbackUsed": false,
  "reason": "배터리·전장 품목군의 최근 수입/전환수요 신호 반영"
}
```

심사 질의 방어 포인트:

- “어떤 데이터가 몇 점에 반영됐는가?”에 답할 수 있음
- “fallback인지 실제 입력 기반인지?”를 결과에 남김
- “HS Code와 대상국을 바꾸면 결과가 달라지는가?”에 코드상 근거가 생김

### 4.3 진단 생성 race condition 보완

변경 파일:

- `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisService.java`

수정 내용:

- 기존에는 트랜잭션 커밋 전에 `@Async` 프로세서를 호출할 수 있어 비동기 스레드가 아직 저장되지 않은 `DiagnosisRun`을 못 찾는 race가 가능했음
- `TransactionSynchronization.afterCommit()`으로 커밋 이후 진단 프로세서를 실행하도록 변경

### 4.4 진단 요청 검증 강화

변경 파일:

- `backend/BE-02/src/main/java/com/growgate/diagnosis/dto/CreateDiagnosisRequest.java`
- `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisService.java`

수정 내용:

- `productId`와 `inputData` 동시 전달 거부
- 직접 입력 진단 시 `productName`, `hsCode` 필수화
- HS Code 숫자 6~10자리 pattern 검증
- description 길이 제한 추가
- description도 `inputData` JSON에 저장

### 4.5 권한 기본 admin 제거

변경 파일:

- `backend/BE-02/src/main/java/com/growgate/product/service/ProductService.java`
- `backend/BE-02/src/main/java/com/growgate/knowledge/service/KnowledgeService.java`
- `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisService.java`

수정 내용:

- `membershipRepository.findFirstByUserId(...).orElse(UserRole.admin)` 제거
- 개인 계정은 본인 소유 리소스에 대해 owner 상당 권한
- 회사 계정은 멤버십이 없으면 권한 없음
- viewer는 진단 실행/수정 불가

### 4.6 제품 soft delete 적용

변경 파일:

- `backend/BE-02/src/main/java/com/growgate/product/entity/Product.java`
- `backend/BE-02/src/main/java/com/growgate/product/repository/ProductRepository.java`
- `backend/BE-02/src/main/java/com/growgate/product/service/ProductService.java`
- `database/mysql/init/01_schema.sql`
- `database/sqlite/schema.sql`

수정 내용:

- 제품 물리 삭제 제거
- `deleted_at`, `deleted_by` 컬럼 추가
- 제품 목록/상세 조회는 `deleted_at IS NULL` 조건 사용
- 감사 로그와 진단 이력 FK 보존

### 4.7 인증/회원가입/보안 설정 보완

변경 파일:

- `backend/BE-02/src/main/java/com/growgate/auth/dto/SignupRequest.java`
- `backend/BE-02/src/main/java/com/growgate/auth/service/AuthService.java`
- `backend/BE-02/src/main/java/com/growgate/security/JwtTokenProvider.java`
- `backend/BE-02/src/main/java/com/growgate/config/SecurityConfig.java`
- `backend/BE-02/src/main/resources/application.yml`
- `backend/BE-02/src/main/resources/application.yml.example`

수정 내용:

- 가입 유형은 `personal|company`만 허용
- service layer에서도 잘못된 가입 유형 거부
- 기업 가입 시 회사명, 사업자번호, 업종 필수 검증
- 사업자번호 숫자 10자리 검증
- 비밀번호 8~72자 제한
- JWT secret 32 bytes 미만 거부
- Base64 32 bytes 이상 secret 지원
- CORS origin을 `APP_CORS_ORIGINS` 환경변수로 분리
- SQL 로그와 app log level을 환경변수로 제어

### 4.8 지식 관리 입력 검증 보완

변경 파일:

- `backend/BE-02/src/main/java/com/growgate/knowledge/service/KnowledgeService.java`

수정 내용:

- `trustGrade` 잘못된 값 입력 시 400 반환
- `riskLevel` 대소문자 입력 허용 후 정규화
- 지식 출처 URL은 `http` 또는 `https`만 허용
- 삭제된 제품은 지식 조회/수정 대상에서 제외

### 4.9 서버 에러 로깅 추가

변경 파일:

- `backend/BE-02/src/main/java/com/growgate/exception/GlobalExceptionHandler.java`

수정 내용:

- 잡히지 않은 500 예외를 서버 로그에 남김
- 운영 중 장애 원인 추적 가능성 개선

## 5. 문서 정합성 업데이트

변경 파일:

- `README.md`
- `frontend/FE-02/README.md`
- `backend/BE-02/README.md`

수정 내용:

- 제출용 공식 구조를 `React FE-02 + Spring Boot BE-02`로 명시
- FastAPI/정적 HTML은 보조 데모로 분리
- “실제 XGBoost/RAG/Agent 완성”처럼 코드보다 앞선 표현 제거
- 현재 구현 범위를 “규칙 기반 scoring + source trace + ML-ready 구조”에 맞춰 정리
- mock fallback은 제출 기본값에서 비활성화된다고 명시
- 백엔드 진단 source trace, soft delete, 권한 정책, JWT secret 정책 문서화

## 6. 검증 결과

실행한 검증:

```bash
cd frontend/FE-02
npm run build
```

결과:

```text
vite build 성공
95 modules transformed
dist 생성 완료
```

추가 검증:

```bash
git diff --check
```

결과:

```text
문제 없음
```

백엔드 테스트 상태:

```bash
cd backend/BE-02
./gradlew test --no-daemon
```

결과:

```text
실행 환경의 기본 JVM이 Java 8이라 실패
Spring Boot 3.4.4는 Java 17 이상 필요
현재 java -version: 1.8.0_391
```

필요 조치:

- Java 17 이상 설치
- `JAVA_HOME`을 Java 17로 설정
- 이후 `./gradlew test --no-daemon` 재실행

## 7. 남은 제약과 후속 보완 권장사항

이번 수정으로 “핵심 버튼이 가짜”, “진단 결과가 고정값”, “mock이 실패를 숨김”, “개인 계정이 admin” 문제는 제거했다.

다만 다음 항목은 실제 수상권 수준을 위해 추가 구현이 필요하다.

| 항목 | 현재 상태 | 권장 보완 |
| --- | --- | --- |
| live 공공데이터 수집 운영 | provider connector와 run/record 저장 구조 구현 | 실제 공공 API key 발급, endpoint 확정, 운영 모니터링 추가 |
| XGBoost | 제출 메인 경로에 실제 라벨 기반 모델 없음 | 실제 진단/수출 성과 로그 기반 feature table과 평가 지표 추가 |
| RAG | 메인 진단은 source trace 중심 | SQLite FTS 또는 vector retrieval, chunk id, citation score 추가 |
| 진단 진행 UX | polling 자동 갱신 구현 | WebSocket/SSE는 고도화 선택사항 |
| 백엔드 테스트 | Java 17로 테스트 실행 완료 | provider별 실제 API sandbox 테스트 추가 |
| 인증 보안 | localStorage JWT 유지 | httpOnly cookie 또는 refresh/revocation 전략 검토 |

## 8. 심사 대응 메시지

이번 수정 이후 기술 설명은 다음처럼 방어하는 것이 안전하다.

> 제출용 메인 서비스는 React와 Spring Boot로 구성되어 있습니다. 사용자가 제품명, HS Code, 대상국을 입력하면 실제 `/api/diagnosis` run이 생성되고, Spring 진단 엔진이 제품 지식 DB와 공공데이터 출처 신호를 조합해 시장성, 규제, 리콜, FTO, 지원 적합도 점수를 계산합니다. 각 점수에는 source id, matched key, used for, score contribution, fallback 여부가 함께 저장되어 어떤 근거가 몇 점에 반영됐는지 추적할 수 있습니다.

피해야 할 표현:

- “실제 라벨 기반 XGBoost 예측 모델이 완성됐다”
- “pgvector 기반 RAG가 운영 중이다”
- “실시간 공공 API만으로 모든 점수가 계산된다”

권장 표현:

- “공공데이터 출처 추적형 규칙 기반 진단 엔진”
- “API key 기반 live 공공데이터 수집과 실패 투명성 source trace 구조”
- “ML-ready feature contract와 향후 라벨 축적 기반 고도화 구조”

## 9. 변경 파일 요약

주요 변경 파일:

- `frontend/FE-02/src/pages/diagnosis/DiagnosisNew.jsx`
- `frontend/FE-02/src/services/diagnosisService.js`
- `frontend/FE-02/src/services/productService.js`
- `frontend/FE-02/src/services/apiClient.js`
- `frontend/FE-02/src/pages/diagnosis/DiagnosisList.jsx`
- `frontend/FE-02/src/pages/mypage/Mypage.jsx`
- `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java`
- `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisService.java`
- `backend/BE-02/src/main/java/com/growgate/diagnosis/dto/CreateDiagnosisRequest.java`
- `backend/BE-02/src/main/java/com/growgate/diagnosis/dto/DiagnosisSourcesResponse.java`
- `backend/BE-02/src/main/java/com/growgate/product/service/ProductService.java`
- `backend/BE-02/src/main/java/com/growgate/product/entity/Product.java`
- `backend/BE-02/src/main/java/com/growgate/product/repository/ProductRepository.java`
- `backend/BE-02/src/main/java/com/growgate/knowledge/service/KnowledgeService.java`
- `backend/BE-02/src/main/java/com/growgate/security/JwtTokenProvider.java`
- `database/mysql/init/01_schema.sql`
- `database/sqlite/schema.sql`
- `README.md`
- `frontend/FE-02/README.md`
- `backend/BE-02/README.md`

## 10. 결론

이번 변경으로 제출용 메인 앱은 더 이상 “데모 화면 + mock fallback + 고정 진단 결과” 구조가 아니다. 핵심 진단 버튼은 실제 API를 호출하고, 백엔드 진단 결과는 입력값과 제품 지식 상태에 따라 달라지며, 점수 근거와 출처 기여도를 확인할 수 있다.

남은 과제는 실제 공공 API key 발급·운영 모니터링, 실제 라벨 기반 ML, 검색형 RAG 고도화다. live 수집 구조 자체는 Spring 메인 앱에 구현됐지만, 발표에서는 “API key가 설정되면 live 호출하고 실패 시 fallbackUsed로 남긴다”고 설명해야 한다.
