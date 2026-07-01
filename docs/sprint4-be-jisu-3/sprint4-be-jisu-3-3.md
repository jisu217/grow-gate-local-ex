### 피드백 요청 사항

과제 3번: `3) 추천 정책은 soft delete입니다. deletedAt 또는 status 필드를 두고 목록 조회에서 제외하는 방향을 검토합니다.`

### 구현 현황

| 항목 | 위치 | 내용 |
| :--- | :--- | :--- |
| 컬럼 | `products.deleted_at`, `products.deleted_by` | MySQL(`01_schema.sql:59-60, 64`)·SQLite(`schema.sql`) 스키마 모두 존재, `idx_products_deleted_at` 인덱스로 조회 최적화 |
| 엔티티 | `Product.deletedAt`/`deletedBy`(:64-68) | `softDelete(deletedBy)`(:91-95)에서 `deletedAt=now()`, `deletedBy` 세팅, row는 유지 |
| 목록 조회 제외 | `findByCompany_IdAndDeletedAtIsNullOrderByUpdatedAtDesc`, `findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc`(`ProductService.java:228, 230`) | 삭제된 제품은 목록에서 조회 조건 자체로 제외 |
| 단건 조회 제외 | `findByIdAndCompany_IdAndDeletedAtIsNull`, `findByIdAndOwnerIdAndDeletedAtIsNull`(:236, 239) | 삭제된 제품은 상세조회/재삭제 시 `404 NOT_FOUND`로 처리되어 목록뿐 아니라 단건 접근에서도 제외 |

`deletedAt` 컬럼 방식(별도 `status` enum 대신)은 "언제 삭제됐는지"까지 함께 남기므로 이력 추적 요구에 더 부합하고, 현재 코드베이스가 이미 이 방향으로 구현되어 있어 완료 기준(삭제 후 목록 미노출 + audit log/diagnosis history 보존)을 충족함.

### 변경사항

없음
