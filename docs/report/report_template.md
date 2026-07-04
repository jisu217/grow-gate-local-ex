---
reportId: rpt_example
diagnosisId: replace-me
companyScenario: synthetic_demo_not_real_customer_or_real_company
publicDataProofRunId: replace-with-proof-run-id
mlModelVersion: growgate-gate-risk-v0.1.0
reportFormat: markdown
pdfStatus: optional_print_export
limitations:
  - 본 보고서는 법률·인증 최종 판단이 아닌 견적 전 risk screening 자료입니다.
  - LLM 보조 문장은 source trace와 claim verification을 통과한 설명에 한정합니다.
  - synthetic persona validation은 실제 고객 검증 자료가 아닙니다.
---

# GROW-GATE Export Gate Report

## Gate Decision

Rule-based Gate and key risks.

## Public Data Proof

Include proofRunId, provider sourceType, raw hash, and schema validation.

## ML Advisory Signal

Include model version and limitations. Do not call this export success probability.

## Source Appendix

List source ids and claim verification results.

## Synthetic Validation Disclaimer

synthetic persona validation은 실제 고객 검증 자료가 아닙니다. 합성 데이터는 시연 경로와 UX 검증용으로만 사용하며 live 공공데이터 proof와 구분합니다.
