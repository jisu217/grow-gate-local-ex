# Codex Prompt 01 — 제출 범위/합성 데이터 안전장치

너는 `grow_gate_local_ex-main` 저장소를 수정한다. 최종 제출 대상은 `frontend/FE-02`, `backend/BE-02`, `database`, `docs`, `scripts`이다.

목표:
1. 합성 데이터가 실제 고객 데이터 또는 live 공공데이터로 오인되지 않도록 전역 안전장치를 추가한다.
2. README, S10, seed SQL, 보고서에 `synthetic_demo_not_real_customer` 라벨을 표시한다.
3. `sourceType=seed`를 `live` 또는 `cache`로 바꾸지 않는다.

작업:
- `docs/SYNTHETIC_DATA_POLICY.md`를 추가하고, 합성 데이터 사용 범위와 금지 표현을 문서화한다.
- `backend/BE-02`에 synthetic flag가 있는 diagnosis result는 reportMarkdown 첫 문단에 warning을 붙인다.
- `frontend/FE-02` S10/SourceTrustBar에 seedCount가 있으면 “Seed는 live API proof가 아닙니다” 배지를 표시한다.
- `scripts/check-risk-gates.sh`에 synthetic 데이터가 `operationEndpoint=true`, `apiKeyConfigured=true`, `sourceType=live`로 들어가면 실패하도록 grep/json 검사 추가한다.

수용 조건:
- `grep -R "synthetic_demo_not_real_customer" docs database frontend backend` 결과가 존재한다.
- seed diagnosis 결과 화면에서 seed 배지가 보인다.
- live/cache proof 숫자는 synthetic seed로 증가하지 않는다.
