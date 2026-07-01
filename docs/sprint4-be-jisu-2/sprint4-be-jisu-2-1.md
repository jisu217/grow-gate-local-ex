### 피드백 요청 사항

과제 1번: `1) CreateDiagnosisRequest가 productId와 inputData를 어떻게 받는지 확인합니다.`

### 분기별 처리 비교

| 구분 | `productId` 사용 (등록 제품) | `inputData` 사용 (직접 입력) |
| :--- | :--- | :--- |
| 전제 | 이미 등록된 제품을 대상으로 진단 | 미등록 제품 정보를 요청 시점에 직접 전달 |
| 소유권 검증 | `findProductByUser`로 접근 권한 확인 | 해당 없음 |
| 스냅샷 생성 | `buildKnowledgeSnapshot(productId)` — 축적된 제품 지식 기반 | `buildInputSnapshot(inputData, targetCountry)` — 요청값 기반 |
| 저장 결과 | `product` 연관관계 설정, `inputData` 컬럼 null | `product` null, `inputData` 컬럼에 직렬화된 JSON 저장 |

요청 바디는 두 값을 동시에 수용하되, 서비스 계층에서 상호 배타 조건을 강제하고, 채워진 값에 따라 DB 조회 경로와 직렬화 저장 경로로 완전히 분리해 처리함

### 변경사항

없음