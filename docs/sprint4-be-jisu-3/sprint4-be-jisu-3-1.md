### 피드백 요청 사항

과제 1번: `1) 현재 ProductService.delete()가 어떤 순서로 감사 로그와 제품 삭제를 처리하는지 읽습니다.`

### 처리 순서

| 단계 | 위치 | 내용 |
| :--- | :--- | :--- |
| 1 | `findUser` (:168) | `userId`로 사용자 조회, 없으면 `404 NOT_FOUND` |
| 2 | `requireRole` (:169, :199-213) | 역할이 `owner`/`admin`이 아니면 `403 FORBIDDEN` |
| 3 | `findProductByUser` (:170) | 소유/소속 조건으로 제품 조회, 없으면(타인 소유·이미 삭제됨) `404 NOT_FOUND` |
| 4 | 감사 로그 생성 (:172-179) | `AuditLog(changeType=delete)` 빌드 후 `auditLogRepository.save(log)` 먼저 실행 |
| 5 | 제품 삭제 (:181-182) | `product.softDelete(user.getId())`로 `deletedAt`/`deletedBy` 세팅 후 `productRepository.save(product)` |

### 변경사항

없음
