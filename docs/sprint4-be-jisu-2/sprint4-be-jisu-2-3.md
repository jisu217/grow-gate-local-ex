### 피드백 요청 사항

과제 3번: `3) 직접 입력값만 있을 때는 targetCountry와 hsCode가 result 또는 snapshot에 남도록 구조를 점검합니다.`

### 저장 구조

| 컬럼 | 생성 메서드 | 포함 필드 |
| :--- | :--- | :--- |
| `diagnosis_runs.input_data` | `serializeInputData` | `productName`, `hsCode`, `description` |
| `diagnosis_runs.knowledge_snapshot` | `buildInputSnapshot` | `productName`, `hsCode`, `targetCountry`, `sourceType`, `missingFields` |
| `diagnosis_runs.target_country` | 컬럼 자체 | `targetCountry` (`nullable = false`) |

비동기 처리(`DiagnosisAsyncProcessor.process`, :80-85)도 `product == null`일 때 `inputData`에서 `hsCode`/`productName`/`description`을 정규화해 재사용하고 최종 `result.inputTrace`(:188-195)에 `hsCode`/`targetCountry`를 재기록해, 컬럼/스냅샷/트레이스 3중으로 보존되어 완료 기준을 충족함

### 변경사항

없음
