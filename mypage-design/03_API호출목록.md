# 마이페이지 API 호출 목록

> 어떤 화면(카드)에서 어떤 API를 부르는지 매핑
> 기준: backend/BE-01/api-list.md (기존 명세) + 마이페이지에 필요한 추가 제안
> 작성: 혜원 / 2026-06-21

---

## 1. 마이페이지 진입 시 호출 흐름

마이페이지(`/mypage`)에 들어오면 카드별로 아래 API를 **병렬 호출**한다.

| 순서 | 카드 | 메서드 | 엔드포인트 | 존재 여부 |
| :-- | :--- | :--- | :--- | :--- |
| 1 | 회사 정보 카드 | GET | `/api/companies/me` | ✅ 있음 |
| 2 | (계정 기본 정보) | GET | `/api/users/me` | ✅ 있음 |
| 3 | 제품 지식 현황 카드 | GET | `/api/products` | ✅ 있음 (=`/api/mypage/knowledge`) |
| 4 | 최근 AI 진단 카드 | GET | `/api/diagnosis` | ✅ 있음 (최신 1건 사용) |
| 5 | 리포트 보관함 카드 | GET | `/api/reports` | ⭐ 신규 필요 (목록) |
| 6 | 팀원 권한 카드 | GET | `/api/companies/me/members` | ⭐ 신규 필요 |

> 성능: 카드별 API를 `Promise.all`로 동시 호출해 초기 로딩을 단축한다.

---

## 2. 카드별 상세 호출

### 회사 정보 카드
```
GET /api/companies/me        # 기업명, 업종, 사업자번호
GET /api/users/me            # 담당자명, 이메일, user_type
PATCH /api/companies/me      # 수정 모드에서 회사 정보 변경
PATCH /api/users/me          # 수정 모드에서 계정 정보 변경
```

### 제품 지식 현황 카드
```
GET /api/products                              # 제품 목록 (신뢰도/만료 임박 집계용)
GET /api/products/{product_id}/knowledge       # 카드 클릭 → 제품 상세 진입
GET /api/products/{product_id}/audit-logs      # 제품 상세의 수정 이력 탭
```

### 최근 AI 진단 결과 카드
```
GET /api/diagnosis                 # 진단 이력 목록 → 최신 1건 추출
GET /api/diagnosis/{run_id}        # 카드 클릭 → 진단 결과 상세 (result, knowledgeSnapshot 포함)
```

### 오늘 할 일 카드
```
GET /api/diagnosis/{run_id}        # result.roadmap (30/60/90일)에서 임박 액션 추출
```
> 별도 API 없이 최근 진단 결과의 `result` 안 로드맵을 프론트에서 가공.

### 리포트 보관함 카드 ⭐
```
GET /api/reports                   # 내 리포트 목록 (신규 제안)
GET /api/report/{run_id}           # 개별 리포트 조회/다운로드 (과제 요구, 현재 미존재)
```

### 팀원 권한 카드 ⭐
```
GET /api/companies/me/members      # 멤버 목록 + 역할 (신규 제안)
```

### AI 진단 다시 실행 버튼
```
POST /api/diagnosis                # 재진단 요청 → 4번 문서(AI Agent 호출 흐름) 참조
```

---

## 3. ⭐ 추가가 필요한 API (백엔드 협의 항목)

| 메서드 | 엔드포인트 | 용도 | 비고 |
| :--- | :--- | :--- | :--- |
| GET | `/api/reports` | 리포트 보관함 목록 | reports 테이블 신설 전제 |
| GET | `/api/report/{run_id}` | 개별 리포트 조회 | **과제 요구사항인데 현재 명세에 없음** |
| GET | `/api/companies/me/members` | 팀원/권한 목록 | membership 테이블 기반 |

> 위 3개는 마이페이지를 완성하려면 백엔드에 요청해야 할 항목.
> 나머지(회사·제품·진단)는 **이미 구현되어 있어** 프론트에서 호출 코드만 작성하면 된다.

---

## 4. 엔드포인트 표기 주의 (명세 정합성 이슈)

작업 중 발견한 불일치 — 백엔드와 확정 필요:

| 항목 | 과제 PDF | 레포 api-list | 백엔드 실제 코드 |
| :--- | :--- | :--- | :--- |
| 제품 목록 | `/api/products` | `/api/mypage/knowledge` | `/api/products` |
| 제품 지식 | `/api/products/{id}/knowledge` | `/api/mypage/knowledge/:id` | `/api/products/{id}/knowledge` |
| 수정 메서드 | - | PATCH | PUT |

→ **경로(`/api/products` vs `/api/mypage/knowledge`)와 수정 메서드(PUT vs PATCH)가 문서마다 다름.**
실제 연동 전 FE/BE가 하나로 통일해야 함. (현재 백엔드 컨트롤러 기준 = `/api/products`, `PUT`)
