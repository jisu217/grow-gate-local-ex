### 피드백 요청 사항

과제 2번: `2) hard delete, cascade delete, soft delete 중 서비스 목적에 맞는 정책을 제안합니다.`

### 정책 비교

| 정책 | 동작 | `audit_logs` 영향 | `diagnosis_runs` 영향 | 평가 |
| :--- | :--- | :--- | :--- | :--- |
| hard delete | `products` row를 즉시 `DELETE` | `fk_audit_logs_product`가 `products(id)`를 참조(:183)하므로 FK 위반으로 삭제 자체가 실패하거나, FK를 느슨하게 두면 감사 로그가 존재하지 않는 제품을 가리키는 고아 레코드가 됨 | `fk_diagnosis_runs_product`(:206)도 동일하게 FK 위반/고아 레코드 위험 | 감사·진단 이력 보존 요구와 정면으로 충돌 |
| cascade delete | 제품 삭제 시 연관 `audit_logs`, `diagnosis_runs`, `certifications` 등을 함께 삭제 | 삭제 자체는 성립하지만 감사 이력이 통째로 사라짐 | 진단 이력도 함께 사라짐 | "이력 보존" 요구사항과 반대 방향이라 채택 불가 |
| soft delete | `products.deleted_at`/`deleted_by`만 채우고 row는 유지 | FK 대상이 그대로 남아 감사 로그 무결성 유지 | 진단 이력도 `product_id`로 계속 연결·조회 가능 | 삭제 목록 제외 + 이력 보존을 동시에 만족 |

### 변경사항

없음
