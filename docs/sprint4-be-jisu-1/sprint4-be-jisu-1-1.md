## B-1 API 계약서와 BE-02 Controller 차이 정리

### 피드백 요청 사항

- 주신 과제: `1) backend/BE-01/api-contract.md와 BE-02 Controller 목록을 비교합니다.`
- 진행 내용: BE-01 API 계약서와 BE-02 Controller 목록을 비교하고, 일치 항목을 제외한 차이 항목을 정리

### 확인 기준

- API 계약서: `backend/BE-01/api-contract.md`
- 실제 컨트롤러: `backend/BE-02/src/main/java/com/growgate/**/controller/*Controller.java`
- 비교 기준: HTTP method, path, 컨트롤러 존재 여부
- 정리 기준: 일치 항목 제외, 차이 항목 중심 정리

### 표기 차이

| 계약서 API | BE-02 실제 API | Controller | 차이 |
| :--- | :--- | :--- | :--- |
| `GET /api/diagnosis/:id` | `GET /api/diagnosis/{diagnosisId}` | `DiagnosisController.getDetail()` | 경로 변수 표기 차이 |
| `GET /api/diagnosis/:id/sources` | `GET /api/diagnosis/{diagnosisId}/sources` | `DiagnosisController.getSources()` | 경로 변수 표기 차이 |

### BE-02 추가 API

| BE-02 실제 API | Controller | 비고 |
| :--- | :--- | :--- |
| `GET /health` | `HealthController.health()` | 서버 상태 확인 API |
| `GET /api/companies/me/members` | `MembershipController.getMyCompanyMembers()` | 회사 팀원 목록 조회 API |
| `POST /api/products/{productId}/certifications` | `KnowledgeController.createCertification()` | 인증서 등록 |
| `PUT /api/products/{productId}/certifications/{certificationId}` | `KnowledgeController.updateCertification()` | 인증서 수정 |
| `DELETE /api/products/{productId}/certifications/{certificationId}` | `KnowledgeController.deleteCertification()` | 인증서 삭제 |
| `POST /api/products/{productId}/fto` | `KnowledgeController.createFto()` | FTO 항목 등록 |
| `PUT /api/products/{productId}/fto/{ftoId}` | `KnowledgeController.updateFto()` | FTO 항목 수정 |
| `DELETE /api/products/{productId}/fto/{ftoId}` | `KnowledgeController.deleteFto()` | FTO 항목 삭제 |
| `POST /api/products/{productId}/buyer-requirements` | `KnowledgeController.createBuyerRequirement()` | 바이어 요구사항 등록 |
| `PUT /api/products/{productId}/buyer-requirements/{buyerRequirementId}` | `KnowledgeController.updateBuyerRequirement()` | 바이어 요구사항 수정 |
| `DELETE /api/products/{productId}/buyer-requirements/{buyerRequirementId}` | `KnowledgeController.deleteBuyerRequirement()` | 바이어 요구사항 삭제 |
| `POST /api/products/{productId}/knowledge/sources` | `KnowledgeController.createKnowledgeSource()` | 지식 출처 등록 |
| `DELETE /api/products/{productId}/knowledge/sources/{sourceId}` | `KnowledgeController.deleteKnowledgeSource()` | 지식 출처 삭제 |

### 응답 DTO 차이

| DTO | 차이 |
| :--- | :--- |
| `KnowledgeDetailResponse` | 계약서 필드 외 `knowledgeSources` 포함 |
