# Synthetic Persona Task Protocol

**Global disclaimer:** synthetic, not real customer data, persona-based validation. All sessions are synthetic and persona-based. They are not real customer interviews.

## Task A. 바이어 문의 기반 첫 진단

- Input: 제품명, HS code, 대상국, 바이어 요구, 납기, 보유 문서
- Success: 결과 화면 도착 후 5분 안에 rule-based Gate와 상위 risk 3개 확인
- Measures: `task_success`, `time_to_first_gate_sec`, `confidence_before`, `confidence_after`
- Guardrail: LLM output may help with report text, but it must not override `finalGate`; keep `llmInfluencedGate=false`.

## Task B. S10 출처 검증

- Input: S10 Public Data Proof 화면
- Success: persona can explain live/cache/fallback differences and raw hash meaning
- Measures: `source_trust_score`, `unsupported_claim_concern`, `proof_comprehension`
- Guardrail: fallback records cannot be presented as live public-data proof.

## Task C. Markdown Report And Buyer Reply

- Input: Markdown report and buyer-facing brief
- Success: persona can edit and use a report/reply draft for internal reporting or buyer communication
- Measures: `report_usefulness`, `edit_distance_pct`, `willingness_to_use_again`
- Guardrail: buyer-facing copy must preserve synthetic, not real customer data, persona-based validation status in demo/admin contexts.
