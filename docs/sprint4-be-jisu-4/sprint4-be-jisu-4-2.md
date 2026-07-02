### 피드백 요청 사항

과제 2번: `2) 기업 계정은 memberships.company_id 기준으로 같은 회사 제품만 보는 테스트를 추가합니다.`

### 검증 대상 로직

| 위치 | 내용 |
| :--- | :--- |
| `ProductService.resolveCompany` (:192-196) | `membershipRepository.findFirstByUserId(userId)`로 조회한 `Membership.company`를 반환 — `memberships.company_id`가 소속 회사를 결정 |
| `ProductService.findProductsByUser` (:224-230) | `resolveCompany`가 `null`이 아니면(기업 계정) `productRepository.findByCompany_IdAndDeletedAtIsNullOrderByUpdatedAtDesc(company.getId())` 호출 — `products.company_id`가 요청자의 `memberships.company_id`와 일치하는 제품만 조회 |

### 추가한 테스트

| 파일 | 테스트명 | 검증 내용 |
| :--- | :--- | :--- |
| `ProductServiceTest.java` | `getListOnlyReturnsProductsWithinRequestingUsersCompanyForCompanyAccount` | 멤버십의 `company_id`(`company-1`)로만 `findByCompany_IdAndDeletedAtIsNullOrderByUpdatedAtDesc`가 호출되고, `findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc`는 호출되지 않음을 검증. 응답에 같은 회사 제품(`product-2`)만 포함됨을 확인 |

### 변경사항

| 파일 | 변경 내용 |
| :--- | :--- |
| `ProductServiceTest.java` | `getListOnlyReturnsProductsWithinRequestingUsersCompanyForCompanyAccount` 테스트 추가 |
