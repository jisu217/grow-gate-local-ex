# GrowGate Backend

---

### Getting Started
### 1. 환경변수 설정

```bash
export DB_PASSWORD=your_local_db_password
export JWT_SECRET=your_jwt_secret_base64_encoded
```

### 2. 실행

```bash
./gradlew bootRun
```

---

### Backend Project Structure

```text
BE-02/
├── build.gradle
├── settings.gradle
├── .env.example                             
└── src/main/java/com/growgate/
    ├── GrowGateApplication.java          (@EnableAsync)
    │
    ├── auth/
    │   ├── controller/AuthController.java
    │   ├── service/AuthService.java
    │   └── dto/ (LoginRequest, SignupRequest, Response)
    │
    ├── user/
    │   ├── entity/User.java
    │   ├── enums/UserType.java
    │   ├── enums/UserRole.java            ← NEW: owner|admin|editor|viewer
    │   ├── repository/UserRepository.java
    │   ├── service/UserService.java
    │   ├── controller/UserController.java
    │   └── dto/
    │
    ├── company/
    │   ├── entity/Company.java
    │   ├── repository/CompanyRepository.java
    │   ├── service/CompanyService.java
    │   ├── controller/CompanyController.java
    │   └── dto/
    │
    ├── product/
    │   ├── entity/Product.java
    │   └── repository/ProductRepository.java
    │
    ├── knowledge/
    │   ├── entity/ (ProductKnowledge, Certification, Fto, BuyerRequirement, AuditLog, KnowledgeSource)
    │   ├── enums/ (TrustGrade, RiskLevel, ChangeType)
    │   ├── repository/
    │   ├── service/KnowledgeService.java
    │   ├── controller/KnowledgeController.java
    │   └── dto/
    │
    ├── diagnosis/
    │   ├── entity/DiagnosisRun.java
    │   ├── enums/DiagnosisStatus.java     ← pending|running|done|failed
    │   ├── repository/DiagnosisRepository.java
    │   ├── service/DiagnosisService.java
    │   ├── service/DiagnosisAsyncProcessor.java (@Async)
    │   ├── controller/DiagnosisController.java
    │   └── dto/
    │
    ├── security/
    │   ├── JwtTokenProvider.java
    │   ├── JwtAuthenticationFilter.java
    │   └── UserDetailsServiceImpl.java
    │
    ├── config/
    │   ├── SecurityConfig.java   (CORS + JWT Filter)
    │   └── JacksonConfig.java
    │
    ├── common/
    │   ├── dto/ErrorResponse.java
    │   └── converter/StringListConverter.java
    │
    └── exception/
        ├── ErrorCode.java
        ├── BusinessException.java
        └── GlobalExceptionHandler.java
```

---

### 역할(Role) 시스템

| Role   | 권한 범위 |
| :----- | :--- |
| owner  | 회사, 제품, 멤버 전체 관리 (제품 생성, 삭제, audit 조회 포함) |
| admin  | 제품 지식 등록/수정, 진단 실행, audit 조회 |
| editor | 제품 지식 수정, 진단 실행 |
| viewer | 조회만 가능 |

- 회사 계정 최초 가입 시 → `owner` 자동 부여
- 개인 계정 가입 시 → `admin` 자동 부여
- **백엔드에서 반드시 역할을 재검증**하며, 프론트의 UI 숨김 처리는 보조적 UX에만 해당

### 권한 적용 엔드포인트

| 기능 | 필요 역할 |
| :--- | :--- |
| 제품 지식 등록 | owner, admin |
| 제품 지식 수정 | owner, admin, editor |
| 제품 지식 삭제 | owner, admin |
| 감사 로그 조회 | owner, admin |
| 진단 실행 | owner, admin, editor |

---

### 회사 계정의 제품 접근

회사 계정 사용자는 같은 회사에 속한 모든 제품에 접근 (owner_user_id가 아닌 company_id 기준으로 조회)

---

### Key Architecture Decisions

| Item | Design |
| :--- | :--- |
| JWT subject | userId(UUID) 저장, email 기반 로그인 후 토큰 발급 |
| 로그인 응답 | accessToken + userId + userType + **role** + companyId |
| Diagnosis Flow | POST → `pending` → `running`(async 시작) → `done`/`failed` |
| Knowledge Snapshot | productId 기반 진단 시 자동 생성 → JSON(TEXT) 저장, knowledgeSnapshotId 포함 |
| Audit Log | changedFields + **previousValue** + **newValue** + **reason** 저장 |
| Audit Tracking | PATCH 요청 시 old/new 비교 → 변경 필드 + 값 함께 저장 |
| Knowledge Status | 만료, 만료임박, 검토필요, 완료, 미완성 자동 계산 |
| Diagnosis Result | diagnosisScore + riskItems + actionRoadmap + evidenceSources 반환 |
| CORS | localhost:3000, 5173, 5174 허용 |
| 비밀키 | 환경변수 DB_PASSWORD, JWT_SECRET 로 관리 (하드코딩 금지) |
