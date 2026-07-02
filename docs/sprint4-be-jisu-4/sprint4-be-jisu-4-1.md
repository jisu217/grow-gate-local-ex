### 피드백 요청 사항

과제 1번: `1) 개인 계정은 owner_user_id 기준으로만 제품을 보는 테스트를 추가합니다.`

### 검증 대상 로직

| 위치 | 내용 |
| :--- | :--- |
| `ProductService.findProductsByUser` (:224-230) | `resolveCompany`가 `null`이면(개인 계정) `productRepository.findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(user.getId())` 호출 — `products.owner_user_id` 기준 조회 |
| `ProductService.getList` (:64-120) | 위 메서드로 조회한 목록만 응답에 포함, 다른 사용자의 제품은 애초에 리포지토리 호출 파라미터에 포함되지 않음 |

### 추가한 테스트

| 파일 | 테스트명 | 검증 내용 |
| :--- | :--- | :--- |
| `ProductServiceTest.java` | `getListOnlyReturnsProductsOwnedByRequestingUserForPersonalAccount` | 개인 계정으로 조회 시 `findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc("user-1")`만 호출되고, `findByCompany_IdAndDeletedAtIsNullOrderByUpdatedAtDesc`는 호출되지 않음을 검증. 응답에 본인 소유 제품(`product-1`)만 포함됨을 확인 |

### 변경사항

| 파일 | 변경 내용 |
| :--- | :--- |
| `ProductServiceTest.java` | `getListOnlyReturnsProductsOwnedByRequestingUserForPersonalAccount` 테스트 추가 |
