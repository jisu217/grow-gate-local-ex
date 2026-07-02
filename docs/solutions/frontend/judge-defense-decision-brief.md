# Judge Defense Decision Brief

## Context

심사 리스크는 기능 수보다 “누르면 실제로 동작하는가”, “API 실패 시 mock을 보여주는가”, “중소 제조기업 담당자가 다음 행동을 이해하는가”에 집중됐다. 특히 S10의 `파이프라인 실행` 버튼은 onClick이 없어 화면만 만든 것처럼 보일 수 있었고, legacy standalone demo의 과장 표현은 실제 제출 앱과 혼동될 수 있었다.

## Decision

1. S10 evidence 화면은 읽기 전용 검증 화면으로 고정한다.
   - 재수집은 진단 생성 또는 관리자 public-data refresh에서만 수행한다.
   - 근거 상세 화면에는 실행 버튼을 두지 않는다.

2. 결과 첫 화면에 `DecisionBrief`를 추가한다.
   - Gate 판단을 기준으로 핵심 위험 3개와 다음 행동 3개를 먼저 보여준다.
   - 다음 행동은 `오늘 할 일`, `이번 주 할 일`, `외부기관 문의`로 고정한다.
   - fallback/cache가 있으면 담당자 검토 필요를 Gate와 brief에서 바로 표시한다.

3. 보고서 산출물은 기본적으로 `Markdown 바이어 대응 메모`라고 표현한다.
   - PDF는 별도 endpoint와 smoke 검증이 있어도, 한국어 렌더링 QA 전에는 발표 기본 산출물로 주장하지 않는다.

4. mock과 standalone demo는 dev/legacy 용도로 격리한다.
   - `frontend/FE-02/src/mock/README.md`에 dev-only fixture 정책을 명시한다.
   - `GROW_GATE_Local_EX_standalone.html`은 `LEGACY DEMO - DO NOT JUDGE AS PRODUCTION APP` 배너를 포함한다.

## Guardrails

- `scripts/check-risk-gates.sh`가 다음 항목을 막는다.
  - S10의 비기능 `파이프라인 실행` 버튼 재도입
  - 첫 화면 `DecisionBrief` 누락
  - fallback 담당자 검토 경고 누락
  - 보고서 UI의 PDF 과장 표현
  - standalone demo의 `AI Agent`, `수출 가능성 예측`, `성공 가능성` 과장 표현

## Verification

- `./scripts/check-risk-gates.sh`
- `node frontend/FE-02/src/services/diagnosisService.mapper.test.js`
- `cd frontend/FE-02 && npm run build`

