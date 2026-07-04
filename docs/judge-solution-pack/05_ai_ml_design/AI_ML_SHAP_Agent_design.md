# GROW-GATE AI/ML/SHAP + Agent 설계안

## 역할 분리
- 최종 Gate: deterministic rule + ML 예측 결과를 검증된 feature로 비교하되, LLM이 직접 override하지 않는다.
- ML 모델: 수치 예측과 분류를 담당한다.
- SHAP: ML 예측의 피처 기여도를 계산한다.
- Agent: 보고서, 초안 작성, 문서 분류, 바이어 문의 감정분석, 고급 검색, SHAP 해석 보완을 담당한다.

## ML 파이프라인
1. `01_synthetic_data/ml_training_synthetic_cases.csv`를 로드한다.
2. `gate_label`은 classification, `delay_risk_days`, `quote_review_hours`는 regression, `support_match_label`은 binary classification으로 학습한다.
3. baseline은 `sklearn` RandomForest/HistGradientBoosting으로 구현하고, 가능하면 XGBoost/LightGBM은 optional dependency로 둔다.
4. train/test split은 company-stratified + time-like scenario split을 병행한다.
5. metrics: macro F1, balanced accuracy, RMSE/MAE, calibration curve.
6. SHAP은 tree explainer를 사용하되 패키지 설치 실패 시 permutation importance fallback을 둔다.

## FastAPI worker endpoint 초안
- `POST /ml/predict-gate-v1`
- request: product, hsCode, targetCountry, features, sourceTrustSummary
- response: predictedGate, probabilities, regressionOutputs, shapTopFeatures, modelVersion, syntheticTraining, limitations

## Agent 보완 로직
1. MLPredictionAgent가 예측과 SHAP top features를 반환한다.
2. ShapInterpretationAgent가 top feature를 source-bound evidence로 매핑한다.
3. ReportAgent가 다음 문장을 생성한다: “이번 Gate 판단은 cert_gap_count와 fto_risk_score가 크게 작용했으며, 근거 문서는 ... 입니다.”
4. VerifierAgent가 sourceId 없는 claim을 limitation으로 내린다.

## 안전장치
- syntheticTraining=true이면 보고서에 “합성 학습 기반 예측” limitation을 반드시 표시한다.
- SHAP feature는 숫자 설명이지 법적/인증 판단 근거가 아니다.
- FTO/PMDA/인증 최종 판단은 전문가 확인으로 제한한다.