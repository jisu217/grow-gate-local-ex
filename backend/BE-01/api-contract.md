# API contract

> **공통 규칙**
> 1. 모든 요청/응답 형식: `application/json`  
> 2. 인증이 필요한 API는 요청 헤더에 `Authorization: Bearer {accessToken}` 포함  
> 3. 에러 응답 형식은 아래 공통 형식을 따름
> 4. 필드 네이밍: camelCase 고정

**공통 에러 응답 형식**
```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "로그인이 필요합니다."
  }
}
```

**주요 에러 코드**

| HTTP 상태 | code | 설명 |
| :--- | :--- | :--- |
| 400 | `INVALID_REQUEST` | 요청 값 형식 오류 또는 필수 필드 누락 |
| 401 | `UNAUTHORIZED` | 인증 토큰 없음 또는 만료 |
| 403 | `FORBIDDEN` | 권한 없음 (본인 리소스가 아닌 경우) |
| 404 | `NOT_FOUND` | 해당 리소스 없음 |
| 409 | `CONFLICT` | 이미 존재하는 리소스 |
| 500 | `SERVER_ERROR` | 서버 내부 오류 |

---

## 1. 인증

### [1] POST /api/auth/login 로그인

**Request**
```json
{
  "email": "user@example.com",
  "password": "password1234"
}
```

**Response** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "uuid-1234",
  "userType": "personal"
}
```

**에러**
```json
// 401 - 이메일 또는 비밀번호 불일치
{ "error": { "code": "UNAUTHORIZED", "message": "이메일 또는 비밀번호가 올바르지 않습니다." } }
```

---

### [2] POST /api/auth/logout 로그아웃

**Request** _(Body 없음, Authorization 헤더 필요)_

**Response** `200 OK`
```json
{
  "message": "로그아웃 되었습니다."
}
```

---

### [3] POST /api/auth/signup 회원가입

> `type` 값에 따라 개인/기업 가입 구분. 기업 가입 시 `companyName`, `businessNumber`, `industry` 필수

**Request — 개인**
```json
{
  "type": "personal",
  "email": "user@example.com",
  "password": "password1234",
  "name": "홍길동"
}
```

**Request — 기업**
```json
{
  "type": "company",
  "email": "corp@example.com",
  "password": "password1234",
  "name": "담당자명",
  "companyName": "㈜예시기업",
  "businessNumber": "123-45-67890",
  "industry": "전자부품"
}
```

**Response** `201 Created`
```json
{
  "userId": "uuid-1234",
  "email": "user@example.com",
  "name": "홍길동",
  "userType": "personal"
}
```

**에러**
```json
// 409 - 이메일 중복
{ "error": { "code": "CONFLICT", "message": "이미 사용 중인 이메일입니다." } }

// 409 - 사업자번호 중복
{ "error": { "code": "CONFLICT", "message": "이미 등록된 사업자번호입니다." } }
```

---

## 2. 사용자 및 회사

### [5] GET /api/users/me 내 계정 정보 조회

**Response** `200 OK`
```json
{
  "userId": "uuid-1234",
  "email": "user@example.com",
  "name": "홍길동",
  "userType": "personal",
  "createdAt": "2026-06-01T09:00:00Z"
}
```

---

### [6] PATCH /api/users/me 내 계정 정보 수정

**Request**
```json
{
  "name": "홍길순"
}
```

**Response** `200 OK`
```json
{
  "userId": "uuid-1234",
  "name": "홍길순",
  "updatedAt": "2026-06-07T12:00:00Z"
}
```

---

### [7] GET /api/companies/me 내 회사 정보 조회

**Response** `200 OK`
```json
{
  "companyId": "uuid-company-001",
  "name": "㈜예시기업",
  "businessNumber": "123-45-67890",
  "industry": "전자부품",
  "createdAt": "2026-06-01T09:00:00Z"
}
```

---

### [8] PATCH /api/companies/me 내 회사 정보 수정

**Request**
```json
{
  "industry": "반도체"
}
```

**Response** `200 OK`
```json
{
  "companyId": "uuid-company-001",
  "industry": "반도체",
  "updatedAt": "2026-06-07T12:00:00Z"
}
```

---

## 3. AI 진단

### [9] GET /api/diagnosis 내 AI 진단 이력 목록 조회

**Response** `200 OK`
```json
{
  "diagnoses": [
    {
      "id": "uuid-diag-001",
      "productId": "uuid-prod-001",
      "productName": "스마트 온도계",
      "targetCountry": "미국",
      "status": "done",
      "createdAt": "2026-06-05T10:00:00Z"
    }
  ]
}
```

---

### [10] POST /api/diagnosis AI 진단 신규 요청

> **knowledge snapshot 처리 방식**
> - productId를 함께 보내면 백엔드가 해당 제품의 현재 지식(certifications, fto, buyerRequirements, knowledge)을 자동으로 snapshot으로 저장함
> - 직접 입력(productId 없이) 진단 시에는 inputData로 제품명과 HS Code만 보냄

**Request — 저장된 제품 지식 불러와서 진단하는 경우**
```json
{
  "productId": "uuid-prod-001",
  "targetCountry": "미국"
}
```

**Request — 직접 입력으로 진단하는 경우** _(productId 없이)_
```json
{
  "targetCountry": "미국",
  "inputData": {
    "productName": "스마트 온도계",
    "hsCode": "9025.19"
  }
}
```

**Response** `202 Accepted`
```json
{
  "diagnosisId": "uuid-diag-002",
  "status": "pending",
  "message": "진단이 요청되었습니다. 잠시 후 결과를 확인해주세요."
}
```

**에러**
```json
// 404 - productId에 해당하는 제품 없음
{ "error": { "code": "NOT_FOUND", "message": "등록된 제품을 찾을 수 없습니다." } }

