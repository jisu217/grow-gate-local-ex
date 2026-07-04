# Codex Prompt 10 — Trajectory/ML/Public Data 회귀 테스트

목표:
심사위원이 공격할 질문을 테스트로 방어한다.

작업:
1. `07_eval_test/expected_trajectory_eval_cases.json`를 test fixture로 복사한다.
2. `AgentTrajectoryEvaluationTest` 작성:
   - battery_germany_hold route 확인
   - marine_us_conditional route 확인
   - medical_japan_stop route 확인
3. `PublicDataProofGuardTest` 작성:
   - synthetic seed는 live/cache proof로 계산되지 않음
   - fallback positive contribution <= 0
4. `MlPredictionContractTest` 작성:
   - `/ml/predict-gate-v1` response schema 검증
   - SHAP/permutation top features 존재
5. `ReportClaimVerificationTest` 작성:
   - sourceId 없는 숫자 claim 차단

수용 조건:
- Gradle test 또는 documented test command 통과.
- Python worker test 통과.
- CI 캡처 제출 가능.
