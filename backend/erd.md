### 1. Users (개인 사용자 계정)

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 사용자 고유 ID |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 로그인 이메일 |
| password_hash | VARCHAR | NOT NULL | 암호화된 비밀번호 |
| name | VARCHAR(100) | NOT NULL | 사용자 이름 |
| user_type | ENUM('personal', 'company') | NOT NULL | 개인 / 기업 구분 |
| created_at | TIMESTAMP | NOT NULL | 가입일 |
| updated_at | TIMESTAMP | NOT NULL | 최종 수정일 |

### 2. Companies (기업 계정)

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 기업 고유 ID |
| name | VARCHAR(200) | NOT NULL | 기업명 |
| business_number | VARCHAR(20) | UNIQUE | 사업자등록번호 |
| industry | VARCHAR(100) | - | 업종 |
| created_at | TIMESTAMP | NOT NULL | 등록일 |
| updated_at | TIMESTAMP | NOT NULL | 최종 수정일 |

### (수정 중)
### 3. Product Knowledge (제품별 수출 지식)

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 지식 고유 ID |
| product_id | UUID | FK → products.id | 연결 제품 |
| trust_grade | ENUM('high', 'medium', 'low') | - | 신뢰도 태그 |
| source_type | VARCHAR(100) | - | 출처 유형 (공식문서 / 자체조사 등) |
| notes | TEXT | - | 추가 메모 |
| created_by | UUID | FK → users.id | 최초 작성자 |
| updated_by | UUID | FK → users.id | 최종 수정자 |
| created_at | TIMESTAMP | NOT NULL | 작성일 |
| updated_at | TIMESTAMP | NOT NULL | 수정일 |

### 4. Certifications (인증서 정보)

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 인증서 고유 ID |
| product_id | UUID | FK → products.id | 연결 제품 |
| name | VARCHAR(200) | NOT NULL | 인증서명 (CE, KC 등) |
| issued_at | DATE | - | 발급일 |
| expires_at | DATE | - | 만료일 |
| renewal_cycle | INTEGER | - | 갱신 주기 (개월 단위) |
| created_at | TIMESTAMP | NOT NULL | 등록일 |

### 5. FTO (특허 분석 정보)

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | FTO 고유 ID |
| product_id | UUID | FK → products.id | 연결 제품 |
| target_country | VARCHAR(100) | NOT NULL | 분석 대상 국가 |
| risk_level | ENUM('high', 'medium', 'low') | - | 위험 등급 |
| summary | TEXT | - | 분석 요약 |
| expires_at | DATE | - | 유효 기간 |
| created_at | TIMESTAMP | NOT NULL | 등록일 |

### 6. Buyer Requirements (바이어별 요구사항)

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 요구사항 고유 ID |
| product_id | UUID | FK → products.id | 연결 제품 |
| buyer_name | VARCHAR(200) | NOT NULL | 바이어명 |
| country | VARCHAR(100) | - | 바이어 국가 |
| requirements | TEXT | NOT NULL | 요구 조건 내용 |
| created_at | TIMESTAMP | NOT NULL | 등록일 |

### 7. Audit Logs (제품 지식 수정 이력)

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 로그 고유 ID |
| product_id | UUID | FK → products.id | 대상 제품 |
| changed_by | UUID | FK → users.id | 수정한 사용자 |
| change_type | ENUM('create', 'update', 'delete') | NOT NULL | 변경 유형 |
| changed_fields | JSON | - | 변경된 필드 목록 |
| changed_at | TIMESTAMP | NOT NULL | 변경 시각 |

### 8. Knowledge Sources (지식 출처 원문)

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 출처 고유 ID |
| knowledge_id | UUID | FK → product_knowledge.id | 연결 지식 항목 |
| url | TEXT | - | 출처 URL |
| document_name | VARCHAR(200) | - | 문서명 |
| retrieved_at | DATE | - | 수집일 |

### 9. Diagnosis Runs (AI 진단 실행 기록)

| 컬럼명 | 타입 | 제약조건 | 설명 |
| :--- | :--- | :--- | :--- |
| id | UUID | PK | 진단 고유 ID |
| company_id | UUID | FK → companies.id | 진단 요청 기업 |
| product_id | UUID | FK → products.id | 진단 대상 제품 |
| knowledge_snapshot | JSON | NOT NULL | 진단 시점 제품 지식 스냅샷 |
| result | JSON | - | AI 진단 결과 |
| status | ENUM('pending', 'done', 'failed') | NOT NULL | 진단 상태 |
| created_at | TIMESTAMP | NOT NULL | 진단 요청일 |