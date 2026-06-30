# PR #34 진단 흐름 보강 리뷰

작성일: 2026-07-01

대상 PR: <https://github.com/jiyongKim615/grow_gate_local_ex/pull/34>  
브랜치: `docs/sprint4-be-jisu-02` -> `main`  
커밋: `e76b491 feat: improve diagnosis flow handling`

## 결론

PR #34는 단독 브랜치 기준으로 백엔드 테스트가 통과하지만, 현재 `origin/main`에는 그대로 병합하면 안 된다.

이유는 최신 `main`에서 이미 진단 엔진에 FastAPI agent 연동, live 공공데이터 수집, PDF/report 응답, `dispatchAfterCommit` 기반 비동기 실행 보강이 들어갔고, PR #34가 같은 파일의 과거 버전을 기준으로 작성되어 3개 파일에서 병합 충돌이 발생하기 때문이다.

따라서 처리 방향은 "PR 그대로 merge"가 아니라 "PR #34의 유효한 개선점만 최신 main 코드에 수동 흡수"가 맞다.

## 확인 결과

| 항목 | 결과 |
| --- | --- |
| PR 상태 | OPEN |
| GitHub Checks | 설정 없음, reported checks 없음 |
| 변경 파일 | `DiagnosisDetailResponse.java`, `DiagnosisAsyncProcessor.java`, `DiagnosisService.java` |
| PR 단독 백엔드 테스트 | 통과, `./gradlew test --no-daemon` |
| 최신 main 병합 가능성 | 충돌 발생 |
| 검토 결론 | 조건부 반영 가능, 현재 PR 그대로 병합 불가 |

## 주요 문제

### P1. 최신 main과 병합 충돌

`git merge-tree $(git merge-base origin/main HEAD) origin/main HEAD` 기준으로 아래 파일들이 `changed in both` 상태다.

- `backend/BE-02/src/main/java/com/growgate/diagnosis/dto/DiagnosisDetailResponse.java`
- `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java`
- `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisService.java`

충돌 성격은 단순 포맷 문제가 아니다. 최신 main은 `agentTrace`, `stageCards`, `reportMarkdown`, `reportFastApiUsed`, 공공데이터 출처, report API, after-commit async dispatch를 갖고 있고, PR #34는 실패 결과 필드와 직접 입력 snapshot 필드를 추가한다.

해결 기준:

- `ResultData`는 최신 main의 agent/report 필드를 유지하고 PR #34의 `errorType`, `errorMessage`, `failedAt`을 추가한다.
- `KnowledgeSnapshot`은 PR #34의 `productName`, `hsCode`, `targetCountry` 필드를 추가한다.
- `DiagnosisAsyncProcessor`는 최신 main의 실제 점수 산출, 공공데이터 수집, FastAPI agent/report 흐름을 유지하고 PR #34의 `updateStatus()`와 실패 결과 저장만 병합한다.
- `DiagnosisService.create()`는 최신 main의 `dispatchAfterCommit(run.getId())`를 유지해야 한다.

### P1. `dispatchAfterCommit` 회귀 위험

PR #34 브랜치의 `DiagnosisService.create()`는 `diagnosisRepository.save(run)` 직후 `asyncProcessor.process(run.getId())`를 직접 호출한다.

현재 main은 트랜잭션 commit 이후 비동기 진단을 실행하도록 `dispatchAfterCommit(run.getId())`를 사용한다. 이 변경을 잃으면 async worker가 아직 commit되지 않은 진단 row를 조회하거나, 요청 트랜잭션과 비동기 처리가 섞이는 회귀가 생길 수 있다.

해결 기준:

- PR #34를 반영할 때 `asyncProcessor.process(run.getId())`로 되돌리지 않는다.
- 반드시 최신 main의 `dispatchAfterCommit(run.getId())` 구조를 유지한다.

### P2. 실패 상세 메시지 노출 위험

PR #34는 진단 실패 시 `errorMessage`에 `e.getMessage()`를 그대로 저장하고, `DiagnosisDetailResponse.ResultData.errorMessage`로 프론트에 내려줄 수 있다.

