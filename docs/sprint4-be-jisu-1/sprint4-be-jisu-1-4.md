## 응답 DTO의 hsCode, targetCountry, description 포함 여부 확인

### 피드백 요청 사항

- 주신 과제: `4) 응답 DTO에 hsCode, targetCountry, description이 빠지는 곳이 없는지 확인합니다.`
- 진행 내용: BE-02 응답 DTO별 `hsCode`, `targetCountry`, `description` 포함 여부와 보완 필요 항목을 정리

### 확인 기준

- 대상 코드: `backend/BE-02/src/main/java/com/growgate/**/dto`
- 확인 필드: `hsCode`, `targetCountry`, `description`
- 확인 관점: FE-02가 제품/진단 화면에서 제품 기본 정보 표시 또는 재사용 가능 여부

### 누락 여부 요약

| 응답 DTO | API | hsCode | targetCountry | description | 판단 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `DiagnosisListResponse.DiagnosisSummary` | `GET /api/diagnosis` | 없음 | 포함 | 없음 | `hsCode` 누락 가능성 있음 |
| `DiagnosisDetailResponse` | `GET /api/diagnosis/{diagnosisId}` | 없음 | 포함 | 없음 | `hsCode`, `description` 누락 가능성 있음 |

### 확인 필요 항목

| API | 빠진 필드 | 영향 |
| :--- | :--- | :--- |
| `GET /api/diagnosis` | `hsCode`, `description` | FE `diagnosisService.mapDiagnosisSummary()`에서 `hsCode`를 빈 문자열로 대체 중 |
| `GET /api/diagnosis/{diagnosisId}` | `hsCode`, `description` | 진단 결과 화면의 제품 입력/스냅샷 영역에서 제품 기본 정보 복원 제한 |

### 보완 후보

| DTO | 추가 후보 필드 | 비고 |
| :--- | :--- | :--- |
| `DiagnosisListResponse.DiagnosisSummary` | `hsCode` | 진단 목록 카드에서 HS Code 표시 필요 시 추가 |
| `DiagnosisDetailResponse` | `hsCode`, `description` | 저장 제품 진단과 직접 입력 진단 양쪽에서 결과 화면 기본 정보 표시 가능 |
