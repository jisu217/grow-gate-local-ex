# Legacy Demo and Mock Removal

## Context

심사 리스크는 실제 제출 앱과 과거 정적 데모, 개발용 fixture가 한 저장소에 섞여 보이는 데 있었다. 특히 mock fallback이 남아 있으면 API 실패가 정상 화면처럼 보일 수 있고, standalone HTML에 과장된 문구가 남으면 현재 구현 범위와 다른 주장으로 해석될 수 있다.

## Decision

- 제출 판단 대상은 `frontend/FE-02` React 앱과 `backend/BE-02` Spring API로 고정한다.
- 과거 정적 demo와 FE mock fixture는 실행 경로에서 제거한다.
- 복구용 백업은 `artifacts/legacy-backups/legacy-demo-and-mock-backup-2026-07-03.zip`에만 보존한다.
- 제출 zip 생성은 `scripts/package-submission.sh`로 수행하고, 백업 zip과 legacy archive를 제외한다.

## Implementation

- `frontend/FE-02/src/mock` 제거
- `GROW_GATE_Local_EX_standalone.html` 제거
- `grow_gate_local_ex_demo/frontend` 제거
- `diagnosisService.js`, `productService.js`에서 mock fallback 로직 제거
- UI 상수는 `frontend/FE-02/src/constants`로 이동
- `scripts/check-risk-gates.sh`에서 legacy/static demo, mock fallback, 제출 zip 제외 규칙 검증
- `scripts/package-submission.sh`에서 `.git`, `.env`, `.gstack`, `.superpowers`, build output, virtualenv, legacy backup, nested zip 제외

## Review Prompts

- CSO: 사용자가 API 실패를 성공 화면으로 오해하지 않는가?
- SRE: 제출 zip에서 개발용 fixture와 백업 archive가 제외되는가?
- Performance: build artifact와 archive가 제출물 크기를 불필요하게 키우지 않는가?
- Judge-defense: 저장소를 열었을 때 어떤 앱이 실제 구현인지 즉시 보이는가?

## Verification

```bash
./scripts/check-risk-gates.sh
./scripts/package-submission.sh
cd frontend/FE-02 && npm run build
```

생성된 제출 zip은 `artifacts/submission/growgate-submission-*.zip`이며, `artifacts/legacy-backups/*`와 `*.zip` archive를 포함하지 않아야 한다.

추가로 `.env`, `.venv-aws`, `.gstack`, `.superpowers`, `.pytest_cache`, `.DS_Store` 같은 로컬 보안/도구 상태 파일도 포함하지 않아야 한다.
