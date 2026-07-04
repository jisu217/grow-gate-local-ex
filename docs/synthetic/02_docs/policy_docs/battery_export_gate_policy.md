---
synthetic_data: true
company: 창원에너지팩 주식회사
product: 산업용 리튬이온 배터리팩 48V 80Ah
created_at: 2026-07-03
data_classification: synthetic_demo_not_real_customer
---

# 창원에너지팩 주식회사 수출 Gate 운영 정책

## 목적
창원에너지팩 주식회사 합성 페르소나의 수출 견적 전 검토 절차를 정의합니다.

## Gate 원칙
- GO: 인증·FTO·리콜·바이어 문서가 견적 확정 전 모두 충족되는 경우
- CONDITIONAL_GO: 경미한 보완 조건이 있으나 바이어에게 조건부 회신 가능한 경우
- HOLD: 인증/공공데이터/fallback 등 핵심 근거가 부족해 견적 확정 전 담당자 확인이 필요한 경우
- STOP: 규제 또는 FTO high risk가 존재해 외부 전문가 확인 전 양산 견적을 금지하는 경우

## AI 사용 원칙
LLM/Agent는 보고서 초안, 문서 분류, 감정분석, 고급 검색, SHAP 해석 보완에만 사용합니다. 최종 Gate와 수치 예측 결과는 deterministic rule 또는 검증된 ML 모델 결과를 기준으로 하며, AI 생성 claim은 source id가 없으면 보고서 본문에 확정 표현으로 반영하지 않습니다.
