# PR #46 S09 리포트 guardrail 충돌 해결 기록

작성일: 2026-07-05

대상 PR: <https://github.com/jiyongKim615/grow_gate_local_ex/pull/46>  
브랜치: `feature/hyewon-ux-cleanup` -> `main`  
충돌 파일: `frontend/FE-02/src/components/diagnosis/S09Report.jsx`

## 요약

PR #46의 merge conflict는 기능 로직 충돌이 아니라 S09 최종 리포트 화면의 안내/면책 문구 충돌이었다.

해결 방향은 한쪽 문구를 선택하는 것이 아니라 두 브랜치의 의도를 병합하는 방식으로 정했다.

- `feature/hyewon-ux-cleanup`의 UX 의도: 규제 답변에서 추천 근거, 최신성, 전문가 확인 필요 여부를 함께 확인하게 한다.
- `main`의 투명성 의도: 자동 생성 정보는 법률/인증/수출 실행의 최종 판단이 아니며, `fallback/seed` 데이터를 live 공공 API 결과처럼 표시하지 않는다.

## 최종 반영 문구

S09 리포트의 `.guardrail` 문구는 아래 내용으로 정리했다.

```jsx
<div className="guardrail">
  모든 규제·인증 답변은 추천 근거, 최신성, 전문가 확인 필요 여부를 함께 확인하십시오. 본 내용은 등록 제품 정보와 공공데이터 출처 신호 기반의 자동 생성 의사결정 보조 정보이며, 법률/인증/수출 실행 여부의 최종 판단이 아닙니다. fallback/seed 데이터는 live 공공 API 결과처럼 표시하지 않으며, 최종 판단은 인증 전문가·변리사·법률 전문가와 함께 하십시오.
</div>
```

## 유지한 정책

- 최종 Gate는 deterministic rule score와 risk/fallback 조건으로 결정한다.
- LLM 또는 AI 보조 해석은 리포트 문구, source-bound explanation, trace summary에만 사용되는 보조 정보다.
- `finalGate`는 LLM 출력으로 override하지 않는다.
- 공공데이터 근거는 `live`, `cache`, `fallback`, `seed`를 분리해서 표시해야 한다.
- `fallback/seed` 데이터는 live 공공 API 호출 성공 결과처럼 표시하지 않는다.
- 규제, 인증, 수출 실행, FTO, 법률 판단은 인증 전문가, 변리사, 법률 전문가의 최종 확인이 필요하다.

## 작업 방식

1. PR #46의 head branch가 `feature/hyewon-ux-cleanup`이고 base branch가 `main`임을 확인했다.
2. 기존 작업트리의 unrelated 변경을 건드리지 않기 위해 `/private/tmp/growgate-pr46`에 별도 worktree를 만들었다.
3. PR branch에 `origin/main`을 merge해 충돌을 재현했다.
4. `S09Report.jsx`의 conflict marker를 제거했다.
5. `.guardrail` 문구만 병합 문구로 교체했다.
6. merge commit `c21a5e0`을 `feature/hyewon-ux-cleanup`에 push했다.

## 검증

실행한 명령:

```bash
rg "^(<<<<<<<|=======|>>>>>>>)" .
npm run test:mappers
npm run build
gh pr view 46 --repo jiyongKim615/grow_gate_local_ex --json number,headRefName,baseRefName,mergeStateStatus,url
gh pr checks 46 --repo jiyongKim615/grow_gate_local_ex
```

결과:

- conflict marker 검색: 남은 marker 없음
- `npm run test:mappers`: 통과
- `npm run build`: PR worktree에 `node_modules`가 없어 `vite: command not found`로 실패
- PR merge state: `DIRTY`에서 `UNSTABLE`로 변경됨
- GitHub checks: `risk-gates` 통과, `backend`와 `frontend`는 당시 pending

## 남은 리스크

PR conflict 자체는 해결되었지만, 최종 merge 가능 여부는 GitHub Actions의 `backend`와 `frontend` check 완료 결과를 확인해야 한다.

빌드 실패는 코드 우회 없이 환경 상태로 기록했다. 동일 소스는 기존 workspace의 `frontend/FE-02/node_modules`가 있는 환경에서는 `npm run build`가 통과했지만, PR worktree 자체에는 의존성이 설치되어 있지 않았다.
