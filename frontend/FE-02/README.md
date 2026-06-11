### GrowGate Frontend

### Getting Started
### 1. 요구 사항

- **Node.js** `v18` 이상 (LTS 권장 — 개발 환경: `v24.x`)
- **npm** `v9` 이상 (Node.js에 포함)

### 2. 설치 및 실행

```bash
# 1. 의존성 설치
npm install

# 2. 개발 서버 실행
npm run dev

# 3. 프로덕션 빌드
npm run build

# 4. 빌드 결과물 미리보기
npm run preview
```

---

### Frontend Project Structure

```text
FE-02/
├── index.html                              ← Vite 진입점 + Google Fonts
├── vite.config.js                          ← React 플러그인 설정
├── package.json
├── package-lock.json                       ← 반드시 커밋 유지 (버전 고정)
├── .env.example                            ← 환경변수 템플릿
└── src/
    ├── main.jsx                            ← ReactDOM.createRoot 마운트
    ├── App.jsx                             ← BrowserRouter + 전체 라우트 정의
    ├── styles/
    │   └── global.css                      ← :root 변수 + 전역 스타일
    │
    ├── mock/                               ← 백엔드 연동 전 임시 데이터 (연동 시 services/로 교체)
    │   ├── auth.js                         ← mockAuth: isLoggedIn, accountType, role
    │   ├── roles.js                        ← PERMISSIONS: 역할별 허용 액션 테이블
    │   ├── diagnosisSteps.js               ← STEP 정의 배열
    │   ├── diagnosisData.js                ← KPI + S00~S10 mock 데이터 (product_id 등 BE 연동 필드 포함)
    │   ├── diagnosisListData.js            ← 진단 이력 목록 mock
    │   └── productData.js                  ← 제품 목록 + 상세 mock (auditLog 포함)
    │
    ├── services/                           ← API 호출 추상화 (현재는 mock 반환, 연동 시 fetch로 교체)
    │   ├── diagnosisService.js             ← getDiagnosisList, getDiagnosisResult, runDiagnosis
    │   └── productService.js               ← getProducts, getProductKnowledge, saveProductKnowledge, createProduct
    │
    ├── hooks/
    │   └── usePermission.js                ← can(action) 권한 체크 훅 (mockAuth.role 기반, 연동 시 useAuth().role로 교체)
    │
    ├── components/
    │   ├── route/
    │   │   └── ProtectedRoute.jsx          ← 로그인/역할 기반 라우트 보호
    │   │
    │   ├── ui/
    │   │   ├── Pill.jsx                    ← req/warn/cond/ok 뱃지
    │   │   ├── MemoBox.jsx                 ← 강조 정보 박스
    │   │   ├── Pager.jsx                   ← 이전/다음 네비게이션
    │   │   ├── DonutChart.jsx              ← SVG 도넛 차트
    │   │   └── StatusMessage.jsx           ← loading/empty/error 상태 메시지
    │   │
    │   ├── layout/
    │   │   ├── AppLayout.jsx               ← 인증 영역 <Outlet> 래퍼
    │   │   ├── Sidebar.jsx / .module.css   ← 접기/펼치기 사이드바
    │   │   ├── Topbar.jsx / .module.css    ← 크럼브 + 검색 + LIVE
    │   │   └── KpiRow.jsx / .module.css    ← KPI 4개 카드
    │   │
    │   └── diagnosis/
    │       ├── DiagnosisStage.jsx          ← stage 공통 래퍼
    │       ├── S00Overview.jsx             ← 진단 개요
    │       ├── S01Input.jsx                ← 제품 입력
    │       ├── S02Connectors.jsx           ← 공공데이터 커넥터
    │       ├── S03PredLab.jsx              ← ML 예측
    │       ├── S04Market.jsx               ← 시장 분석
    │       ├── S05RegMatrix.jsx            ← 규제·인증 매트릭스
    │       ├── S06Recall.jsx               ← 리콜 위험 분석
    │       ├── S07FtoSupport.jsx           ← FTO + 지원사업
    │       ├── S08ExecBoard.jsx            ← 30/60/90 실행 보드
    │       ├── S09Report.jsx               ← 리포트
    │       └── S10Sources.jsx              ← 데이터 근거
    │
    └── pages/
        ├── auth/
        │   ├── Home.jsx / .module.css      ← 랜딩 홈
        │   ├── Login.jsx / Auth.module.css
        │   ├── Signup.jsx
        │   ├── SignupPersonal.jsx
        │   └── SignupCompany.jsx
        │
        ├── diagnosis/
        │   ├── DiagnosisList.jsx           ← /diagnosis
        │   ├── DiagnosisNew.jsx            ← /diagnosis/new  (진단 요청 폼)
        │   ├── DiagnosisResult.jsx         ← /diagnosis/:id  (핵심 workspace UI)
        │   └── DiagnosisSources.jsx        ← /diagnosis/:id/sources
        │
        └── mypage/
            ├── Mypage.jsx                  ← /mypage
            ├── Knowledge.jsx               ← /mypage/knowledge
            ├── KnowledgeNew.jsx            ← /mypage/knowledge/new
            ├── KnowledgeDetail.jsx         ← /mypage/knowledge/:productId
            └── KnowledgeEdit.jsx           ← /mypage/knowledge/:productId/edit
```

---

### Routing Structure (App.jsx)

| 경로 | 컴포넌트 | 접근 조건 |
| :--- | :--- | :--- |
| `/` | Home.jsx | 공개 |
| `/login` | Login.jsx | 공개 |
| `/signup` | Signup.jsx | 공개 |
| `/signup/personal` | SignupPersonal.jsx | 공개 |
| `/signup/company` | SignupCompany.jsx | 공개 |
| `/diagnosis` | DiagnosisList.jsx | 로그인 필요 |
| `/diagnosis/new` | DiagnosisNew.jsx | 로그인 필요 |
| `/diagnosis/:id` | DiagnosisResult.jsx | 로그인 필요 |
| `/diagnosis/:id/sources` | DiagnosisSources.jsx | 로그인 필요 |
| `/mypage` | Mypage.jsx | 로그인 필요 |
| `/mypage/knowledge` | Knowledge.jsx | 로그인 필요 |
| `/mypage/knowledge/new` | KnowledgeNew.jsx | 회사 계정 필요 (owner/admin) |
| `/mypage/knowledge/:productId` | KnowledgeDetail.jsx | 로그인 필요 |
| `/mypage/knowledge/:productId/edit` | KnowledgeEdit.jsx | 회사 계정 필요 (owner/admin/editor) |

> `ProtectedRoute`는 현재 `mockAuth`를 참조. (백엔드 연동 시 `useAuth()` 훅으로 교체)

---