// 400 - productId도 없고 inputData도 없는 경우
{ "error": { "code": "INVALID_REQUEST", "message": "productId 또는 inputData 중 하나는 필수입니다." } }
```

---

### [11] GET /api/diagnosis/:id 특정 진단 결과 조회

> **우측 인스펙터 패널용 데이터**: `knowledgeSnapshot` 필드가 이 진단에 사용된 제품 지식 기준값.
> FE는 이 필드를 기반으로 신뢰도 등급, 만료 임박 인증서, 누락 필드를 인스펙터 패널에 표시함.

**Response** `200 OK`
```json
{
  "id": "uuid-diag-001",
  "productId": "uuid-prod-001",
  "companyId": "uuid-company-001",
  "knowledgeSnapshotId": "uuid-snapshot-001",
  "productName": "스마트 온도계",
  "targetCountry": "미국",
  "status": "done",
  "result": {
    "diagnosisScore": 72,
    "summary": "전반적인 수출 준비도는 양호하나 FTO 리스크 보완 필요",
    "riskItems": [
      { "level": "high", "label": "FTO 리스크 미해결" }
    ],
    "actionRoadmap": { "d30": 2, "d60": 1, "d90": 3 },
    "evidenceSources": ["uuid-source-001", "uuid-source-002"]
  },
  "knowledgeSnapshot": {
    "trustGrade": "medium",
    "sourceType": "공식문서",
    "certifications": [
      {
        "name": "KC 인증",
        "expiresAt": "2027-03-01",
        "daysUntilExpiry": 267
      }
    ],
    "missingFields": ["fto", "buyerRequirements"],
    "snapshotAt": "2026-06-05T10:00:00Z"
  },
  "createdAt": "2026-06-05T10:00:00Z"
}
```

> **`result` 필드 설명**
>
> | 필드 | 타입 | 설명 |
> | :--- | :--- | :--- |
> | `diagnosisScore` | number | 진단 점수 (0~100) |
> | `summary` | string | 진단 결과 요약 |
> | `riskItems` | array | 리스크 항목 목록. 각 항목은 `level`(`"high"` \| `"medium"` \| `"low"`), `label`로 구성 |
> | `actionRoadmap` | object | 30·60·90일 실행 항목 수. `d30`, `d60`, `d90` 필드로 구성 |
> | `evidenceSources` | array | 진단 근거 출처 ID 목록. `/api/diagnosis/:id/sources`로 상세 조회 가능 |

> **`knowledgeSnapshot` 필드 설명**
>
> | 필드 | 타입 | 설명 |
> | :--- | :--- | :--- |
> | `trustGrade` | `"high"` \| `"medium"` \| `"low"` | 진단 시점 신뢰도 등급 |
> | `sourceType` | string | 출처 유형 (공식문서 / 자체조사 등) |
> | `certifications` | array | 진단 시점 인증서 목록. `daysUntilExpiry`가 90 이하면 만료 임박으로 표시 |
> | `missingFields` | array | 진단 시점에 비어있던 필드 목록. FE가 누락 필드 경고 배지 표시에 사용 |
> | `snapshotAt` | ISO 8601 | snapshot 생성 시각 |

**에러**
```json
// 404
{ "error": { "code": "NOT_FOUND", "message": "진단 결과를 찾을 수 없습니다." } }

