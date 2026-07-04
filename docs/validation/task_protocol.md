# Synthetic Persona Task Protocol

All sessions are synthetic and persona-based. They are not real customer interviews.

## Task A. Buyer Request To First Gate

- Input: product name, HS code, target country, buyer request, deadline, documents
- Success: Gate and top 3 risks found within 5 minutes of result screen arrival
- Measures: `task_success`, `time_to_first_gate_sec`, `confidence_before`, `confidence_after`

## Task B. Source Proof Review

- Input: S10 Public Data Proof screen
- Success: persona can explain live/cache/fallback and raw hash
- Measures: `source_trust_score`, `unsupported_claim_concern`, `proof_comprehension`

## Task C. Markdown Report And Buyer Reply

- Input: Markdown report and buyer-facing brief
- Success: persona can edit and send a report/reply draft
- Measures: `report_usefulness`, `edit_distance_pct`, `willingness_to_use_again`
