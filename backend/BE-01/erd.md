# ERD 

> 테이블 간 관계 요약
> - `Users` 1 : N `Diagnosis Runs` (개인 계정도 진단 가능)
> - `Companies` 1 : N `Diagnosis Runs` (기업 계정 진단 시)
> - `Products` 1 : 1 `Product Knowledge`
> - `Products` 1 : N `Certifications`, `FTO`, `Buyer Requirements`, `Audit Logs`
> - `Product Knowledge` 1 : N `Knowledge Sources`

---

### 1. Users (사용자 계정)

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 사용자 고유 ID |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 로그인 이메일 |
| password_hash | VARCHAR | NOT NULL | 암호화된 비밀번호 |
| name | VARCHAR(100) | NOT NULL | 사용자 이름 |
| user_type | ENUM('personal', 'company') | NOT NULL | 개인 / 기업 구분 |
| company_id | UUID | FK → companies.id, NULL | 기업 계정일 때만 값 존재. 개인 계정은 NULL |
| created_at | TIMESTAMP | NOT NULL | 가입일 |
| updated_at | TIMESTAMP | NOT NULL | 최종 수정일 |

---

### 2. Companies (기업 정보)

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 기업 고유 ID |
| name | VARCHAR(200) | NOT NULL | 기업명 |
| business_number | VARCHAR(20) | UNIQUE | 사업자등록번호 |
| industry | VARCHAR(100) | - | 업종 |
| created_at | TIMESTAMP | NOT NULL | 등록일 |
| updated_at | TIMESTAMP | NOT NULL | 최종 수정일 |

---

### 3. Products (제품)

> Users 또는 Companies가 등록한 제품, 제품 지식 관리의 기준 단위

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 제품 고유 ID |
| owner_user_id | UUID | FK → users.id, NOT NULL | 제품 등록자 |
| name | VARCHAR(200) | NOT NULL | 제품명 |
| category | VARCHAR(100) | - | 제품 카테고리 |
| description | TEXT | - | 제품 설명 |
| created_at | TIMESTAMP | NOT NULL | 등록일 |
| updated_at | TIMESTAMP | NOT NULL | 최종 수정일 |

---

### 4. Product Knowledge (제품별 수출 지식)

> - 제품 등록 시 자동 생성되며 신뢰도/출처/메모를 관리
> - AI 진단 시 이 테이블의 내용이 `knowledge_snapshot`으로 복사됨

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 지식 고유 ID |
| product_id | UUID | FK → products.id, UNIQUE, NOT NULL | 연결 제품 (1:1) |
| trust_grade | ENUM('high', 'medium', 'low') | - | 신뢰도 등급. API 응답의 `trustGrade` 필드에 대응 |
| source_type | VARCHAR(100) | - | 출처 유형 (공식문서 / 자체조사 등). API의 `sourceType` 필드에 대응 |
| notes | TEXT | - | 추가 메모 |
| created_by | UUID | FK → users.id | 최초 작성자 |
| updated_by | UUID | FK → users.id | 최종 수정자 |
| created_at | TIMESTAMP | NOT NULL | 작성일 |
| updated_at | TIMESTAMP | NOT NULL | 수정일 |

---

### 5. Certifications (인증서 정보)

> `daysUntilExpiry`는 DB 컬럼이 아닌 API 응답 시 `expires_at` 기준으로 서버에서 계산해서 내려주는 값

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 인증서 고유 ID |
| product_id | UUID | FK → products.id, NOT NULL | 연결 제품 |
| name | VARCHAR(200) | NOT NULL | 인증서명 (CE, KC 등) |
| issued_at | DATE | - | 발급일 |
| expires_at | DATE | - | 만료일. 90일 이내면 만료 임박 처리 |
| renewal_cycle | INTEGER | - | 갱신 주기 (개월 단위) |
| created_at | TIMESTAMP | NOT NULL | 등록일 |

---

### 6. FTO (특허 분석 정보)

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | FTO 고유 ID |
| product_id | UUID | FK → products.id, NOT NULL | 연결 제품 |
| target_country | VARCHAR(100) | NOT NULL | 분석 대상 국가 |
| risk_level | ENUM('high', 'medium', 'low') | - | 위험 등급 |
| summary | TEXT | - | 분석 요약 |
| expires_at | DATE | - | 유효 기간 |
| created_at | TIMESTAMP | NOT NULL | 등록일 |

---

### 7. Buyer Requirements (바이어별 요구사항)

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 요구사항 고유 ID |
| product_id | UUID | FK → products.id, NOT NULL | 연결 제품 |
| buyer_name | VARCHAR(200) | NOT NULL | 바이어명 |
| country | VARCHAR(100) | - | 바이어 국가 |
| requirements | TEXT | NOT NULL | 요구 조건 내용 |
| created_at | TIMESTAMP | NOT NULL | 등록일 |

---

### 8. Audit Logs (제품 지식 수정 이력)

> `PUT /api/products/:productId/knowledge` 호출 시 백엔드 자동 기록

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 로그 고유 ID |
| product_id | UUID | FK → products.id, NOT NULL | 대상 제품 |
| changed_by | UUID | FK → users.id, NOT NULL | 수정한 사용자 |
| change_type | ENUM('create', 'update', 'delete') | NOT NULL | 변경 유형 |
| changed_fields | JSON | - | 변경된 필드 목록. 예: `["trustGrade", "notes"]` |
| changed_at | TIMESTAMP | NOT NULL | 변경 시각 |

---

### 9. Knowledge Sources (지식 출처 원문)

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 출처 고유 ID |
| knowledge_id | UUID | FK → product_knowledge.id, NOT NULL | 연결 지식 항목 |
| url | TEXT | - | 출처 URL |
| document_name | VARCHAR(200) | - | 문서명 |
| retrieved_at | DATE | - | 수집일 |

---

### 10. Diagnosis Runs (AI 진단 실행 기록)

> - `user_id` 기준으로 "내 진단 목록"([9] GET /api/diagnosis)을 조회
> - 개인 계정도 진단 가능하므로 `company_id`와 `product_id`는 nullable
> - 직접 입력(productId 없이) 진단 시에는 `product_id`가 NULL이고 `input_data`에 제품명/HS Code가 저장됨
> - `knowledge_snapshot`은 productId 기반 진단에서만 생성되므로 nullable

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 진단 고유 ID |
| user_id | UUID | FK → users.id, NOT NULL | 진단 요청 사용자. "내 진단 목록" 조회 기준 |
| company_id | UUID | FK → companies.id, NULL | 기업 계정 진단 시에만 값 존재 |
| product_id | UUID | FK → products.id, NULL | 저장된 제품 지식 기반 진단 시에만 값 존재. 직접 입력 시 NULL |
| input_data | JSON | NULL | 직접 입력 진단 시 제품명·HS Code 저장. `product_id`가 있으면 NULL |
| knowledge_snapshot | JSON | NULL | 진단 시점 제품 지식 복사본. `product_id`가 있을 때만 생성. API 응답의 `knowledgeSnapshot` 필드에 대응 |
| result | JSON | - | AI 진단 결과. API 응답의 `result` 필드에 대응 |
| status | ENUM('pending', 'done', 'failed') | NOT NULL | 진단 상태 |
| target_country | VARCHAR(100) | NOT NULL | 진단 대상 국가 |
| created_at | TIMESTAMP | NOT NULL | 진단 요청일 |
