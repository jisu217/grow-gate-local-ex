### 피드백 요청 사항

과제 4번: `4) pending -> running -> done 상태 변화가 실패 시에도 추적 가능하도록 로그와 예외 처리를 정리합니다.`

### 상태 흐름

| 단계 | 트리거 | 처리 | 로그 |
| :--- | :--- | :--- | :--- |
| pending | `create()` 저장 | `DiagnosisRun` insert | - |
| → running | 커밋 후 `dispatchAfterCommit` → `process()` | `updateStatus(running)` | `previous=pending, current=running` |
| → done | 채점/공공데이터/agent/LLM 처리 성공 | `setResult(resultJson)` + `updateStatus(done)` | `previous=running, current=done` |
| → failed | 처리 중 예외 발생 | `setResult(buildFailureResult(e))` + `updateStatus(failed)` | `previous=running, current=failed` + `log.error(e)` |

`dispatchAfterCommit`(:298-310)은 `TransactionSynchronizationManager`로 insert 커밋 후에만 비동기 처리를 시작해 미커밋 row 조회 경합을 막고, `process()` 전체가 단일 `try/catch(Exception e)`(:77-204)로 감싸져 있어 어느 단계에서 실패하든 `failed` 상태로 수렴하며 `errorType`/`errorMessage`/`failedAt`이 `result`에 안전하게 기록되어 실패 시에도 추적이 가능함

### 변경사항

없음