외부 API, DB, 파일, 직렬화 예외 메시지는 내부 URL, SQL, 설정값 일부, 인프라 정보가 섞일 수 있으므로 운영/제출용 서비스에서는 그대로 노출하지 않는 것이 안전하다.

권장 기준:

- 사용자 응답에는 일반화된 메시지 사용: `진단 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.`
- 내부 로그에는 stack trace 유지
- 필요하면 `errorType`, `failureId`, `failedAt`만 응답에 노출
- 공공데이터 API 실패처럼 외부 요청 정보가 포함될 수 있는 메시지는 키/토큰 마스킹 후 저장

### P2. 테스트 커버리지 부족

PR 설명에는 `./gradlew test 통과`가 적혀 있고 실제 PR 브랜치에서도 테스트는 통과했다. 다만 이번 변경이 보장해야 하는 동작을 직접 검증하는 테스트는 없다.

추가 권장 테스트:

- 직접 입력 진단 생성 시 `knowledgeSnapshot.productName`, `hsCode`, `targetCountry`, `snapshotAt`이 상세 응답에 포함되는지
- 진단 처리 실패 시 status가 `failed`로 저장되고 result에 `summary`, `errorType`, `failedAt`이 남는지
- 실패 응답에 raw exception message를 그대로 노출하지 않는지
- 최신 main 병합 후 기존 FastAPI report/public-data source 응답 필드가 유지되는지

## 반영할 가치가 있는 부분

PR #34의 아래 변경은 최신 main에 흡수할 가치가 있다.

- 직접 입력 진단에서도 `knowledgeSnapshot`을 생성하는 구조
- `KnowledgeSnapshot` 응답에 `productName`, `hsCode`, `targetCountry` 추가
- 진단 상태 전환을 공통 메서드로 묶고 상태 변경 로그를 남기는 방향
- 실패 시 `failed` 상태와 실패 시각/유형을 result JSON에 남기는 방향

단, 위 개선은 최신 main의 실제 진단 산출물과 배포 안정화 코드를 덮어쓰지 않는 방식으로만 반영해야 한다.

## 권장 처리 순서

1. PR #34 브랜치를 최신 `main`으로 rebase 또는 merge한다.
2. 3개 충돌 파일을 수동 해결한다.
3. 최신 main의 `agentTrace`, `stageCards`, `reportMarkdown`, `reportFastApiUsed`, 공공데이터 source 확장 필드를 유지한다.
4. 최신 main의 `dispatchAfterCommit()` 구조를 유지한다.
5. PR #34의 직접 입력 snapshot 필드와 실패 상태 저장 로직만 추가한다.
6. 실패 응답의 `errorMessage`는 raw exception 대신 안전한 메시지로 제한한다.
7. 백엔드 테스트를 실행한다.
8. 가능하면 직접 입력 진단과 실패 진단에 대한 단위 테스트를 추가한다.

## 실행한 검증 명령

```bash
gh pr view 34 --repo jiyongKim615/grow_gate_local_ex --json state,title,body,baseRefName,headRefName,url,isDraft,mergeable,reviewDecision,files,commits,comments,reviews
git fetch origin pull/34/head:refs/remotes/origin/pr/34
git worktree add /private/tmp/growgate-pr34-review origin/pr/34
git diff --stat origin/main...HEAD
git merge-tree $(git merge-base origin/main HEAD) origin/main HEAD
cd /private/tmp/growgate-pr34-review/backend/BE-02 && GRADLE_USER_HOME=.gradle-home ./gradlew test --no-daemon
gh pr checks 34 --repo jiyongKim615/grow_gate_local_ex
```

## 최종 판단

현재 상태의 PR #34는 "문제 없음"이 아니다. 기능 의도는 좋지만 최신 main과 충돌하며, 병합 해결을 잘못하면 최근 반영한 제출용 핵심 기능을 되돌릴 수 있다.

권장 결정은 `changes requested`에 가깝다. 다만 PR 작성자가 수정하기 어렵다면, main에서 별도 커밋으로 PR #34의 유효 변경만 선별 반영하는 방식이 더 안전하다.
