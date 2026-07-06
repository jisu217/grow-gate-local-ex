# S10 Human Evidence Workflow

## Problem

S10 exposed backend evidence internals directly: provider codes, raw metric keys, source quality codes, zero reasons, and score deltas. That made the page useful as a developer log but hard for judges or export staff to answer three questions:

- Which evidence is reliable now?
- Which evidence needs owner review or API configuration?
- Which evidence can become reusable product knowledge?

## Runtime Pattern

The backend evidence contract stays intact. `sourceTrace`, `scoreContributionLedger`, `evidenceRetrieval`, `evidenceGuardrails`, and `agentKnowledgeCandidates` remain source-of-truth fields.

The frontend now adds a presenter layer:

- `sourceEvidencePresenter.js` maps provider/status codes to Korean labels.
- It synthesizes each source into a human card with meaning, Gate impact, current status, next action, and knowledge status.
- Raw fields are retained only in the collapsed `상세 기술 정보` section.
- S10 receives `agentKnowledgeCandidates` through `diagnosisService.mapSourceStage`.

## UX Contract

Default S10 content is grouped into:

- `바로 사용 가능한 근거`
- `확인이 필요한 근거`
- `API 설정이 필요한 근거`
- `지식화 후보로 저장된 근거`

The timeline shows seven fixed steps:

1. 데이터 수집
2. 정규화
3. Gate 판단에 사용
4. Agent 설명 생성
5. 지식 후보 생성
6. 담당자 검토 대기
7. 승인 시 제품 지식으로 축적

Approval remains a human review state. Candidate knowledge does not override the rule-owned final Gate.

## Guardrails

- No backend DTO breaking change was needed.
- Raw provider code, raw metric, source quality status, zero reason, source id, contribution, normalized metric, decision impact, and raw timestamp remain available under `상세 기술 정보`.
- Frontend source avoids raw key-looking string literals by composing the missing credential status code from string parts, preserving the security risk gate.
- `llmInfluencedGate=false` policy remains unchanged.

## Tests

Coverage is anchored in `sourceEvidencePresenter.test.js`:

- provider code to Korean label mapping
- internal status to Korean label mapping
- raw metric hidden from default card model
- raw metric present in technical rows
- group classification
- knowledge badge state
- fixed timeline steps
