### 피드백 요청 사항

과제 5번: `5) DiagnosisDetailResponse와 DiagnosisSourcesResponse가 프론트에서 쓰기 좋은 형태인지 확인합니다.`

### BE 응답 ↔ FE 소비 매핑

| 구분 | BE 응답 필드 | FE 소비 위치 | 비고 |
| :--- | :--- | :--- | :--- |
| `DiagnosisDetailResponse` | `result.*`, `knowledgeSnapshot.*`, `productId`/`companyId`/`productName`/`targetCountry`/`status` | `session.*` (camelCase 그대로 구조 분해) | 별도 매핑 계층 없이 그대로 사용, null 허용 필드는 `!= null` 가드 처리 |
| `DiagnosisSourcesResponse` | `sources[].provider`/`.documentName`/`.field`/`.scoreContribution`/`.fallbackUsed` | 라벨 조합, 부호 포함 문자열(`+3`) 변환, `Fallback` 라벨 분기 | 표시용 가공만 FE에서 수행 |
| `DiagnosisListResponse.DiagnosisSummary` | `hsCode` 누락 | `DiagnosisList.jsx:68` (`HS {diag.hsCode}`) | 실사용 결함 확인 |

두 DTO 모두 필드명 변환 없이 그대로 소비되어 구조상 적합하지만, 목록 응답에서만 `hsCode`가 누락되어 있어 FE 표시가 깨지는 결함을 발견함

### 변경사항

| 파일 | 변경 내용 |
| :--- | :--- |
| `DiagnosisListResponse.java` | `DiagnosisSummary`에 `hsCode` 필드 추가 |
| `DiagnosisService.java` | `resolveHsCode(run)` 추가 (제품 보유 시 `product.getHsCode()`, 직접 입력 시 `inputData.hsCode`), `getList()`에 연결 |
| `frontend/.../diagnosisService.js` | `mapDiagnosisSummary()`의 하드코딩 `hsCode: ''` → `d.hsCode ?? ''` |

상세 응답(`DiagnosisDetailResponse`)은 `knowledgeSnapshot.hsCode`로 이미 값이 보존되고 FE 상세 화면에서 top-level 소비처가 없어 필드는 추가하지 않음
