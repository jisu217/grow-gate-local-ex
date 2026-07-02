# FE-02 화면별 API 호출 흐름표 (F-1)

> 담당: 한혜원 (FE) · 대상: `frontend/FE-02`
> 목적: 어느 화면에서 어떤 API를 부르는지, 어디가 실제 API이고 어디가 mock fallback인지 한눈에 파악.
> 기준 커밋: `main` 최신 (수출 Gate 판단 반영본)

---

## 0. 공통 사항

- **API 베이스**: `apiClient.js` — `VITE_API_URL`(없으면 현재 origin) + `/api`. 로컬은 `.env`의 `VITE_API_URL=http://localhost:8080`.
- **인증**: 로그인 시 `accessToken`을 **메모리**에 보관(`memoryAccessToken`) + 서버가 **HttpOnly 쿠키** 발급. `apiFetch`가 메모리 토큰을 `Authorization: Bearer`로 자동 첨부. localStorage에는 사용자 정보(`gg_auth_user`)만 저장.
  - ⚠️ 새로고침하면 메모리 토큰이 사라짐 → `ProtectedRoute`가 `getAccessToken()`(메모리)만 확인하므로 로그인으로 튕길 수 있음(별도 이슈).
- **공통 fetch 특성**: 타임아웃 15s, GET 자동 재시도 2회(`apiClient`).
- **mock 경계 스위치**: `VITE_ENABLE_MOCK === 'true'` 일 때만 조회 실패 시 mock으로 대체. **기본값 false → 실패는 그대로 에러로 전파(운영 안전).**

---

## 1. 화면별 API 호출표

| 경로(route) | 화면 | 호출 함수 | 실제 endpoint | method | loading/error/empty |
| :-- | :-- | :-- | :-- | :-- | :-- |
| `/` | 홈 | — (네비게이션만) | — | — | — |
| `/login` | 로그인 | `authService.login` | `/auth/login` | POST | 인라인 에러(`error` state) |
| `/signup` `/signup/personal` `/signup/company` | 회원가입 | `authService.signup` | `/auth/signup` | POST | 인라인 에러 |
| `/diagnosis` | Gate 판단 이력 | `getDiagnosisList` | `/diagnosis` | GET | ✅ loading·error·empty·ok (`StatusMessage`) |
| `/diagnosis/new` | Gate 판단 입력 | `getMypageProducts`(불러오기)<br>`createMypageProduct`(저장 시)<br>`runDiagnosis` | `/products`<br>`/products`<br>`/diagnosis` | GET<br>POST<br>POST | 폼 필드 에러(`errors`) + `submitError` + `submitting`(중복클릭 방지). **StatusMessage 미사용** |
| `/diagnosis/:id` | 판정 결과 뷰어 | `getDiagnosisResult`(+`getDiagnosisSources`)<br>`getDiagnosisReport` | `/diagnosis/{id}` (+ `/diagnosis/{id}/sources`)<br>`/diagnosis/{id}/report` | GET | ✅ loading·error·**failed**·ok + **진행중 폴링 자동 갱신** |
| `/diagnosis/:id/sources` | 근거·출처 | `getDiagnosisSourceEntries`<br>`getPublicDataDashboard` | `/diagnosis/{id}/sources`<br>`/public-data/dashboard` | GET | (StatusMessage 미사용) |
| `/mypage` | 마이페이지 | `getMypageOverview`<br>`getMypageProducts`<br>`getDiagnosisList`+`getDiagnosisResult`(선택 제품 최신 진단)<br>`createMypageProduct`·`deleteMypageProduct`·`rerunDiagnosis` | `/users/me` (+`/companies/me`,`/companies/me/members`)<br>`/products`(+`/diagnosis`)<br>`/diagnosis`,`/diagnosis/{id}`<br>`/products`,`/products/{id}`(DELETE),`/diagnosis` | GET/POST/DELETE | `loadError`·`saveError` **인라인**(Promise.allSettled로 부분 실패 표시). StatusMessage 미사용 |
| `/mypage/diagnosis/:productId` | 점수 근거 상세 | `getDiagnosisList`+`getDiagnosisResult` | `/diagnosis`, `/diagnosis/{id}` | GET | ✅ loading·empty·error·ok |
| `/mypage/knowledge` | 제품 지식 관리 | `getProducts` | `/products` | GET | ✅ loading·error·empty·ok |
| `/mypage/knowledge/:productId` | 제품 지식 상세 | `getProductKnowledge`·`getProductAuditLogs`·knowledge-loop 함수들 | `/products/{id}/knowledge`, `/products/{id}/audit-logs`, `/products/{id}/knowledge-loop-*` | GET/POST | ✅ loading·error |
| `/mypage/knowledge/new` | 신규 제품 등록 | `createProduct` | `/products` | POST | 폼 에러 |
| `/mypage/knowledge/:productId/edit` | 제품 정보 수정 | `getProductKnowledge`·`saveProductKnowledge` | `/products/{id}/knowledge` | GET/PUT | ✅ loading·error·**권한없음** |

