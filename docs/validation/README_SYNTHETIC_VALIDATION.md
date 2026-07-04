# Synthetic Persona Validation

**Global disclaimer:** synthetic, not real customer data, not real customer interviews, persona-based validation.

이 데이터팩은 **실제 고객 검증 자료가 아님**. GROW-GATE의 바이어 문의, rule-based Gate, S10 출처 검증, Markdown report/회신문 활용 흐름을 실제 고객 검증 전 단계에서 점검하기 위한 가상 페르소나 기반 모의 검증 자료입니다.

다시 명시합니다. 이 자료는 **실제 고객 검증 자료가 아님**. 고객 traction, paid usage, field-verified ROI, 실제 기업 도입 사례로 표현하면 안 됩니다.

세 번째로 명시합니다. 이 자료는 **실제 고객 검증 자료가 아님**. 3개 가상 제조기업과 5개 사용자 페르소나를 설계해 현장성 가설을 검토한 synthetic persona validation입니다.

## Safe Presentation Sentence

> 실제 고객 검증 전 단계로, 3개 가상 제조기업과 5개 사용자 페르소나를 설계해 바이어 문의, 진단, 출처 검증, 보고서 작성까지의 모의 사용자 검증을 수행했습니다. 실제 인터뷰 데이터가 아니라 synthetic persona validation입니다.

## Unsafe Sentence

> 실제 고객 검증 5건을 완료했습니다.

## Purpose

- 심사평에서 지적된 고객 검증 공백을 실제 검증처럼 포장하지 않고, 현장성 가설 검증 자료로 정리합니다.
- persona별 업무 소요시간 self-estimate와 GrowGate task 수행 시간을 비교하되, 모든 수치는 `synthetic_persona_assumption`으로 표시합니다.
- final Gate는 deterministic rule score와 risk/fallback 조건으로 결정되며, LLM 출력은 report text와 source-bound explanation 보조에만 사용된다는 원칙을 유지합니다.

## Artifacts

- `persona_profiles.json`
- `task_protocol.md`
- `synthetic_interview_transcripts.jsonl`
- `synthetic_validation_sessions.csv`
- `synthetic_validation_summary.md`
- `limitations.md`
- `../../database/mysql/seed/2026-07-04_seed_synthetic_persona_validation.sql`
- `../../frontend/FE-02/src/fixtures/syntheticPersonaValidationSummary.json`

Every file carries the phrase `synthetic, not real customer data, persona-based validation`, and every session row keeps `synthetic_disclaimer=true`.
