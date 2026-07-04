# Codex Prompt 06 — ML 예측/분류 + SHAP 해석 구현

목표:
수치 예측과 분류는 ML 모델로 돌리고, SHAP 해석을 산출한 뒤 Agent가 보완 설명한다.

입력:
- `01_synthetic_data/ml_training_synthetic_cases.csv`

작업:
1. `grow_gate_local_ex_demo/backend/ml/` 또는 `backend/BE-02`와 분리된 Python worker module을 추가한다.
2. 학습 스크립트: `train_models.py`
   - GateClassifier: gate_label classification
   - DelayRegressor: delay_risk_days regression
   - QuoteHoursRegressor: quote_review_hours regression
   - SupportMatcher: support_match_label classification
3. 모델은 sklearn HistGradientBoosting/RandomForest를 기본값으로 사용하고, XGBoost/LightGBM은 optional로 둔다.
4. SHAP이 설치되어 있으면 TreeExplainer, 없으면 permutation importance fallback.
5. 예측 endpoint `POST /ml/predict-gate-v1`를 추가한다.
6. 응답은 `predictedGate`, `probabilities`, `delayRiskDays`, `quoteReviewHours`, `supportMatchProbability`, `shapTopFeatures`, `modelVersion`, `syntheticTraining`, `limitations`를 반환한다.
7. Spring `FastApiAgentClient`에 ml endpoint client를 추가한다.
8. `DiagnosisAsyncProcessor`는 ML 결과를 `mlPrediction`에 저장하되 deterministic gate를 override하지 않는다. 차이가 나면 `gateDisagreement` warning을 표시한다.

수용 조건:
- synthetic CSV로 학습/예측 smoke test 통과.
- `llmInfluencedGate=false` 유지.
- SHAP top feature가 S03 또는 S10에 표시된다.
- syntheticTraining=true이면 보고서에 limitation이 표시된다.