> 인증서/FTO/바이어 요구사항 CRUD(`/products/{id}/certifications|fto|buyer-requirements`)는 제품 지식 상세의 탭에서 호출됨.

---

## 2. mock fallback 점검표

`VITE_ENABLE_MOCK=true`일 때만 조회 실패 시 mock 대체. 쓰기(생성/수정/삭제)는 **절대 mock 대체 안 함** = 실패가 사용자에게 보임.

| 함수 | 종류 | mock fallback | fallback 내용 |
| :-- | :-- | :-- | :-- |
| `diagnosisService.getDiagnosisList` | 조회 | ✅ (ENABLE_MOCK) | `DIAGNOSIS_LIST` |
| `diagnosisService.getDiagnosisResult` | 조회 | ✅ (ENABLE_MOCK) | mock 세션/스테이지 |
| `diagnosisService.getDiagnosisSources` | 조회 | ✅ (ENABLE_MOCK) | 상세 응답의 `evidenceSources`로 대체 |
| `diagnosisService.getDiagnosisSourceEntries` | 조회 | ❌ | 에러 전파 |
| `diagnosisService.getDiagnosisReport` | 조회 | ❌ | 에러 전파 |
| `diagnosisService.runDiagnosis` | 쓰기 | ❌ | 에러 전파 |
| `productService.getProducts` | 조회 | ✅ (ENABLE_MOCK) | `PRODUCTS` |
| `productService.getProductKnowledge` | 조회 | ✅ (ENABLE_MOCK) | `PRODUCT_DETAILS` |
| `productService.getProductAuditLogs` | 조회 | ✅ (ENABLE_MOCK) | 빈 배열(권한 없으면 정상적으로 빈 배열) |
| `productService.saveProductKnowledge` | 쓰기 | ❌ (주석에 사유 명시) | 에러 전파 — mock 성공 둔갑 시 입력 유실 위험 |
| `productService.createProduct` / `deleteProduct` / cert·fto·buyer CRUD | 쓰기 | ❌ | 에러 전파 |
| `mypageService.getMypageOverview` | 조회 | ❌ | 에러 전파 |
| `mypageService.getMypageProducts` | 조회 | ⚠️ 부분 | 최신 진단 조회 실패 시 **제품 기본 정보만** 표시(console.warn) — 제품 목록 자체 실패는 전파 |
| `mypageService.createMypageProduct`/`updateMypageProduct`/`deleteMypageProduct`/`rerunDiagnosis` | 쓰기 | ❌ | 에러 전파 |
| `authService.*` (login/signup/logout/refresh) | 인증 | ❌ | 에러 전파 |
| `publicDataService.getPublicDataDashboard` | 조회 | ❌ | 에러 전파 |

---

## 3. 실제 API vs mock 경계 (요약)

- **기본 상태(`VITE_ENABLE_MOCK=false`)**: 모든 API가 실제 백엔드(8080) 호출. 실패는 화면 에러로 노출 → 운영 안전.
- **mock 대체가 켜지는 조건**: `.env`에 `VITE_ENABLE_MOCK=true` 설정 시에만, 그리고 **조회(GET) 함수 일부만** mock으로 대체.
- **쓰기 작업은 항상 실제 API**: 진단 실행/제품 생성·삭제·지식 저장은 mock으로 조용히 성공하지 않음.

---

## 4. 재현 먼저 — 조사 중 발견한 이슈

1. **[버그] `/diagnosis/new`에서 "마이페이지에도 저장" 체크 시 판단 생성 실패**
   - 화면: Gate 판단 입력 / 계정: 로그인 후 / 버튼: "수출 Gate 판단 시작"
   - 기대: 제품 저장 + 진단 생성 → 결과 화면 이동
   - 실제: 제품은 저장되지만 진단 생성이 "제품명은 필수입니다, HS Code는 필수입니다"로 실패
   - 원인: `runDiagnosis`가 `productId`가 있으면 `inputData`에서 `productName`·`hsCode`를 뺌. 백엔드 `CreateDiagnosisRequest.InputData`는 두 값을 항상 `@NotBlank`로 요구. → **F-2에서 수정 예정** (productId 유무와 무관하게 항상 포함).

2. **[UX] 새로고침 시 로그인 풀림** — `ProtectedRoute`가 메모리 토큰만 확인. 쿠키 기반 복구 동선 없음. (별도 이슈로 기록)

3. **[UX] `/diagnosis/new`·`/mypage`는 `StatusMessage`를 안 쓰고 인라인 에러** — 다른 화면과 로딩/에러 표현이 일관되지 않음. (F-4 대상)
