### 피드백 요청 사항

과제 4번: `4) 삭제 권한은 owner/admin만 가능하게 유지하고, editor/viewer 테스트를 추가합니다.`

### 권한 검증 로직

| 역할 | 위치 | 결과 |
| :--- | :--- | :--- |
| 역할 없음(`null`) | `requireRole` (:199-202) | `403 FORBIDDEN` |
| `owner` / `admin` | `requireRole` (:206) | 통과, 삭제 진행 |
| `editor` / `viewer` | `requireRole` (:206-208) | `403 FORBIDDEN` |

### 변경사항

| 파일 | 변경 내용 |
| :--- | :--- |
| `ProductServiceTest.java` | `deleteSoftDeletesProductAndRecordsAuditLogForOwnerRole`: 개인 계정(owner) 삭제 시 soft delete 반영과 감사 로그 기록 검증 추가 |
| `ProductServiceTest.java` | `deleteSoftDeletesProductForAdminRole`: 회사 소속 admin 멤버십 삭제 성공 검증 추가 |
| `ProductServiceTest.java` | `deleteIsForbiddenForEditorAndViewerRoles`: `editor`/`viewer` 역할에서 `BusinessException(FORBIDDEN)` 발생 및 조회/저장 로직 미호출 검증 추가(`@ParameterizedTest` + `@EnumSource`) |