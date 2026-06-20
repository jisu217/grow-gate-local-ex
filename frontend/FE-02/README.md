# GROW-GATE Local EX — Frontend

---

## Table of Contents

1. [Key Screens](#1-key-screens)
2. [Tech Stack](#2-tech-stack)
3. [Getting Started](#3-getting-started)
4. [Project Structure](#4-project-structure)
5. [Routing](#5-routing)
6. [Permission Structure](#6-permission-structure)
7. [Development Guidelines](#7-development-guidelines)
8. [Backend Integration Guide](#8-backend-integration-guide)
9. [Required Backend APIs](#9-required-backend-apis)
10. [TODO](#10-todo)

---

## 1. Key Screens

| 홈 | AI 진단 결과 |
| :--- | :--- |
| ![홈 화면](docs/screenshot-home.png) | ![진단 결과 화면](docs/screenshot-diagnosis.png) |

---

## 2. Tech Stack

| 항목 | 내용 |
| :--- | :--- |
| 프레임워크 | React 18 + Vite 5 |
| 라우팅 | React Router v6 |
| 스타일 | CSS Modules |
| 언어 | JavaScript (JSX, TypeScript 미사용) |

---

## 3. Getting Started

### 요구 사항

- Node.js `v18` 이상 (LTS 권장)
- npm `v9` 이상

### 설치 및 실행

```bash
npm install        # 의존성 설치
npm run dev        # 개발 서버 (http://localhost:5173)
npm run build      # 프로덕션 빌드
npm run preview    # 빌드 결과 로컬 미리보기
```

### 환경 변수

```bash
cp .env.example .env
```

| 변수 | 설명 | 기본값 |
| :--- | :--- | :--- |
| `VITE_API_URL` | 백엔드 API 기본 URL | `http://localhost:8080` |

> Vite에서 브라우저에 노출되는 환경 변수는 `VITE_` 접두사 필수.  
> 코드에서 `import.meta.env.VITE_API_URL`로 접근.

---

## 4. Project Structure

```
src/
├── main.jsx                    # ReactDOM 마운트
├── App.jsx                     # 라우트 전체 정의
├── styles/global.css           # CSS 변수 + 전역 스타일
│
├── mock/                       # 백엔드 연동 전 임시 데이터
│   ├── auth.js                 # 로그인 상태 (isLoggedIn, role, accountType)
│   ├── roles.js                # 역할별 허용 액션 테이블
│   ├── diagnosisSteps.js       # 진단 단계 정의 (UI 전용)
│   ├── diagnosisData.js        # S00~S10 진단 결과 데이터
│   ├── diagnosisListData.js    # 진단 이력 목록 데이터
│   └── productData.js          # 제품 지식 데이터
│
├── services/                   # API 추상화 계층
│   ├── diagnosisService.js     # 진단 관련 (목록 조회, 결과 조회, 실행)
│   └── productService.js       # 제품 지식 관련 (조회, 저장, 생성)
│
├── hooks/
│   └── usePermission.js        # 역할 기반 권한 확인 훅
│
├── components/
│   ├── route/ProtectedRoute.jsx        # 로그인·권한 보호 라우트
│   ├── layout/                         # AppLayout, Sidebar, Topbar, KpiRow
│   ├── ui/                             # Pill, MemoBox, Pager, DonutChart, StatusMessage
│   └── diagnosis/                      # DiagnosisStage + S00~S10 스테이지 컴포넌트
│
└── pages/
    ├── auth/                   # Home, Login, Signup, SignupPersonal, SignupCompany
    ├── diagnosis/              # DiagnosisList, DiagnosisNew, DiagnosisResult, DiagnosisSources
    └── mypage/                 # Mypage, Knowledge, KnowledgeNew, KnowledgeDetail, KnowledgeEdit
```

---

## 5. Routing

| 경로 | 페이지 | 접근 조건 |
| :--- | :--- | :--- |
| `/` | Home | 공개 |
| `/login` | Login | 공개 |
| `/signup` | Signup | 공개 |
| `/signup/personal` | SignupPersonal | 공개 |
| `/signup/company` | SignupCompany | 공개 |
| `/diagnosis` | DiagnosisList | 로그인 |
| `/diagnosis/new` | DiagnosisNew | 로그인 |
| `/diagnosis/:id` | DiagnosisResult | 로그인 |
| `/diagnosis/:id/sources` | DiagnosisSources | 로그인 |
| `/mypage` | Mypage | 로그인 |
| `/mypage/knowledge` | Knowledge | 로그인 |
| `/mypage/knowledge/new` | KnowledgeNew | 기업 계정 |
| `/mypage/knowledge/:productId` | KnowledgeDetail | 로그인 |
| `/mypage/knowledge/:productId/edit` | KnowledgeEdit | 기업 계정 |

---

## 6. Permission Structure

역할 계층: `owner` > `admin` > `editor` > `viewer`

| 액션 | owner | admin | editor | viewer |
| :--- | :--- | :--- | :--- | :--- |
| `knowledge:create` | ✓ | ✓ | | |
| `knowledge:edit` | ✓ | ✓ | ✓ | |
| `knowledge:view` | ✓ | ✓ | ✓ | ✓ |
| `audit:view` | ✓ | ✓ | | |

컴포넌트에서 `usePermission` 훅으로 접근.

```js
const { can } = usePermission()
if (can('knowledge:edit')) { ... }
```

---

## 7. Development Guidelines

### 스타일

- 전역 공유 스타일 → `styles/global.css` (CSS 변수, reset, 공통 클래스)
- 컴포넌트 고유 스타일 → `ComponentName.module.css` (CSS Modules, 로컬 스코프)
- 전역 클래스 (`pill`, `big`, `eyebrow` 등)는 `global.css`에서만 정의

### 파일 명명

| 대상 | 규칙 | 예시 |
| :--- | :--- | :--- |
| 컴포넌트 | PascalCase | `DiagnosisResult.jsx` |
| CSS 모듈 | 컴포넌트명 동일 | `DiagnosisResult.module.css` |
| 서비스·훅·목업 | camelCase | `diagnosisService.js` |
| 상수 | UPPER_SNAKE_CASE | `DIAGNOSIS_STEPS` |

### 컴포넌트 패턴

- 페이지 컴포넌트는 `pages/` 하위, 재사용 컴포넌트는 `components/` 하위로 분리
- 데이터 조회는 반드시 `services/`를 통해 수행 (컴포넌트에서 `mock/` 직접 import 금지)

---

## 8. Backend Integration Guide

현재 `services/`는 `mock/` 데이터를 반환. 연동 시 **`services/` 내부 함수만 교체**하면 컴포넌트 변경 불필요.

```js
// 연동 전
export async function getProducts() {
  return PRODUCTS
}

// 연동 후
export async function getProducts() {
  const res = await fetch(`${import.meta.env.VITE_API_URL}/api/products`)
  return res.json()
}
```

**연동 후 삭제 대상**

| 파일 | 처리 |
| :--- | :--- |
| `mock/productData.js` | 삭제 |
| `mock/diagnosisData.js` | 삭제 |
| `mock/diagnosisListData.js` | 삭제 |
| `mock/auth.js` | AuthContext로 교체 후 삭제 |
| `mock/roles.js` | 서버 권한 응답으로 교체 후 삭제 |
| `mock/diagnosisSteps.js` | UI 전용이므로 유지 가능 |

---

## 9. Required Backend APIs

> 현재 모두 mock 대체 상태. 백엔드 연동 시 아래 엔드포인트 구현 필요.

### 인증·사용자

| 메서드 | 경로 | 설명 | 서비스 파일 |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | 이메일·비밀번호 로그인, JWT 발급 | — (미구현) |
| `POST` | `/api/auth/signup` | 회원가입 (개인/기업 구분은 요청 바디로 전달) | — (미구현) |
| `POST` | `/api/auth/logout` | 로그아웃 | — (미구현) |
| `GET` | `/api/users/me` | 현재 로그인 사용자 정보 조회 | — (미구현) |
| `PATCH` | `/api/users/me` | 사용자 정보 수정 | — (미구현) |
| `GET` | `/api/companies/me` | 소속 기업 정보 조회 (기업 계정) | — (미구현) |
| `PATCH` | `/api/companies/me` | 소속 기업 정보 수정 (기업 계정) | — (미구현) |

### 제품 지식

| 메서드 | 경로 | 설명 | 서비스 함수 |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/products` | 제품 목록 조회 | `getProducts()` |
| `POST` | `/api/products` | 신규 제품 생성 | `createProduct(payload)` |
| `GET` | `/api/products/:productId/knowledge` | 제품 상세·지식 조회 | `getProductKnowledge(id)` |
| `PUT` | `/api/products/:productId/knowledge` | 제품 지식 수정 | `saveProductKnowledge(id, payload)` |
| `DELETE` | `/api/products/:productId` | 제품 삭제 | — (미구현) |
| `GET` | `/api/products/:productId/audit-logs` | 제품 지식 변경 이력 조회 | — (미구현) |

### 진단

| 메서드 | 경로 | 설명 | 서비스 함수 |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/diagnosis` | 진단 이력 목록 조회 | `getDiagnosisList()` |
| `POST` | `/api/diagnosis` | 새 진단 실행 요청 | `runDiagnosis(payload)` |
| `GET` | `/api/diagnosis/:id` | 진단 결과 상세 조회 | `getDiagnosisResult(id)` |
| `GET` | `/api/diagnosis/:id/sources` | 진단 출처 데이터 조회 | — (미구현) |

> 화면 라우트(`/mypage/knowledge`, `/diagnosis` 등)는 [§5 Routing](#5-routing)의 UI 경로로 위 표의 `/api/...` 경로와는 별개. 화면 라우트는 유지, 실제 백엔드 호출만 `/api` 경로로 연결.

---

## 10. TODO

### 인증·사용자

- [ ] 실제 로그인 API 연동 및 JWT 토큰 저장 (현재: mock 고정값)
- [ ] 회원가입 API 연동 (현재: 폼 UI만 존재, 제출 시 navigate만)
- [ ] 로그아웃 기능
- [ ] 로그인 상태 유지 (LocalStorage / AuthContext)
- [ ] 마이페이지 사용자 정보 API 연동 (현재: 하드코딩)

### 진단

- [ ] 새 진단 제출 시 실제 API 호출 (현재: 항상 `diag-001`로 이동)
- [ ] 진단 진행 상태 폴링 / WebSocket 실시간 업데이트
- [ ] "저장된 제품 지식 불러오기" 버튼 동작 연결
- [ ] 진단 저장·삭제 기능

### 제품 지식

- [ ] KnowledgeEdit 저장 시 `saveProductKnowledge()` 실제 호출 (현재: navigate만)
- [ ] KnowledgeNew 생성 시 `createProduct()` 실제 호출 (현재: navigate만)
- [ ] 문서 파일 업로드 기능

### 공통

- [ ] 에러 경계(ErrorBoundary) 컴포넌트
- [ ] 접근 토큰 만료 시 자동 재발급 처리
- [ ] 화면 스크린샷 / 데모 영상 추가

---