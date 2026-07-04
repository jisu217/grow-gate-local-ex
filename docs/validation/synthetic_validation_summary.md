# Synthetic Validation Summary

**Global disclaimer:** synthetic, not real customer data, persona-based validation. This is synthetic persona validation, not real customer interview evidence.

## Summary

- Personas: 5
- Synthetic task sessions: 15
- Scenarios: battery Germany/EU, marine US, medical Japan, multi-company consultant
- Task success: 15 / 15 synthetic sessions
- Median manual baseline: 150 minutes
- Median GrowGate time: 15 minutes
- Estimate type: `synthetic_persona_assumption`
- Not real customer data: true

## Safe Presentation Sentence

> 실제 고객 검증 전 단계로, 3개 가상 제조기업과 5개 사용자 페르소나를 설계해 바이어 문의, 진단, 출처 검증, 보고서 작성까지의 모의 사용자 검증을 수행했습니다. 실제 인터뷰 데이터가 아니라 synthetic persona validation입니다.

## Risky Sentence To Avoid

> 실제 고객 검증 5건을 완료했습니다.

## Impact Estimate

```json
{
  "estimateType": "synthetic_persona_assumption",
  "notRealCustomerData": true,
  "calculationBasis": "PV-01~PV-05 persona task simulation",
  "confidence": "low_to_medium",
  "manualBaselineMinutes": 150,
  "growgateMinutes": 15,
  "timeReductionPct": 90.0,
  "limitations": [
    "실제 기업 인터뷰가 아니라 가상 페르소나 기반 모의 검증입니다.",
    "금액 절감은 실제 계약·인증 비용이 아닌 업무시간 가정 기반입니다."
  ]
}
```

## Interpretation

The result suggests the workflow is understandable for early product validation. It does not prove market adoption, willingness to pay, model accuracy, production ROI, or actual customer demand.

## Required Presentation Disclaimer

> 이 수치는 실제 고객 검증 자료가 아니라 가상 페르소나 기반 모의 검증입니다. 실제 기업 인터뷰와 운영 로그로 후속 검증해야 합니다.
