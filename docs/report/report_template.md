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

## 근거 데이터와 AI 보조 해석 구분

### 근거 데이터

- Provider:
- sourceType: live/cache/fallback/seed/empty
- 조회일:
- 원문 링크:
- rawCount:
- normalizedCount:
- license/terms status:

### AI 보조 해석

- 요약:
- 리스크 후보:
- 확인 필요 사항:

주의: 본 내용은 자동 생성된 의사결정 보조 정보이며, 법률/인증/수출 실행 여부의 최종 판단이 아닙니다.

## FTO 사전 탐색 고지

본 결과는 공개 특허·상표 데이터와 입력 정보를 기반으로 한 사전 탐색 결과입니다. 특허 침해 여부, 실시 가능성, 권리 유효성에 대한 법률 판단이 아니며, 최종 의사결정 전 변리사 또는 법률 전문가 검토가 필요합니다.

## ML Advisory Signal

Include model version and limitations. Do not call this export success probability.

## Source Appendix

List source ids and claim verification results.

## Synthetic Validation Disclaimer

synthetic persona validation은 실제 고객 검증 자료가 아닙니다. 합성 데이터는 시연 경로와 UX 검증용으로만 사용하며 live 공공데이터 proof와 구분합니다.

## 개인정보·영업비밀 입력 제한

데모 환경에는 주민등록번호, 연락처, 계약서, 도면, 원가, 거래처명, 미공개 인증문서 등 개인정보 또는 영업비밀에 해당할 수 있는 정보를 입력하지 마십시오. 실제 개인정보가 포함될 경우 별도 동의와 처리방침이 필요합니다.
