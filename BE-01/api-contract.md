### 1. 인증

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

### [3] POST /api/auth/signup/personal 개인 회원가입

**Request**
```json
{
  "email": "user@example.com",
  "password": "password1234",
  "name": "홍길동"
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

---

### [4] POST /api/auth/signup/company 기업 회원가입

**Request**
```json
{
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
  "userId": "uuid-5678",
  "email": "corp@example.com",
  "name": "담당자명",
  "userType": "company",
  "companyId": "uuid-company-001"
}
```

---

### 2. 사용자 및 회사

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

### 3. AI 진단

### [9] GET /api/diagnosis 내 AI 진단 이력 목록 조회

**Response** `200 OK`
```json
{
  "diagnoses": [
    {
      "id": "uuid-diag-001",
      "productId": "uuid-prod-001",
      "productName": "스마트 온도계",
      "status": "done",
      "createdAt": "2026-06-05T10:00:00Z"
    }
  ]
}
```

---

### [10] POST /api/diagnosis AI 진단 신규 요청

**Request**
```json
{
  "productId": "uuid-prod-001",
  "targetCountry": "미국"
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

---

### [11] GET /api/diagnosis/:id 특정 진단 결과 조회

**Response** `200 OK`
```json
{
  "id": "uuid-diag-001",
  "productId": "uuid-prod-001",
  "status": "done",
  "result": {
    "readinessScore": 72,
    "summary": "전반적인 수출 준비도는 양호하나 FTO 리스크 보완 필요"
  },
  "createdAt": "2026-06-05T10:00:00Z"
}
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
    }
  ]
}
```

---

### 4. 마이페이지 — 제품 지식 관리

### [22] GET /api/mypage/knowledge 등록 제품 목록 조회

**Response** `200 OK`
```json
{
  "products": [
    {
      "productId": "uuid-prod-001",
      "name": "스마트 온도계",
      "category": "전자기기",
      "trustGrade": "high",
      "updatedAt": "2026-06-06T08:00:00Z"
    }
  ]
}
```

---

### [23] POST /api/mypage/knowledge 신규 제품 등록

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

### [24] GET /api/mypage/knowledge/:productId 제품 상세 조회

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
      "expiresAt": "2027-03-01",
      "renewalCycle": 24
    }
  ],
  "fto": [
    {
      "id": "uuid-fto-001",
      "targetCountry": "미국",
      "riskLevel": "low",
      "summary": "유효 특허 충돌 없음"
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

### [25] PATCH /api/mypage/knowledge/:productId 제품 정보 수정

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

### [26] DELETE /api/mypage/knowledge/:productId 제품 삭제

**Response** `200 OK`
```json
{
  "message": "제품이 삭제되었습니다.",
  "productId": "uuid-prod-001"
}
```

---

### [27] GET /api/mypage/knowledge/:productId/logs 제품 수정 이력 조회

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

---
