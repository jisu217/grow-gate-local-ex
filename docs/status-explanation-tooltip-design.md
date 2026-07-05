# Status Explanation Tooltip Design

## 기능 목적

진단 상세 화면의 `긴급`, `보강`, `고위험`, `중위험`, `누락위험`, `fallback`, `미연결` 같은 상태 배지는 사용자가 불안하게 받아들일 수 있다. 이 기능은 상태 배지에 마우스를 올리거나 키보드 포커스, 모바일 탭을 했을 때 쉬운 말로 판단 이유와 다음 행동을 설명한다.

목표는 부정 상태를 숨기는 것이 아니라, 왜 그렇게 표시됐는지와 무엇을 보완해야 하는지를 source-bound하게 보여주는 것이다.

## 적용 상태값

- 긴급
- 보강
- 고위험
- 중위험
- 누락위험
- fallback
- not configured
- insufficient evidence
- 담당자 확인 필요
- 검토 필요
- 미연결
- 출처확인
- 검토
- 확인
- 높음
- 대기

## 설명 데이터 구조

프론트 매퍼는 각 배지 항목에 다음 구조를 붙인다.

```json
{
  "severity": "high",
  "status": "긴급",
  "plainSummary": "수출 전에 먼저 확인해야 할 필수 항목이 남아 있습니다.",
  "reason": "대상국 규제나 인증 근거가 부족하거나, 견적 전에 확인해야 할 조건이 있습니다.",
  "nextAction": [
    "인증/규제 담당자 검토 후 견적을 확정하세요.",
    "필요한 인증서와 시험성적서 보유 여부를 먼저 확인하세요."
  ],
  "sourceType": "cache",
  "evidenceCount": 5,
  "confidence": "medium",
  "dataStatus": "데이터 상태 cache · 근거 5건"
}
```

## 상태별 쉬운말 Template

- 긴급: 수출 전에 먼저 확인해야 할 필수 항목이 남아 있음
- 보강: 진단은 가능하지만 근거 자료를 더 채우면 판단이 좋아짐
- 고위험: 그대로 진행하면 일정이나 비용 문제가 커질 수 있음
- 중위험: 바로 중단할 정도는 아니지만 확인할 항목이 있음
- 누락위험: 판단에 필요한 자료가 빠져 있음
- fallback: 실시간 데이터 대신 예비 데이터로 판단함
- not configured: API key 또는 endpoint 설정이 부족함
- insufficient evidence: 판단에 필요한 근거 데이터가 충분하지 않음
- 미연결: 필요한 근거 데이터 연결이 되지 않음
- 출처확인: 원문 출처 확인 후 비용이나 기간을 확정해야 함

## Source Trace 연결 방식

현재 구현은 LLM으로 설명을 생성하지 않는다. `statusExplanation.js`의 deterministic template이 상태값, sourceType, evidenceCount, reason을 조합한다.

연결 위치:

- S05 규제·인증: priority, status, cost
- S06 리콜 위험: risk badge, checklist item
- S07 FTO·지원: patent badge, FTO trace text, support deadline chip
- S08 실행 보드: task tag

sourceType과 evidenceCount가 있는 경우 tooltip의 데이터 상태에 그대로 표시한다. 없으면 `unknown` 또는 `insufficient_evidence`로 표시해 없는 근거를 지어내지 않는다.

## QA 체크리스트

- 부정 상태 배지에 hover하면 설명이 열린다.
- 키보드 Tab으로 배지에 접근하면 설명이 열린다.
- 모바일 폭에서 tap/focus로 설명이 열린다.
- 설명에는 쉬운 요약, 판단 근거, 다음 행동, 데이터 상태가 포함된다.
- sourceType/evidenceCount가 있으면 표시된다.
- 설명 데이터가 없어도 기본 template으로 표시된다.
- 진단 점수 산식과 Gate 결정은 변경하지 않는다.

## 심사위원 설명 문장

GROW-GATE Local EX는 긴급, 고위험, 보강 같은 부정 상태를 단순히 표시하는 데 그치지 않고, 마우스를 올리면 Agent 판단 이유를 쉬운 말로 설명합니다. 사용자는 어떤 근거 데이터 때문에 위험으로 분류됐는지, live/cache/fallback 중 어떤 데이터 상태인지, 다음에 무엇을 보완해야 하는지 바로 확인할 수 있습니다.
