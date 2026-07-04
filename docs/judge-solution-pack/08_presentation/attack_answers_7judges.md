# 7명 심사위원 공격 질문 대응 답변

## 실제 고객 데이터입니까?
아닙니다. 이 패키지는 실제 고객 데이터를 위장하지 않습니다. 실제 업무 흐름을 모사한 합성 PoC 데이터이며, 심사장 데모와 ML 초기 벤치마크용입니다. 실제 고객 검증은 별도 인터뷰/동의/익명 로그로 확보합니다.

## 공공데이터 live record는 몇 건입니까?
S10 Proof Card와 `/api/public-data/health`에서 live/cache, operationEndpoint, apiKeyConfigured, rawResponseHash를 봅니다. 합성 seed는 proofEligible=false라서 live record로 카운트하지 않습니다.

## AI Agent가 판단합니까?
최종 Gate는 deterministic rule과 검증된 feature 기반 ML 보조 예측을 비교해 결정합니다. LLM/Agent는 보고서, 초안 작성, 문서 분류, 감정분석, 고급 검색, SHAP 해석 보완을 맡고 Gate를 override하지 않습니다.

## ML 모델은 실제입니까?
Codex 구현 후 synthetic CSV로 baseline ML을 학습하고, 모델/평가/SHAP 결과를 표시합니다. 합성 학습 결과는 실제 정확도 주장으로 쓰지 않고, 실제 검증셋 확보 전까지 limitation을 붙입니다.

## KOTRA/KIPRIS/SafetyKorea 들어가면 되지 않나요?
GROW-GATE의 차별점은 정보 조회가 아니라 바이어 RFQ 직후 견적 전 Gate, 업무 보드, source contribution ledger, agent trace, 보고서로 연결하는 것입니다.