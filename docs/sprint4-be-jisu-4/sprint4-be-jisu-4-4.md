### 피드백 요청 사항

과제 4번: `4) role owner/admin/editor/viewer별 create/edit/delete/audit 권한 테스트를 최소 1개씩 둡니다.`

### 권한 매트릭스

| 액션 | 서비스 | `requireRole` 조건 | owner | admin | editor | viewer |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| create | `ProductService` (:203-208) | `owner`/`admin`만 허용 | 허용 | 허용 | 403 | 403 |
| delete | `ProductService` (:203-208) | `owner`/`admin`만 허용 | 허용 | 허용 | 403 | 403 |
| edit(수정) | `KnowledgeService` (:684-688) | `viewer`만 차단 | 허용 | 허용 | 허용 | 403 |
| audit(로그 조회) | `KnowledgeService` (:689-693) | `owner`/`admin`만 허용 | 허용 | 허용 | 403 | 403 |

### 추가/기존 테스트 매핑

| 액션 | 역할 | 파일 | 테스트명 |
| :--- | :--- | :--- | :--- |
| create | owner | `ProductServiceTest.java` | `createStoresHsCodeAndTargetCountryForAuthenticatedUser` (기존) |
| create | admin | `ProductServiceTest.java` | `createSucceedsForAdminRoleWithinCompany` (신규) |
| create | editor/viewer | `ProductServiceTest.java` | `createIsForbiddenForEditorAndViewerRoles` (신규, `@ParameterizedTest`) |
| delete | owner | `ProductServiceTest.java` | `deleteSoftDeletesProductAndRecordsAuditLogForOwnerRole` (기존) |
| delete | admin | `ProductServiceTest.java` | `deleteSoftDeletesProductForAdminRole` (기존) |
| delete | editor/viewer | `ProductServiceTest.java` | `deleteIsForbiddenForEditorAndViewerRoles` (기존, `@ParameterizedTest`) |
| edit | owner/admin/editor | `KnowledgeServiceTest.java` | `updateSucceedsForOwnerAdminAndEditorRoles` (신규, `@ParameterizedTest`) |
| edit | viewer | `KnowledgeServiceTest.java` | `updateIsForbiddenForViewerRole` (신규) |
| audit | owner/admin | `KnowledgeServiceTest.java` | `getLogsIsAllowedForOwnerAndAdminRoles` (신규, `@ParameterizedTest`) |
| audit | editor/viewer | `KnowledgeServiceTest.java` | `getLogsIsForbiddenForEditorAndViewerRoles` (신규, `@ParameterizedTest`) |

모든 역할 × 액션 조합에 대해 최소 1개의 테스트가 존재하며, 테스트명이 `역할 + 허용/금지` 형태로 정책을 그대로 드러내도록 명명함. 진단 실행(`create` in `DiagnosisService`)에 대한 역할 테스트도 `DiagnosisServiceTest.createSucceedsForOwnerAdminAndEditorRolesAndDispatchesAsyncProcessing`, `createIsForbiddenForViewerRole`로 추가함.

### 변경사항

| 파일 | 변경 내용 |
| :--- | :--- |
| `ProductServiceTest.java` | `createSucceedsForAdminRoleWithinCompany`, `createIsForbiddenForEditorAndViewerRoles` 테스트 추가 |
| `KnowledgeServiceTest.java` | `updateSucceedsForOwnerAdminAndEditorRoles`, `updateIsForbiddenForViewerRole`, `getLogsIsAllowedForOwnerAndAdminRoles`, `getLogsIsForbiddenForEditorAndViewerRoles` 테스트 추가 |
| `DiagnosisServiceTest.java` | `createSucceedsForOwnerAdminAndEditorRolesAndDispatchesAsyncProcessing`, `createIsForbiddenForViewerRole` 테스트 추가 |
