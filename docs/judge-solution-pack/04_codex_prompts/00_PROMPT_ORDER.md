# Codex 프롬프트 실행 순서

01. scope_and_safety → `CODEX_PROMPT_01_scope_and_safety.md`
02. seed_3_companies → `CODEX_PROMPT_02_seed_3_companies.md`
03. regional_profiles → `CODEX_PROMPT_03_regional_profiles.md`
04. agent_trace_schema_ui → `CODEX_PROMPT_04_agent_trace_schema_ui.md`
05. structured_agent_outputs → `CODEX_PROMPT_05_structured_agent_outputs.md`
06. ml_prediction_shap → `CODEX_PROMPT_06_ml_prediction_shap.md`
07. agent_services_document_sentiment_search_report → `CODEX_PROMPT_07_agent_services_document_sentiment_search_report.md`
08. public_data_live_proof → `CODEX_PROMPT_08_public_data_live_proof.md`
09. demo_wizard_ux → `CODEX_PROMPT_09_demo_wizard_ux.md`
10. evaluation_tests → `CODEX_PROMPT_10_evaluation_tests.md`
11. reporting_docs_presentation → `CODEX_PROMPT_11_reporting_docs_presentation.md`
12. packaging_demo → `CODEX_PROMPT_12_packaging_demo.md`
13. multi_agent_public_data_harness → `CODEX_PROMPT_13_multi_agent_public_data_harness.md`
14. github_main_aws_zero_downtime_deploy_rollback → `CODEX_PROMPT_14_github_main_aws_zero_downtime_deploy_rollback.md`

권장 실행 방식: 01→02→03→04→05→06→07→08→09→10→11→12→13→14 순서. 중간마다 테스트와 빌드를 실행하고, synthetic seed를 live proof로 표시하지 않는지 확인하세요. 14번은 보안 감사와 기능 검증이 끝난 뒤 GitHub main, AWS Lightsail 운영 반영, 롤백 검증을 분리해서 실행할 때 사용하세요.