// 403 - 본인 진단이 아닌 경우
{ "error": { "code": "FORBIDDEN", "message": "접근 권한이 없습니다." } }
```

---

### [12] GET /api/diagnosis/:id/sources 진단 결과 근거/출처 조회

**Response** `200 OK`
```json
{
  "diagnosisId": "uuid-diag-001",
  "sources": [
    {
      "field": "certifications",
      "documentName": "KC 인증서",
      "url": "https://example.com/kc.pdf",
      "retrievedAt": "2026-05-20"
    },
    {
      "field": "regulations",
      "documentName": "EU CE 규제 가이드라인",
      "url": "https://ec.europa.eu/...",
      "retrievedAt": "2026-05-21"
    }
  ]
}
```

---

## 4. 마이페이지 — 제품 지식 관리

### [22] GET /api/products 등록 제품 목록 조회

> **사용 화면**: `/mypage/knowledge` 제품 목록 카드, `/diagnosis/new` "저장된 제품 지식 불러오기" 모달 양쪽에서 이 API를 공통으로 사용함.
> (모달에서는 `productId`, `name`, `trustGrade`, `missingFieldCount`만 표시)

**Response** `200 OK`
```json
{
  "products": [
    {
      "productId": "uuid-prod-001",
      "name": "스마트 온도계",
      "category": "전자기기",
      "trustGrade": "high",
      "expiringCertCount": 1,
      "missingFieldCount": 0,
      "updatedAt": "2026-06-06T08:00:00Z"
    }
  ]
}
```

> **추가 필드 설명**
>
> | 필드 | 타입 | 설명 |
> | :--- | :--- | :--- |
> | `expiringCertCount` | number | 만료 90일 이내 인증서 수. 마이페이지 카드에서 "만료 임박 N건" 배지로 표시 |
> | `missingFieldCount` | number | 누락된 필드 수 (`fto`, `buyerRequirements` 등). 0이면 배지 미표시 |

---

### [23] POST /api/products 신규 제품 등록

**Request**
```json
{
  "name": "스마트 온도계",
  "category": "전자기기",
  "description": "IoT 기반 실시간 온도 모니터링 기기"
}
```

**Response** `201 Created`
```json
{
  "productId": "uuid-prod-001",
  "name": "스마트 온도계",
  "createdAt": "2026-06-07T09:00:00Z"
}
```

---

### [24] GET /api/products/{productId}/knowledge 제품 지식 상세 조회

**Response** `200 OK`
```json
{
  "productId": "uuid-prod-001",
  "name": "스마트 온도계",
  "category": "전자기기",
  "description": "IoT 기반 실시간 온도 모니터링 기기",
  "knowledge": {
    "trustGrade": "high",
    "sourceType": "공식문서",
    "notes": "미국 FDA 등록 완료"
  },
  "certifications": [
    {
      "id": "uuid-cert-001",
      "name": "KC 인증",
      "issuedAt": "2025-03-01",
      "expiresAt": "2027-03-01",
      "renewalCycle": 24,
      "daysUntilExpiry": 267
    }
  ],
  "fto": [
    {
      "id": "uuid-fto-001",
      "targetCountry": "미국",
      "riskLevel": "low",
      "summary": "유효 특허 충돌 없음",
      "expiresAt": "2027-06-01"
    }
  ],
  "buyerRequirements": [
    {
      "id": "uuid-buyer-001",
      "buyerName": "ABC Corp",
      "country": "미국",
      "requirements": "FDA 510(k) 등록 필수"
    }
  ],
  "updatedAt": "2026-06-06T08:00:00Z"
}
```

---

### [25] PUT /api/products/{productId}/knowledge 제품 지식 수정

> 수정 저장 시 백엔드에서 Audit Log(`audit_logs` 테이블)를 자동으로 기록함

**Request**
```json
{
  "description": "업데이트된 설명",
  "knowledge": {
    "trustGrade": "medium",
    "notes": "추가 검토 필요"
  }
}
```

**Response** `200 OK`
```json
{
  "productId": "uuid-prod-001",
  "updatedAt": "2026-06-07T10:00:00Z"
}
```

---

### [26] DELETE /api/products/{productId} 제품 삭제

**Response** `200 OK`
```json
{
  "message": "제품이 삭제되었습니다.",
  "productId": "uuid-prod-001"
}
```

**에러**
```json
// 403 - 본인 제품이 아닌 경우
{ "error": { "code": "FORBIDDEN", "message": "접근 권한이 없습니다." } }
```

---

### [27] GET /api/products/{productId}/audit-logs 제품 수정 이력 조회

**Response** `200 OK`
```json
{
  "productId": "uuid-prod-001",
  "logs": [
    {
      "logId": "uuid-log-001",
      "changedBy": "홍길동",
      "changeType": "update",
      "changedFields": ["trustGrade", "notes"],
      "changedAt": "2026-06-06T08:00:00Z"
    }
  ]
}
```

> **`changeType` 값**
> - `create` : 제품 최초 등록
> - `update` : 필드 수정
> - `delete` : 제품 삭제
