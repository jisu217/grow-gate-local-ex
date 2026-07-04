# GROW-GATE 심사 보완 산출물 패키지

생성일: 2026-07-03

## 포함물
- `01_synthetic_data/2026-07-03_seed_3_synthetic_companies.sql`: 기존 MySQL 스키마에 바로 넣는 3개 합성 기업 seed
- `01_synthetic_data/ml_training_synthetic_cases.csv`: ML 분류/예측/SHAP 실험용 240개 합성 시나리오
- `02_docs/`: 위키, 정책 문서, 상품 문서
- `03_planning/GROW_GATE_7심사위원_전면보완_기획서.md`: 7명 심사위원 지적사항 해결 기획서
- `04_codex_prompts/`: 잘라서 실행하는 Codex 프롬프트 12개
- `05_ai_ml_design/`: AI/ML/SHAP/Agent 설계안
- `06_schema_migrations/`: Agent Trace, ML prediction, 지역산업 프로필, synthetic registry 보강 스키마
- `07_eval_test/`: trajectory eval fixture
- `08_presentation/`: 공격 질문 대응 답변

## 중요한 경고
이 패키지는 실제 기업/고객 데이터가 아닙니다. `synthetic_demo_not_real_customer` 라벨을 제거하거나, `sourceType=seed`를 live/cache로 바꾸면 심사 신뢰도가 크게 떨어집니다.

## 적용 순서
1. 기존 저장소 루트에 이 패키지를 복사합니다.
2. `06_schema_migrations/2026-07-03_agent_ml_regional_schema.sql` 적용.
3. `06_schema_migrations/2026-07-03_seed_regional_and_registry.sql` 적용.
4. `01_synthetic_data/2026-07-03_seed_3_synthetic_companies.sql` 적용.
5. Codex 프롬프트를 `04_codex_prompts/00_PROMPT_ORDER.md` 순서대로 실행합니다.