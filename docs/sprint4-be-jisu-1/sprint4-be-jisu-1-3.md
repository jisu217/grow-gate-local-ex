## FE-02 실제 호출 Endpoint와 BE-02 이름 차이 정리

### 피드백 요청 사항

- 주신 과제: `3) 프런트가 지금 실제로 호출하는 endpoint와 이름이 다른 부분을 찾습니다.`
- 진행 내용: FE-02 서비스 코드가 실제 호출하는 endpoint와 BE-02 구현 endpoint 및 필드명 차이를 정리

### 확인 기준

- 프런트 호출부: `frontend/FE-02/src/services`
- 백엔드 구현부: `backend/BE-02/src/main/java/com/growgate/**/controller`
- API 베이스: FE `apiClient.js` 기준 `${VITE_API_URL}/api`
- 비교 기준: 실제 호출 endpoint, 화면/서비스 명칭, 요청/응답 필드명

### Endpoint 차이

| FE 서비스 | FE 실제 호출 | BE-02 실제 endpoint | 차이 |
| :--- | :--- | :--- | :--- |
| `diagnosisService.getDiagnosisResult()` | `GET /api/diagnosis/{id}` | `GET /api/diagnosis/{diagnosisId}` | 경로 변수명 차이 |
| `diagnosisService.getDiagnosisSources()` | `GET /api/diagnosis/{id}/sources` | `GET /api/diagnosis/{diagnosisId}/sources` | 경로 변수명 차이 |

### Endpoint 명칭 차이

| 화면/FE 명칭 | FE 실제 호출 | BE-02 명칭 | 차이 |
| :--- | :--- | :--- | :--- |
| 마이페이지 제품 지식 | `GET /api/products` | 제품 목록 조회 | 화면명은 `knowledge`, BE 리소스명은 `products` |
| 제품 기본 정보 수정 | `PUT /api/products/{id}/knowledge` | 제품 지식 수정 | FE 기능명은 기본 정보 수정, BE endpoint는 지식 수정에 포함 |
| 저장된 제품 지식 불러오기 | `GET /api/products` | 제품 목록 조회 | FE 동작명은 지식 불러오기, BE 호출은 제품 목록 |
| AI 진단 다시 실행 | `POST /api/diagnosis` | 새 진단 실행 | FE 명칭은 재실행, BE endpoint는 신규 진단 생성 |
| 진단 결과 라우트 ID | `id` | `diagnosisId` | FE 라우트 파라미터명과 BE PathVariable명 차이 |

### 요청 필드명 차이

| FE 내부 이름 | BE 요청 필드 | 사용 위치 | 비고 |
| :--- | :--- | :--- | :--- |
| `productName` | `name` | 제품 생성/수정 | `productService`, `mypageService`에서 변환 |
| `hs` | `hsCode` | 마이페이지 제품 생성/수정 | `mypageService`에서 변환 |
| `country` | `targetCountry` | 마이페이지 제품 생성/수정 | `mypageService`에서 변환 |
| `targetMarket` | `targetCountry` | 진단 실행 | `diagnosisService.runDiagnosis()`에서 변환 |
| `id` | `productId` | 제품 상세/수정/삭제 | FE 라우트/상태명과 BE 식별자명 차이 |

### 응답 필드명 차이

| BE 응답 필드 | FE 내부 이름 | 사용 위치 | 비고 |
| :--- | :--- | :--- | :--- |
| `productId` | `id` | 제품 목록/상세 | FE 카드/라우팅용 ID로 변환 |
| `trustGrade` | `grade` | 제품 목록/상세 | FE 표시용 이름 |
| `expiringCertCount` | `expiringCerts` | 제품 목록 | FE 표시용 이름 |
| `missingFieldCount` | `missingFields.length` | 제품 목록 | BE는 건수, FE는 배열 길이 사용 |
| `knowledgeStatus` | `knowledgeStatus` | 제품 목록 | 값 이름 변환: `expiring_soon` → `expiring`, `needs_review` → `needs-check`, `incomplete` → `missing` |
| `expiresAt` | `expiryDate` | 인증서 표시 | FE 표시용 이름 |
| `targetCountry` | `target` | FTO 표시 | FE 표시용 이름 |
| `riskLevel` | `status` | FTO 표시 | FE 표시용 이름 |
| `changedAt` | `date` | 감사 로그 | FE 표시용 이름 |
| `changedBy` | `by` | 감사 로그 | FE 표시용 이름 |
| `changedFields` | `field` | 감사 로그 | 배열을 문자열로 변환 |
| `previousValue` | `oldValue` | 감사 로그 | FE 표시용 이름 |
| `createdAt` | `requestedAt` | 진단 목록 | FE 카드 표시용 이름 |
| `targetCountry` | `targetMarket` | 진단 목록/상세 | FE 화면 용어와 BE 계약 용어 차이 |
| `diagnosisId` | `id` | 진단 실행 응답 | FE 라우팅/상태용 ID로 변환 |

### 미호출 Endpoint

| BE-02 endpoint | FE 호출 여부 | 비고 |
| :--- | :--- | :--- |
| `PATCH /api/users/me` | 미호출 | 마이페이지 계정 수정 화면/서비스 미연동 |
| `PATCH /api/companies/me` | 미호출 | 마이페이지 회사 수정 화면/서비스 미연동 |
| `GET /health` | 미호출 | 프런트 기능 화면에서 직접 호출하지 않음 |
