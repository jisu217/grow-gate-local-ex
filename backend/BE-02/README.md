# GROW-GATE Local EX — Backend

---

## Table of Contents

1. [Tech Stack](#1-tech-stack)
2. [Getting Started](#2-getting-started)
3. [Project Structure](#3-project-structure)
4. [API Endpoints](#4-api-endpoints)
5. [Permission Structure](#5-permission-structure)
6. [Architecture Decisions](#6-architecture-decisions)

---

## 1. Tech Stack

| 항목 | 내용 |
| :--- | :--- |
| 프레임워크 | Spring Boot 3.4.4 |
| 언어 | Java 17 |
| DB | MySQL 8.0 |
| ORM | Spring Data JPA (Hibernate) |
| 인증 | JWT (jjwt 0.11.5) |
| 빌드 | Gradle 8.13 (Wrapper 포함) |

---

## 2. Getting Started

### 요구 사항

- Java `17` 이상
- MySQL `8.0` 이상
- Gradle 별도 설치 불필요 (`./gradlew` Wrapper 포함)

### 설치 및 실행

```bash
# 1. MySQL DB 생성
CREATE DATABASE growgate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 2. 스키마 적용 또는 로컬 개발용 DDL 자동 갱신 선택
# 운영/제출 기본: database/mysql/init/01_schema.sql 적용 후 JPA_DDL_AUTO=validate
# 빠른 로컬 개발: export JPA_DDL_AUTO=update

# 3. 설정 파일 준비
cp src/main/resources/application.yml.example src/main/resources/application.yml

# 4. 서버 실행
./gradlew bootRun
```

실행 후 → `http://localhost:8080`

### 환경 변수

```bash
export DB_PASSWORD=your_local_db_password
export JWT_SECRET=your_jwt_secret_base64_encoded
export APP_FASTAPI_ENABLED=false
export PUBLIC_DATA_ENABLED=false
```

> `application.yml`에 직접 값을 입력하거나 위처럼 환경변수로 주입하면 됩니다.  
> JWT_SECRET 생성: `openssl rand -base64 32`

| 변수 | 설명 | 기본값 |
| :--- | :--- | :--- |
| `DB_URL` | MySQL 접속 URL | `jdbc:mysql://localhost:3306/growgate` |
| `DB_USERNAME` | DB 사용자 | `root` |
| `DB_PASSWORD` | DB 비밀번호 | 없음 (필수) |
| `JWT_SECRET` | JWT 서명 시크릿 (Base64) | 없음 (필수) |
| `JWT_EXPIRATION` | JWT 만료 시간 (ms) | `86400000` (24시간) |
| `APP_CORS_ORIGINS` | 허용 CORS origin CSV | 로컬 개발 포트 |
| `APP_FASTAPI_ENABLED` | FastAPI `/analyze`, `/report` bridge 사용 여부 | `false` |
| `APP_FASTAPI_BASE_URL` | FastAPI backend URL | `http://localhost:8000` |
| `PUBLIC_DATA_ENABLED` | 공공 API live 수집 사용 여부 | `false` |
| `PUBLIC_DATA_CACHE_TTL_HOURS` | live/cache record 재사용 시간 | `24` |
| `DATAGO_SERVICE_KEY` | 공공데이터포털 일반 인증키 공통 alias | 없음 |
| `KCS_TRADE_API_URL`, `KCS_TRADE_API_KEY` | 관세청 수출입무역통계 API URL/key. key 미설정 시 `DATAGO_SERVICE_KEY` 사용 | `http://apis.data.go.kr/1220000/Newtrade/getNewtradeList` |
| `KOTRA_MARKET_API_URL`, `KOTRA_MARKET_API_KEY` | KOTRA 시장/뉴스 API URL/key. key 미설정 시 `DATAGO_SERVICE_KEY` 사용 | `https://www.data.go.kr/data/15122648/openapi.do` |
| `SAFETY_RECALL_API_URL`, `SAFETY_RECALL_API_KEY` | 제품안전/리콜 API URL/key. key 미설정 시 `DATAGO_SERVICE_KEY` 사용 | `https://www.safetykorea.kr/release/openapi` |
| `KIPRIS_API_URL`, `KIPRIS_API_KEY` | KIPRIS Plus API URL/key. key 미설정 시 `KIPRIS_PLUS_KEY` 사용 | `https://plus.kipris.or.kr/eng/main/contents.do?menuNo=300024` |
| `BIZINFO_API_URL`, `BIZINFO_API_KEY` | 기업마당/수출바우처 지원사업 API URL/key. key 미설정 시 `DATAGO_SERVICE_KEY` 사용 | `https://www.exportvoucher.com/` |
| `JPA_DDL_AUTO` | Hibernate DDL 정책 | `validate` |
| `JPA_SHOW_SQL` | SQL 로그 출력 여부 | `false` |
| `APP_LOG_LEVEL` | `com.growgate` 로그 레벨 | `INFO` |

KOTRA, Safety Korea, KIPRIS, 수출바우처 기본 URL은 공식 카탈로그/신청 페이지입니다. `PUBLIC_DATA_ENABLED=true` 상태에서도 이 URL이 그대로면 HTML 상세 페이지를 API 응답으로 저장하지 않고 fallback record에 "operation endpoint 필요" 사유를 남깁니다. 활용신청 후 발급 화면의 실제 요청 URL을 각 `*_API_URL`에 넣으면 live 호출로 전환됩니다.

---

## 3. Project Structure

```text
BE-02/
├── .gitignore
├── build.gradle
├── settings.gradle
├── gradlew                                     # Mac/Linux 실행 스크립트
├── gradlew.bat                                 # Windows 실행 스크립트
├── gradle/wrapper/
│   └── gradle-wrapper.properties               # Gradle 8.13
└── src/
    ├── main/resources/
    │   ├── application.yml                     # 환경변수 참조, git 포함
    │   └── application.yml.example             # 설정 템플릿
    └── main/java/com/growgate/
        ├── GrowGateApplication.java            # @EnableAsync
        │
        ├── auth/
        │   ├── controller/AuthController.java      # 로그인·회원가입 API 엔드포인트
        │   ├── service/AuthService.java            # 인증 로직 (JWT 발급, 비밀번호 검증)
        │   └── dto/                                # LoginRequest, SignupRequest, Response
        │
        ├── user/
        │   ├── entity/User.java                    # 회원 엔티티
        │   ├── enums/                              # UserType (personal | company), UserRole (owner | admin | editor | viewer)
        │   ├── repository/UserRepository.java      # 회원 DB 조회
        │   ├── service/UserService.java            # 회원 정보 조회·수정
        │   ├── controller/UserController.java      # 내 정보 API 엔드포인트
        │   └── dto/                                # UserMeResponse, UpdateUserRequest·Response
        │
        ├── company/
        │   ├── entity/Company.java                 # 회사 엔티티
        │   ├── repository/CompanyRepository.java   # 회사 DB 조회
        │   ├── service/CompanyService.java         # 회사 정보 조회·수정
        │   ├── controller/CompanyController.java   # 내 회사 정보 API 엔드포인트
        │   └── dto/                                # CompanyMeResponse, UpdateCompanyRequest·Response
        │
        ├── product/
        │   ├── entity/Product.java                 # 상품 엔티티
        │   ├── repository/ProductRepository.java   # 상품 DB 조회
        │   ├── service/ProductService.java         # 제품 목록·생성·삭제
        │   ├── controller/ProductController.java   # 제품 API 엔드포인트 (/api/products)
        │   └── dto/                                # List·Create·Delete Request·Response
        │
        ├── knowledge/
        │   ├── entity/                             # ProductKnowledge, Certification, Fto, BuyerRequirement, AuditLog, KnowledgeSource
        │   ├── enums/                              # TrustGrade, RiskLevel, ChangeType
        │   ├── repository/                         # 각 엔티티별 Repository
        │   ├── service/KnowledgeService.java       # 제품 지식·인증서·FTO·바이어요구사항·출처 CRUD + 감사 로그
        │   ├── controller/KnowledgeController.java # 제품 지식 하위 리소스 API 엔드포인트
        │   └── dto/                                # Knowledge/Certification/Fto/BuyerRequirement/Source Request·Response, Logs
        │
        ├── publicdata/
        │   ├── controller/PublicDataController.java # live/cache/fallback 수집 API
        │   ├── service/                             # provider connector + collection orchestration
        │   ├── entity/                              # PublicDataCollectionRun, PublicDataRecord
        │   ├── repository/                          # 수집 run/record 조회
        │   └── dto/                                 # collect/runs/records 응답
        │
        ├── diagnosis/
        │   ├── entity/DiagnosisRun.java            # 진단 실행 엔티티
        │   ├── enums/DiagnosisStatus.java          # pending | running | done | failed
        │   ├── repository/DiagnosisRepository.java # 진단 DB 조회
        │   ├── service/DiagnosisService.java       # 진단 생성·조회
        │   ├── service/DiagnosisAsyncProcessor.java # @Async 진단 실행 처리
        │   ├── service/FastApiAgentClient.java      # optional FastAPI analyze/report bridge
        │   ├── controller/DiagnosisController.java # 진단 API 엔드포인트
        │   └── dto/                                # Create·List·Detail·Sources Request·Response
        │
        ├── security/                           # JwtTokenProvider, JwtAuthenticationFilter, UserDetailsServiceImpl, TokenRevocationService
        ├── config/                             # SecurityConfig, JacksonConfig, HttpClientConfig
        ├── common/                             # dto/ErrorResponse, converter/StringListConverter
        └── exception/                          # ErrorCode, BusinessException, GlobalExceptionHandler
```

---

## 4. API Endpoints

모든 인증 필요 API는 요청 헤더에 `Authorization: Bearer {token}` 포함 필요.
응답 필드 네이밍: camelCase 고정

### 헬스체크

| 메서드 | 경로 | 설명 | 인증 |
| :--- | :--- | :--- | :--- |
| `GET` | `/health` | 서버 상태 확인 (배포·연결 체크용) | |

### 인증

| 메서드 | 경로 | 설명 | 인증 |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | 이메일·비밀번호 로그인, JWT 발급 | |
| `POST` | `/api/auth/logout` | 로그아웃 | |
| `POST` | `/api/auth/signup` | 회원가입 (`type`: personal/company로 구분) | |

### 사용자

| 메서드 | 경로 | 설명 | 인증 |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/users/me` | 내 정보 조회 | ✓ |
| `PATCH` | `/api/users/me` | 내 정보 수정 | ✓ |

### 회사

| 메서드 | 경로 | 설명 | 인증 |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/companies/me` | 내 회사 정보 조회 | ✓ |
| `PATCH` | `/api/companies/me` | 내 회사 정보 수정 | ✓ |

### 제품

| 메서드 | 경로 | 설명 | 필요 역할 |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/products` | 제품 목록 조회 | 전체 |
| `POST` | `/api/products` | 제품 생성 | owner, admin |
| `DELETE` | `/api/products/{productId}` | 제품 소프트 삭제 (`deleted_at`, 감사 로그·진단 이력 유지) | owner, admin |

### 제품 지식

| 메서드 | 경로 | 설명 | 필요 역할 |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/products/{productId}/knowledge` | 제품 상세·지식 조회 (인증서·FTO·바이어요구사항·출처 포함) | 전체 |
| `PUT` | `/api/products/{productId}/knowledge` | 제품 정보·지식 수정 | owner, admin, editor |
| `GET` | `/api/products/{productId}/audit-logs` | 감사 로그 조회 | owner, admin |

### 인증서 / FTO / 바이어 요구사항 / 지식 출처

| 메서드 | 경로 | 설명 | 필요 역할 |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/products/{productId}/certifications` | 인증서 등록 | owner, admin, editor |
| `PUT` | `/api/products/{productId}/certifications/{certificationId}` | 인증서 수정 | owner, admin, editor |
| `DELETE` | `/api/products/{productId}/certifications/{certificationId}` | 인증서 삭제 | owner, admin, editor |
| `POST` | `/api/products/{productId}/fto` | FTO 항목 등록 | owner, admin, editor |
| `PUT` | `/api/products/{productId}/fto/{ftoId}` | FTO 항목 수정 | owner, admin, editor |
| `DELETE` | `/api/products/{productId}/fto/{ftoId}` | FTO 항목 삭제 | owner, admin, editor |
| `POST` | `/api/products/{productId}/buyer-requirements` | 바이어 요구사항 등록 | owner, admin, editor |
| `PUT` | `/api/products/{productId}/buyer-requirements/{buyerRequirementId}` | 바이어 요구사항 수정 | owner, admin, editor |
| `DELETE` | `/api/products/{productId}/buyer-requirements/{buyerRequirementId}` | 바이어 요구사항 삭제 | owner, admin, editor |
| `POST` | `/api/products/{productId}/knowledge/sources` | 지식 출처 등록 | owner, admin, editor |
| `DELETE` | `/api/products/{productId}/knowledge/sources/{sourceId}` | 지식 출처 삭제 | owner, admin, editor |

### 진단

| 메서드 | 경로 | 설명 | 필요 역할 |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/diagnosis` | 진단 이력 목록 조회 | 전체 |
| `POST` | `/api/diagnosis` | 새 진단 실행 (같은 회사 소유 제품이면 누가 등록했든 가능) | owner, admin, editor |
| `GET` | `/api/diagnosis/{diagnosisId}` | 진단 결과 상세 조회 | 전체 |
| `GET` | `/api/diagnosis/{diagnosisId}/sources` | 진단 출처 데이터 조회 (`sourceId`, `provider`, `matchedKey`, `usedFor`, `scoreContribution`, `fallbackUsed`) | 전체 |
| `GET` | `/api/diagnosis/{diagnosisId}/report` | 진단 리포트 Markdown 다운로드 payload | 전체 |

### 공공데이터 수집

| 메서드 | 경로 | 설명 | 필요 역할 |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/public-data/collect` | HS Code/대상국/제품명 기준 provider별 live/cache/fallback 수집 | 로그인 |
| `GET` | `/api/public-data/runs` | 최근 공공데이터 수집 run 조회 | 로그인 |
| `GET` | `/api/public-data/records` | 최근 공공데이터 record 조회 (`hsCode`, `targetCountry` optional) | 로그인 |

---

## 5. Permission Structure

역할 계층: `owner` > `admin` > `editor` > `viewer`

| 액션 | owner | admin | editor | viewer |
| :--- | :--- | :--- | :--- | :--- |
| 제품 생성 | ✓ | ✓ | | |
| 제품 지식 수정 | ✓ | ✓ | ✓ | |
| 제품 삭제 | ✓ | ✓ | | |
| 감사 로그 조회 | ✓ | ✓ | | |
| 진단 실행 | ✓ | ✓ | ✓ | |
| 조회 전체 | ✓ | ✓ | ✓ | ✓ |

- 기업 계정 최초 가입 시 → `owner` 자동 부여
- 개인 계정은 회사 멤버십 없이 본인 소유 리소스에 한해 `owner` 상당 권한으로 처리
- 회사 계정은 멤버십이 없으면 회사 리소스 작업 권한 없음
- 기업 계정 사용자는 같은 회사에 속한 모든 제품에 접근 (`company_id` 기준)
- **프론트의 UI 숨김 처리는 보조적 UX**, 백엔드에서 반드시 역할 재검증

---

## 6. Architecture Decisions

| 항목 | 설계 내용 |
| :--- | :--- |
| JWT subject | userId(UUID) 저장, email 기반 로그인 후 토큰 발급 |
| 로그인 응답 | accessToken + userId + userType + **role** + companyId |
| Diagnosis Flow | POST → `pending` → `running`(async 시작) → `done`/`failed` |
| Knowledge Snapshot | productId 기반 진단 시 자동 생성 → JSON(TEXT) 저장 |
| Audit Log | changedFields + **previousValue** + **newValue** + **reason** 저장 |
| Audit Tracking | PATCH 요청 시 old/new 비교 → 변경 필드 + 값 함께 저장 |
| Knowledge Status | 만료, 만료임박, 검토필요, 완료, 미완성 자동 계산 |
| Diagnosis Result | 제품/입력, HS Code, 대상국, 인증서 만료, FTO, 바이어 요구사항, 지식 출처 기반 규칙 점수 + source trace 반환 |
| Public Data Collection | provider별 API key가 있으면 live 호출, 없거나 실패하면 fallback record 저장 후 source trace에 노출 |
| Product Delete | 물리 삭제 대신 `deleted_at`, `deleted_by` 소프트 삭제로 FK 이력 보존 |
| CORS | `APP_CORS_ORIGINS` 환경변수로 관리 |
| 비밀키 | 환경변수 DB_PASSWORD, JWT_SECRET 으로 관리. JWT secret은 32 bytes 이상 또는 32 bytes 이상 Base64 값만 허용 |

---
