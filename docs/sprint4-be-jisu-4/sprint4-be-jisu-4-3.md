### 피드백 요청 사항

과제 3번: `3) 다른 회사 productId로 knowledge 조회, 수정, 진단 실행을 시도하는 실패 테스트를 추가합니다.`

### 검증 대상 로직

| 서비스 | 메서드 | 위치 | 내용 |
| :--- | :--- | :--- | :--- |
| `ProductService` | `delete` | `findProductByUser` (:232-240) | 소유/소속 조건에 맞지 않는 `productId`는 `404 NOT_FOUND` |
| `KnowledgeService` | `getDetail`, `update` | `findProductByUser` (:699-707) | 다른 회사의 `productId`로 조회·수정 시도 시 `productRepository.findByIdAndCompany_IdAndDeletedAtIsNull`이 빈 값을 반환하여 `404 NOT_FOUND` |
| `DiagnosisService` | `create` | `findProductByUser` (:370-378) | `productId`가 지정된 경우 동일한 방식으로 소속 회사 검증, 실패 시 `404 NOT_FOUND`. 이 검증이 `diagnosisRepository.save` 이전에 실행되므로 다른 회사 제품으로는 진단 실행 자체가 생성되지 않음 |

세 서비스 모두 `productRepository.findByIdAndCompany_IdAndDeletedAtIsNull(productId, companyId)` / `findByIdAndOwnerIdAndDeletedAtIsNull(productId, ownerId)`로 소유·소속 범위를 DB 조회 조건 자체에 강제하고 있어, 다른 회사·다른 사용자의 `productId`는 애초에 엔티티로 조회되지 않고 `404`로 귀결됨을 확인함.

### 추가한 테스트

| 파일 | 테스트명 | 검증 내용 |
| :--- | :--- | :--- |
| `ProductServiceTest.java` | `deleteFailsWithNotFoundWhenProductIdBelongsToDifferentOwner` | 다른 개인 계정 소유 `productId`로 삭제 시도 시 `404 NOT_FOUND`, 감사 로그·저장 미실행 검증 |
| `ProductServiceTest.java` | `deleteFailsWithNotFoundWhenProductIdBelongsToDifferentCompany` | 다른 회사 소속 `productId`로 삭제 시도 시 `404 NOT_FOUND` 검증 |
| `KnowledgeServiceTest.java` | `getDetailFailsWithNotFoundWhenProductIdBelongsToDifferentCompany` | 다른 회사 `productId`로 지식 상세 조회 시 `404 NOT_FOUND` 검증 |
| `KnowledgeServiceTest.java` | `updateFailsWithNotFoundWhenProductIdBelongsToDifferentCompany` | 다른 회사 `productId`로 지식 수정 시도 시 `404 NOT_FOUND`, 감사 로그 미기록 검증 |
| `DiagnosisServiceTest.java` | `createFailsWithNotFoundWhenProductIdBelongsToDifferentCompany` | 다른 회사 `productId`로 진단 실행 시도 시 `404 NOT_FOUND`, 진단 레코드 저장 및 비동기 처리 미호출 검증 |

### 변경사항

| 파일 | 변경 내용 |
| :--- | :--- |
| `ProductServiceTest.java` | 다른 소유자/다른 회사 `productId`에 대한 삭제 실패 테스트 2건 추가 |
| `KnowledgeServiceTest.java` | 신규 생성. 다른 회사 `productId`에 대한 조회·수정 실패 테스트 포함 |
| `DiagnosisServiceTest.java` | 신규 생성. 다른 회사 `productId`에 대한 진단 실행 실패 테스트 포함 |
