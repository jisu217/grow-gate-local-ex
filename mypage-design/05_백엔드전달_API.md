# 마이페이지 API 요청서 (백엔드 전달용)

> 마이페이지(FE-02) 화면이 호출하는 API 목록입니다.
> 현재 백엔드 컨트롤러 기준으로 **이미 있는 것 / 신규·수정 필요한 것**을 구분했습니다.
> 작성: 혜원 / 2026-06-21

---

## 1. 한눈에 보기

| 마이페이지 기능 | 메서드 | 엔드포인트 | 상태 |
| :--- | :--- | :--- | :--- |
| 회사 정보 카드 | GET | `/api/companies/me` | ✅ 있음 |
| 계정 정보 | GET | `/api/users/me` | ✅ 있음 |
| 제품 목록/지식현황 카드 | GET | `/api/products` | ✅ 있음 |
| 최근 AI 진단 카드 | GET | `/api/diagnosis` | ✅ 있음 |
| 제품 추가 | POST | `/api/products` | ✅ 있음 |
| AI 진단 재실행 | POST | `/api/diagnosis` | ✅ 있음 |
| **제품 기본정보 수정** | PATCH | `/api/products/{id}` | ❌ **신규 필요** |
| **팀원 권한 카드** | GET | `/api/companies/me/members` | ❌ **신규 필요** |
| **리포트 보관함 카드** | GET | `/api/reports` | ❌ **신규 필요** |

> ✅ 6개는 그대로 사용. ❌ 3개만 추가해주시면 마이페이지 연동이 끝납니다.

---

## 2. 신규/수정 요청 API (백엔드 작업 부탁드립니다)

### 2-1. ❌ 제품 기본정보 수정 — `PATCH /api/products/{id}`
마이페이지 "제품 수정" 모달에서 제품명·HS Code·대상국을 수정합니다.
현재 제품 지식 수정(`PUT /api/products/{id}/knowledge`)은 있지만, **제품 기본정보 수정 엔드포인트가 없습니다.**

**요청 (Request body)**
```json
{
  "name": "리튬배터리 팩",
  "hsCode": "850760",
  "targetCountry": "독일"
}
```
**응답 (Response)**
```json
{
  "id": "prod_001",
  "name": "리튬배터리 팩",
  "hsCode": "850760",
  "targetCountry": "독일",
  "updatedAt": "2026-06-21"
}
```

---

### 2-2. ❌ 팀원 권한 목록 — `GET /api/companies/me/members`
마이페이지 "팀원 권한 / 역할" 카드에 사용합니다. (기업 계정 전용)

**응답 (Response)**
```json
{
  "members": [
    { "userId": "u1", "name": "홍길동", "role": "owner" },
    { "userId": "u2", "name": "김담당", "role": "editor" }
  ]
}
```
> `role`은 `owner | admin | editor | viewer` (memberships 테이블의 role)

---

### 2-3. ❌ 리포트 보관함 — `GET /api/reports`
마이페이지 "리포트 보관함" 카드에 사용합니다. 진단 결과를 문서로 보관·재열람합니다.
> ⚠️ 현재 ERD에 `reports` 테이블이 없어, 테이블 신설이 함께 필요합니다. (제안: `02_DB구조초안.md` 참조)

**응답 (Response)**
```json
{
  "reports": [
    {
      "id": "rep_001",
      "diagnosisRunId": "diag_001",
      "title": "리튬배터리 팩 · 독일 진단 리포트",
      "fileUrl": "https://.../report_001.pdf",
      "createdAt": "2026-06-19"
    }
  ]
}
```
> 제품별 필터가 필요하면 `GET /api/reports?productId={id}` 형태로 쿼리 지원 부탁드립니다.

---

## 3. 이미 있는 API — 프론트가 기대하는 응답 형태

> 구현은 돼 있으나, 마이페이지 카드가 아래 필드를 사용하니 응답에 포함되는지 확인 부탁드립니다.

### `GET /api/companies/me` (회사 정보 카드)
```json
{ "id": "comp_001", "name": "경남 배터리테크", "industry": "전기전자 · 배터리", "businessNumber": "123-45-67890" }
```

### `GET /api/products` (제품 목록 / 지식현황 카드)
마이페이지 카드는 제품별로 아래 요약값이 필요합니다:
```json
{
  "products": [
    {
      "id": "prod_001",
      "name": "리튬배터리 팩",
      "hsCode": "850760",
      "targetCountry": "독일",
      "knowledgeProgress": 72,         // 제품 지식 입력 완성도(%) — 등록현황 카드
      "trustGrade": "high",            // 신뢰도 등급
      "expiringCertCount": 1,          // 만료 임박 인증서 수
      "latestRunId": "diag_001",       // 최근 진단 id (카드 클릭 시 이동)
      "latestScore": 83                // 최근 진단 점수
    }
  ]
}
```
> `knowledgeProgress`, `expiringCertCount`, `latestScore`는 서버 계산값입니다. 제공 가능 여부 확인 부탁드립니다.

### `GET /api/diagnosis` (최근 AI 진단 카드)
```json
{
  "diagnoses": [
    {
      "id": "diag_001",
      "productName": "리튬배터리 팩",
      "targetCountry": "독일",
      "status": "done",                // pending | running | done | failed
      "score": 83,
      "createdAt": "2026-06-01"
    }
  ]
}
```
> 마이페이지는 이 목록에서 **최신 1건**만 카드에 표시합니다.

### `POST /api/diagnosis` (AI 진단 재실행)
```json
// 저장 제품 기반
{ "productId": "prod_001", "targetCountry": "독일" }
// 또는 직접 입력
{ "targetCountry": "독일", "inputData": { "productName": "리튬배터리 팩", "hsCode": "850760" } }
```

---

## 4. 마이페이지 진입 시 호출 순서

```
[마이페이지 첫 진입]
 ├─ GET /api/users/me               (계정)
 ├─ GET /api/companies/me           (회사 정보 카드)
 ├─ GET /api/companies/me/members   (팀원 카드) ❌신규
 ├─ GET /api/products               (제품/지식현황 카드)
 ├─ GET /api/diagnosis              (최근 진단 카드, 최신 1건)
 └─ GET /api/reports                (리포트 카드) ❌신규
        ↓ (사용자 액션)
 ├─ POST  /api/products             (제품 추가)
 ├─ PATCH /api/products/{id}        (제품 수정) ❌신규
 └─ POST  /api/diagnosis            (진단 재실행)
```

---

## 5. 확인 부탁드릴 점 (FE/BE 합의 항목)
1. **응답 필드명 케이스**: FE는 camelCase(`hsCode`, `targetCountry`) 기준입니다. 백엔드 응답도 camelCase로 통일 가능한지요.
2. **제품 수정 메서드**: 제품 기본정보는 `PATCH /api/products/{id}` 로 추가 부탁드립니다. (지식 수정은 기존 `PUT .../knowledge` 유지)
3. **reports 테이블 신설** 가능 여부 (리포트 보관함 카드 전제).
4. `GET /api/products` 응답에 마이페이지용 요약 필드(`knowledgeProgress` 등) 포함 가능 여부.
