# API List

> 상세 요청/응답 형식은 `api-contract.md` 참조

---

### 1. 인증

| # | 메서드 | 엔드포인트 | 설명 | 인증 |
| :--- | :--- | :--- | :--- | :--- |
| 1 | POST | /api/auth/login | 로그인 | 불필요 |
| 2 | POST | /api/auth/logout | 로그아웃 | 필요 |
| 3 | POST | /api/auth/signup | 회원가입 (`type`: personal/company로 구분) | 불필요 |

---

### 2. 사용자 및 회사

| # | 메서드 | 엔드포인트 | 설명 | 인증 |
| :--- | :--- | :--- | :--- | :--- |
| 5 | GET | /api/users/me | 내 계정 정보 조회 | 필요 |
| 6 | PATCH | /api/users/me | 내 계정 정보 수정 | 필요 |
| 7 | GET | /api/companies/me | 내 회사 정보 조회 | 필요 (기업 계정) |
| 8 | PATCH | /api/companies/me | 내 회사 정보 수정 | 필요 (기업 계정) |

---

### 3. AI 진단

| # | 메서드 | 엔드포인트 | 설명 | 인증 |
| :--- | :--- | :--- | :--- | :--- |
| 9 | GET | /api/diagnosis | 내 AI 진단 이력 목록 조회 | 필요 |
| 10 | POST | /api/diagnosis | AI 진단 신규 요청 (productId 또는 inputData 중 하나 필수) | 필요 |
| 11 | GET | /api/diagnosis/:id | 특정 진단 결과 조회 (knowledgeSnapshot 포함) | 필요 |
| 12 | GET | /api/diagnosis/:id/sources | 진단 결과 근거 / 출처 조회 | 필요 |

---

### 3-1. 진단 세부 항목

> ⚠️ **미확정**: 아래 엔드포인트는 현재 contract에 포함되지 않음.
> [11]번 `GET /api/diagnosis/:id` 응답의 `result` 필드 안에 통합할지,
> 별도 엔드포인트로 분리할지 FE/BE 협의 후 확정 예정.

| # | 메서드 | 엔드포인트 | 설명 |
| :--- | :--- | :--- | :--- |
| 13 | GET | /api/diagnosis/:id/readiness | 수출 준비도 종합 점수 |
| 14 | GET | /api/diagnosis/:id/regulations | 규제·인증 분석 결과 |
| 15 | GET | /api/diagnosis/:id/market | 시장 분석 결과 |
| 16 | GET | /api/diagnosis/:id/recall | 리콜 위험 분석 결과 |
| 17 | GET | /api/diagnosis/:id/fto | 특허(FTO) 위험 분석 결과 |
| 18 | GET | /api/diagnosis/:id/support | 매칭 지원사업 목록 |
| 19 | GET | /api/diagnosis/:id/roadmap | 실행 로드맵 (30/60/90일) |

---

### 4. 마이페이지

> ⚠️ **미확정**: [20], [21]번 `/api/mypage` 기본 정보 조회/수정은 현재 contract에 미포함.
> `/api/users/me`([5], [6])와 `/api/companies/me`([7], [8])로 대체 가능한지 검토 필요.

| # | 메서드 | 엔드포인트 | 설명 | 인증 |
| :--- | :--- | :--- | :--- | :--- |
| 20 | GET | /api/mypage | 마이페이지 기본 정보 | 필요 |
| 21 | PATCH | /api/mypage | 마이페이지 수정 | 필요 |

---

### 4-1. 제품 지식 관리

| # | 메서드 | 엔드포인트 | 설명 | 인증 |
| :--- | :--- | :--- | :--- | :--- |
| 22 | GET | /api/products | 등록 제품 목록 조회 (`/diagnosis/new` 불러오기 모달에서도 공통 사용) | 필요 |
| 23 | POST | /api/products | 신규 제품 등록 | 필요 |
| 24 | GET | /api/products/:productId/knowledge | 제품 상세 조회 | 필요 |
| 25 | PUT | /api/products/:productId/knowledge | 제품 정보 수정 (저장 시 Audit Log 자동 기록) | 필요 |
| 26 | DELETE | /api/products/:productId | 제품 삭제 | 필요 |
| 27 | GET | /api/products/:productId/audit-logs | 제품 수정 이력 조회 | 필요 |

---

### 5. 헬스체크

| # | 메서드 | 엔드포인트 | 설명 | 인증 |
| :--- | :--- | :--- | :--- | :--- |
| 28 | GET | /health | 서버 상태 확인 (배포·연결 체크용) | 불필요 |
