#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

fail() {
  echo "[risk-gate] FAIL: $*" >&2
  exit 1
}

pass() {
  echo "[risk-gate] PASS: $*"
}

require_file() {
  [[ -f "$ROOT_DIR/$1" ]] || fail "missing required file: $1"
}

search_quiet() {
  local pattern="$1"
  shift

  if command -v rg >/dev/null 2>&1; then
    rg -n "$pattern" "$@" >/dev/null
  else
    grep -R -n -E "$pattern" "$@" >/dev/null
  fi
}

require_file "scripts/smoke-prod.sh"
require_file "scripts/verify-local.sh"
require_file "scripts/package-submission.sh"
require_file "scripts/check-submission-zip.sh"
require_file "scripts/build-proof-pack.sh"
require_file "scripts/check-proof-pack.sh"
require_file "scripts/presentation-public-data-proof.sh"
require_file ".github/workflows/ci.yml"
require_file "BUILD.md"
require_file ".env.example.presentation"
require_file "docs/public-data-field-mapping.md"
require_file "docs/judge-defense-90.md"
require_file "docs/evidence-workflow-redesign-2026-07-02.md"
require_file "docs/agent-public-data-harness-playbook-2026-07-05.md"
require_file "docs/security-sre-performance-review-2026-07-02.md"
require_file "docs/python-worker-engine-contract-2026-07-02.md"
require_file "docs/live-public-data-smoke-checklist-2026-07-02.md"
require_file "docs/pdf-rendering-strategy-2026-07-02.md"
require_file "docs/presentation-evidence-pack-2026-07-02.md"
require_file "docs/security-csrf-rate-limit-threat-model-2026-07-03.md"
require_file "AGENTS.md"
require_file "docs/contest-defense.md"
require_file "docs/ai-claim-truth-table.md"
require_file "docs/engineering/failure-log.md"
require_file "docs/SYNTHETIC_DATA_POLICY.md"
require_file "docs/demo-scenarios.md"
require_file "SUBMISSION_SCOPE.md"
require_file "database/mysql/init/03_agent_ml_regional_schema.sql"
require_file "database/mysql/init/04_seed_regional_and_registry.sql"
require_file "database/mysql/init/05_seed_synthetic_companies.sql"
require_file "database/mysql/init/06_public_api_proof_ml_validation.sql"
require_file "database/mysql/init/07_seed_synthetic_persona_validation.sql"
require_file "docs/judge-solution-pack/04_codex_prompts/00_PROMPT_ORDER.md"
require_file "docs/judge-solution-pack/04_codex_prompts/CODEX_PROMPT_13_multi_agent_public_data_harness.md"
require_file "docs/validation/README_SYNTHETIC_VALIDATION.md"
require_file "docs/validation/synthetic_validation_sessions.csv"
require_file "docs/report/REPORT_FORMAT_POLICY.md"
require_file "ml/pipelines/build_dataset.py"
require_file "ml/pipelines/train_all.py"
require_file "grow_gate_local_ex_demo/backend/synthetic_ml.py"
require_file "scripts/verify-fresh-db.sh"
require_file "backend/BE-02/src/test/resources/publicdata/kotra-market-sample-live-response.json"
require_file "backend/BE-02/src/test/resources/publicdata/safety-recall-sample-live-response.json"
require_file "backend/BE-02/src/test/resources/publicdata/kipris-fto-sample-live-response.json"
require_file "backend/BE-02/src/test/resources/publicdata/bizinfo-support-sample-live-response.json"
require_file "backend/BE-02/src/test/resources/publicdata/export-evidence-feed-sample.xml"

grep -q 'scripts/check-risk-gates.sh' "$ROOT_DIR/.github/workflows/ci.yml" \
  || fail "CI must run scripts/check-risk-gates.sh"
pass "CI runs risk gates"

grep -q 'confidence DOUBLE PRECISION' "$ROOT_DIR/database/mysql/init/01_schema.sql" \
  || fail "Docker init schema must include public_data_records.confidence"
grep -q 'source_type VARCHAR(20)' "$ROOT_DIR/database/mysql/init/01_schema.sql" \
  || fail "Docker init schema must include public_data_records.source_type"
grep -q 'JPA_DDL_AUTO=\"validate\"' "$ROOT_DIR/scripts/verify-fresh-db.sh" \
  || fail "fresh DB verifier must start backend with ddl-auto=validate"
grep -q 'COMPOSE_PROJECT_NAME' "$ROOT_DIR/scripts/verify-fresh-db.sh" \
  || fail "fresh DB verifier must isolate Docker project/volume"
grep -q 'verify-fresh-db.sh' "$ROOT_DIR/README.md" \
  || fail "README must document fresh DB verification"
pass "fresh Docker DB schema is aligned with validate startup"

grep -q 'synthetic_demo_not_real_customer' "$ROOT_DIR/docs/SYNTHETIC_DATA_POLICY.md" \
  || fail "synthetic data policy must preserve the required synthetic label"
grep -q 'synthetic_demo_not_real_customer' "$ROOT_DIR/database/mysql/init/05_seed_synthetic_companies.sql" \
  || fail "synthetic seed SQL must mark demo data as synthetic_demo_not_real_customer"
if grep -q '"sourceType":"live"\|"sourceType":"cache"\|"operationEndpoint":true\|"apiKeyConfigured":true' \
  "$ROOT_DIR/database/mysql/init/05_seed_synthetic_companies.sql"; then
  fail "synthetic seed must not masquerade as live/cache public API proof"
fi
if grep -Eq 'XGBoost|LightGBM|SHAP' "$ROOT_DIR/database/mysql/init/05_seed_synthetic_companies.sql"; then
  fail "synthetic seed must not imply production XGBoost/LightGBM/SHAP implementation"
fi
grep -q 'owner+battery@growgate.local' "$ROOT_DIR/docs/demo-scenarios.md" \
  || fail "demo scenario docs must include the synthetic owner accounts"
grep -q 'syntheticTraining.*True\|syntheticTraining.*true' "$ROOT_DIR/grow_gate_local_ex_demo/backend/synthetic_ml.py" \
  || fail "Python ML contract must mark syntheticTraining=true"
pass "synthetic judge seed policy and not-live proof guard are wired"

grep -q 'enabled: ${PUBLIC_DATA_ENABLED:true}' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml" \
  || fail "public-data.enabled must default to true"
grep -q 'enabled: ${PUBLIC_DATA_ENABLED:true}' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml.example" \
  || fail "application.yml.example public-data.enabled must default to true"
pass "public data defaults to enabled"

search_quiet 'KICOX_FACTORY_REGISTRATION_BY_REGION_INDUSTRY' \
  "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/enums/PublicDataProvider.java" \
  || fail "KICOX factory registration provider enum must be registered"
require_file "backend/BE-02/src/main/java/com/growgate/publicdata/service/KicoxFactoryRegistrationConnector.java"
require_file "backend/BE-02/src/test/java/com/growgate/publicdata/service/KicoxFactoryRegistrationConnectorTest.java"
search_quiet 'kicox-factory-registration' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml" \
  || fail "KICOX factory registration config must exist"
search_quiet 'KICOX_FACTORY_REGISTRATION_BY_REGION_INDUSTRY' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataOperationalStatusService.java" \
  || fail "KICOX factory registration must appear in public-data health"
search_quiet 'kicox_factory_registration_region_industry' "$ROOT_DIR/data/public_data_sources.json" \
  || fail "public data source catalog must include KICOX factory registration"
search_quiet 'KICOX_OPERATION_RATE|KICOX_FACTORY_REGISTRATION_BY_REGION_INDUSTRY' "$ROOT_DIR/scripts/check_public_data_providers.py" \
  || fail "public data provider smoke must include KICOX runtime providers"
search_quiet 'KICOX_OPERATION_RATE' \
  "$ROOT_DIR/docs/judge-solution-pack/04_codex_prompts/CODEX_PROMPT_13_multi_agent_public_data_harness.md" \
  || fail "multi-agent harness prompt must name KICOX operation rate runtime provider"
search_quiet 'KICOX_FACTORY_REGISTRATION_BY_REGION_INDUSTRY' \
  "$ROOT_DIR/docs/judge-solution-pack/04_codex_prompts/CODEX_PROMPT_13_multi_agent_public_data_harness.md" \
  || fail "multi-agent harness prompt must name KICOX factory registration runtime provider"
search_quiet 'llmInfluencedGate=false' "$ROOT_DIR/docs/agent-public-data-harness-playbook-2026-07-05.md" \
  || fail "agent public-data harness playbook must preserve rule-owned Gate policy"
pass "KICOX CSV/API providers and multi-agent public-data harness are wired"

if search_quiet 'MOCK_STAGES|S04_MARKETS|S05_REGULATIONS|S06_RECALLS|S07_PATENTS|S07_SUPPORTS' \
  "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js"; then
  fail "diagnosisService must not import or merge S04-S07 mock stage data"
fi
if search_quiet "import .*\\.\\./mock/" "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js" \
  "$ROOT_DIR/frontend/FE-02/src/services/productService.js"; then
  fail "presentation services must not import mock data"
fi
if [[ -d "$ROOT_DIR/frontend/FE-02/src/mock" ]]; then
  fail "frontend mock directory must be removed from the judged app"
fi
if search_quiet 'VITE_ENABLE_MOCK|MOCK_ALLOWED|loadDiagnosisMockFallback|diagnosisData|diagnosisListData|productData' \
  "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js" \
  "$ROOT_DIR/frontend/FE-02/src/services/productService.js"; then
  fail "frontend services must not contain mock fallback code"
fi
if [[ -d "$ROOT_DIR/frontend/FE-02/dist" ]] && search_quiet 'CURRENT_SESSION|KPI_DATA|diag-001|S04_MARKETS|S05_REGULATIONS|S06_RECALLS|S07_PATENTS|S07_SUPPORTS|경남 배터리테크' \
  "$ROOT_DIR/frontend/FE-02/dist"; then
  fail "presentation build artifact must not contain demo mock diagnosis data"
fi
pass "S04-S07 diagnosis service mapping is backend-driven"

if search_quiet 'XGBoost|xgboost|ML 예측|AI 진단' "$ROOT_DIR/frontend/FE-02/src"; then
  fail "main frontend must use defensible Gate/rule-scoring language, not XGBoost/AI diagnosis claims"
fi
if search_quiet '파이프라인 실행' "$ROOT_DIR/frontend/FE-02/src/components/diagnosis/S10Sources.jsx"; then
  fail "S10 must not show a non-functional pipeline execution button"
fi
search_quiet 'DecisionBrief' "$ROOT_DIR/frontend/FE-02/src/pages/diagnosis/DiagnosisResult.jsx" \
  || fail "diagnosis result must show a first-screen decision brief"
search_quiet 'buildDecisionBrief' "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js" \
  || fail "frontend must normalize top risks and next actions for non-expert users"
search_quiet '오늘 할 일' "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js" \
  || fail "decision brief must include today action bucket"
search_quiet '이번 주 할 일' "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js" \
  || fail "decision brief must include this-week action bucket"
search_quiet '외부기관 문의' "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js" \
  || fail "decision brief must include external-inquiry action bucket"
search_quiet 'fallback.*담당자 검토' "$ROOT_DIR/frontend/FE-02/src/components/diagnosis/GateDecisionHero.jsx" \
  || fail "Gate area must warn when fallback sources require owner review"
search_quiet 'Markdown 다운로드' "$ROOT_DIR/frontend/FE-02/src/components/diagnosis/S09Report.jsx" \
  || fail "report UI must describe the default customer artifact as Markdown, not ambiguous PDF"
if [[ -f "$ROOT_DIR/GROW_GATE_Local_EX_standalone.html" || -d "$ROOT_DIR/grow_gate_local_ex_demo/frontend" ]]; then
  fail "legacy standalone/static demo must be removed from the judged repository tree"
fi
if ! compgen -G "$ROOT_DIR/artifacts/legacy-backups/legacy-demo-and-mock-backup-*.zip" >/dev/null; then
  fail "legacy demo/mock backup zip must exist under artifacts/legacy-backups"
fi
grep -q 'artifacts/legacy-backups/\*' "$ROOT_DIR/scripts/package-submission.sh" \
  || fail "submission package script must exclude legacy backup archives"
grep -q 'artifacts/presentation/\*' "$ROOT_DIR/scripts/package-submission.sh" \
  || fail "submission package script must exclude presentation proof runtime artifacts"
grep -q '\*.zip' "$ROOT_DIR/scripts/package-submission.sh" \
  || fail "submission package script must exclude zip archives from the submission zip"
grep -q '\*/\.env' "$ROOT_DIR/scripts/package-submission.sh" \
  || fail "submission package script must exclude local .env files"
grep -q '\.venv-aws/\*' "$ROOT_DIR/scripts/package-submission.sh" \
  || fail "submission package script must exclude local AWS CLI virtualenv"
grep -q '\.gstack/\*' "$ROOT_DIR/scripts/package-submission.sh" \
  || fail "submission package script must exclude local gstack state"
grep -q 'growgate-submission-' "$ROOT_DIR/scripts/package-submission.sh" \
  || fail "submission package script must create a dated growgate submission zip"
search_quiet 'GateDecisionHero' "$ROOT_DIR/frontend/FE-02/src/pages/diagnosis/DiagnosisResult.jsx" \
  || fail "diagnosis result must show gate decision before detailed stages"
search_quiet 'gateDecision' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "backend result must include gateDecision"
search_quiet 'buyerRequest' "$ROOT_DIR/frontend/FE-02/src/pages/diagnosis/DiagnosisNew.jsx" \
  || fail "diagnosis input must capture buyer inquiry context"
search_quiet 'buyerRequest' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/dto/CreateDiagnosisRequest.java" \
  || fail "backend diagnosis request must accept buyer inquiry context"
search_quiet 'Before / After' "$ROOT_DIR/frontend/FE-02/src/pages/auth/Home.jsx" \
  || fail "home must explain before/after differentiation"
search_quiet 'thisWeek|beforeQuote|beforeShipment' "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js" \
  || fail "frontend roadmap mapper must use Gate action buckets"
search_quiet 'metricSummary|decisionImpact' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "public-data source entries must include metric summary and decision impact"
pass "Gate decision, buyer context, and defensible wording are wired"

search_quiet 'evidenceRetrieval' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "diagnosis result must expose evidenceRetrieval"
search_quiet 'exportKnowledgeTimeline' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "diagnosis result must expose exportKnowledgeTimeline"
search_quiet 'evidenceGuardrails' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "diagnosis result must expose evidenceGuardrails"
search_quiet 'sourceTrustSummary' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "diagnosis result must expose sourceTrustSummary"
search_quiet 'scoreContributionLedger' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "diagnosis result must expose scoreContributionLedger"
for field in sourceTrustSummary scoreContributionLedger publicDataCollectionRunId inputTrace sourceTrace llmFallbackReasons claimVerifications; do
  grep -q "$field" "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/dto/DiagnosisDetailResponse.java" \
    || fail "DiagnosisDetailResponse must preserve result field: $field"
done
for mapping in 'sourceTrustSummary(parseObjectMap' 'scoreContributionLedger(parseMapList' 'publicDataCollectionRunId(asString' 'inputTrace(parseObjectMap' 'sourceTrace(parseObjectMap' 'llmFallbackReasons(parseStringList' 'claimVerifications(parseMapList'; do
  grep -q "$mapping" "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisService.java" \
    || fail "DiagnosisService.getDetail must map: $mapping"
done
for field in ruleScore riskLevel fallbackCount finalGate llmInfluencedGate llmDecisionPolicy; do
  grep -q "$field" "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/dto/DiagnosisDetailResponse.java" \
    || fail "GateDecision DTO must expose rule-owned field: $field"
  grep -q "$field" "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
    || fail "DiagnosisAsyncProcessor must write rule-owned gate field: $field"
done
grep -q 'ClaimVerificationService' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/agent/service/ClaimVerificationService.java" \
  || fail "ClaimVerificationService must exist"
search_quiet 'source-less|Source-less' "$ROOT_DIR/AGENTS.md" "$ROOT_DIR/docs/contest-defense.md" "$ROOT_DIR/docs/ai-claim-truth-table.md" \
  || fail "AI truth docs must document source-less claim blocking"
for field in collectionEvidence operationEndpoint apiKeyConfigured endpointType fetchedAt metricSummary decisionImpact; do
  grep -q "$field" "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/dto/DiagnosisSourcesResponse.java" \
    || fail "DiagnosisSourcesResponse must expose source proof field: $field"
done
for mapping in 'collectionEvidence(s.get' 'operationEndpoint(asBoolean' 'apiKeyConfigured(asBoolean' 'metricSummary(asString' 'decisionImpact(asString'; do
  grep -q "$mapping" "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisService.java" \
    || fail "DiagnosisService.getSources must map: $mapping"
done
search_quiet 'getDetailPreservesEvidenceContractFieldsFromResultJson|getSourcesPreservesCollectionEvidenceForPublicApiProof' "$ROOT_DIR/backend/BE-02/src/test/java/com/growgate/diagnosis/service/DiagnosisServiceTest.java" \
  || fail "DiagnosisService evidence contract regression tests must exist"
search_quiet 'buildSourceTrustSummaryFromSources|operationEndpointLiveCount|apiKeyConfiguredLiveCount' "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js" \
  || fail "frontend mapper must restore source trust summary from /sources proof fields"
search_quiet 'sourceRestored|operationEndpointLiveCount|apiKeyConfiguredLiveCount' "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.mapper.test.js" \
  || fail "frontend mapper contract test must cover source proof restoration"
search_quiet 'liveSuccessRate|providerHealth|lastCollectedAt|sourcePeriod' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "sourceTrustSummary must expose freshness, period, live success rate, and provider health"
search_quiet 'SourceTrustBar' "$ROOT_DIR/frontend/FE-02/src/pages/diagnosis/DiagnosisResult.jsx" \
  || fail "diagnosis result must render SourceTrustBar near the top"
search_quiet 'Score contribution.*waterfall|waterfall.*ledger' "$ROOT_DIR/frontend/FE-02/src/components/diagnosis/S10Sources.jsx" \
  || fail "S10 must render score contribution ledger"
search_quiet 'collectPublicData' "$ROOT_DIR/frontend/FE-02/src/pages/diagnosis/DiagnosisResult.jsx" \
  || fail "public data retry button must call the real collect API"
search_quiet 'failureReason|Provider health' "$ROOT_DIR/frontend/FE-02/src/components/diagnosis/S10Sources.jsx" \
  || fail "S10 must show provider failure reason and health"
search_quiet '/worker/diagnosis-v1' "$ROOT_DIR/grow_gate_local_ex_demo/backend/app.py" \
  || fail "Python worker diagnosis contract endpoint must exist"
search_quiet 'workerDiagnosis' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/FastApiAgentClient.java" \
  || fail "Spring FastAPI client must call the Python worker diagnosis contract"
search_quiet 'pythonWorker' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "diagnosis result must expose Python worker status"
search_quiet 'pythonAgenticLoop' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "diagnosis result must expose Python worker agentic loop details"
search_quiet 'budgetGuard|sourceBoundVerification|claimVerification|retryPlan' "$ROOT_DIR/grow_gate_local_ex_demo/backend/worker_engine.py" \
  || fail "Python worker must expose CSO/SRE/performance guardrails"
search_quiet 'normalizedMetrics' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "diagnosis result must expose normalized metrics from worker or Spring fallback"
search_quiet 'compiledWiki' "$ROOT_DIR/grow_gate_local_ex_demo/backend/worker_engine.py" \
  || fail "Python worker must compile wiki/log/error-book artifacts"
search_quiet 'compiledWiki' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "Spring diagnosis result must preserve compiled wiki artifacts"
search_quiet 'source-bound-evidence-search' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/agent/service/EvidenceContextBuilder.java" \
  || fail "evidence retrieval must use defensible source-bound naming"
search_quiet 'Export knowledge timeline' "$ROOT_DIR/frontend/FE-02/src/components/diagnosis/S10Sources.jsx" \
  || fail "S10 must render export knowledge timeline"
search_quiet 'LlmClaimVerifier' "$ROOT_DIR/backend/BE-02/src/test/java/com/growgate/agent/service/LlmClaimVerifierTest.java" \
  || fail "LLM claim verifier tests must exist"
search_quiet 'external_source_documents|evidence_claims|temporal_events|knowledge_pages|agent_trace_steps' "$ROOT_DIR/backend/BE-02/src/main/resources/db/01_schema.sql" \
  || fail "external evidence schema tables must exist"
if search_quiet 'AI 지식화|추론 Agent' "$ROOT_DIR/frontend/FE-02/src" "$ROOT_DIR/backend/BE-02/src/main/java"; then
  fail "main app must avoid overclaiming AI/Agent wording in knowledge workflow"
fi
pass "evidence workflow exposes retrieval, timeline, guardrails, and defensible UI wording"

grep -q 'max-daily-requests-per-user' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml" \
  || fail "LLM agent must have per-user daily budget controls"
grep -q 'max-daily-requests-global' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml" \
  || fail "LLM agent must have global daily budget controls"
grep -q 'max-prompt-chars' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml" \
  || fail "LLM agent must cap prompt context size"
grep -q 'max-output-tokens' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml" \
  || fail "LLM provider calls must cap output tokens"
search_quiet 'budget guard: user daily LLM quota reached' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/agent/service/AgentInferenceService.java" \
  || fail "LLM agent service must stop calls when user quota is reached"
search_quiet 'maxOutputTokens|max_tokens' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/agent/service" \
  || fail "LLM provider request bodies must include output-token caps"
if search_quiet 'VITE_.*(KEY|SECRET|TOKEN)|OPENROUTER|GEMINI|API_KEY|sk-or-v1-[A-Za-z0-9]{20,}|AQ\.Ab8[A-Za-z0-9_-]{12,}' \
  "$ROOT_DIR/frontend/FE-02/src"; then
  fail "frontend source must not contain LLM/API keys or public VITE secret variables"
fi
if search_quiet 'sk-or-v1-[A-Za-z0-9]{20,}|AQ\.Ab8[A-Za-z0-9_-]{12,}' \
  "$ROOT_DIR/backend/BE-02/src/main/resources" "$ROOT_DIR/README.md" "$ROOT_DIR/docs" "$ROOT_DIR/scripts"; then
  fail "repository files must not contain raw OpenRouter/Gemini token values"
fi
pass "LLM calls are budget-capped and raw keys are blocked from frontend/docs"

search_quiet 'reportError|try \{|catch \(err\)' "$ROOT_DIR/frontend/FE-02/src/pages/diagnosis/DiagnosisResult.jsx" \
  || fail "DiagnosisResult must surface report download errors"
if search_quiet 'report\.pdf|getReportPdf|minimalPdf|%PDF-' \
  "$ROOT_DIR/backend/BE-02/src/main/java" "$ROOT_DIR/scripts/smoke-prod.sh" "$ROOT_DIR/scripts/setup-demo-scenario.sh" "$ROOT_DIR/README.md" "$ROOT_DIR/docs/pdf-rendering-strategy-2026-07-02.md" "$ROOT_DIR/docs/presentation-evidence-pack-2026-07-02.md" "$ROOT_DIR/docs/judge-question-defense-2026-07-01.md" "$ROOT_DIR/docs/demo-scenario-pmgrow-battery-2026-07-02.md"; then
  fail "presentation path must not expose unverified PDF report claims"
fi
grep -q 'Markdown report payload' "$ROOT_DIR/docs/pdf-rendering-strategy-2026-07-02.md" \
  || fail "PDF strategy must explain Markdown-only presentation report policy"
pass "report download has visible error handling"

if search_quiet 'localStorage\.setItem\((LEGACY_TOKEN_KEY|["'\'']gg_access_token)' "$ROOT_DIR/frontend/FE-02/src"; then
  fail "access token must not be written to localStorage"
fi
search_quiet 'httpOnly\(true\)' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/auth/controller/AuthController.java" \
  || fail "auth cookie must be HttpOnly"
search_quiet 'gg_access_token' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/security/JwtAuthenticationFilter.java" \
  || fail "JWT filter must accept gg_access_token cookie"
search_quiet 'X-GG-CSRF|gg_csrf_token' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/security/CsrfCookieFilter.java" \
  || fail "cookie auth must have CSRF double-submit filter"
search_quiet 'X-GG-CSRF|gg_csrf_token' "$ROOT_DIR/frontend/FE-02/src/services/apiClient.js" \
  || fail "frontend API client must send CSRF header from cookie"
grep -q 'VITE_API_BASE_URL' "$ROOT_DIR/frontend/FE-02/src/services/apiClient.js" \
  || fail "frontend API client must support presentation env VITE_API_BASE_URL"
grep -q 'CsrfCookieFilterTest' "$ROOT_DIR/backend/BE-02/src/test/java/com/growgate/security/CsrfCookieFilterTest.java" \
  || fail "CSRF double-submit filter must have tests"
grep -q 'LoginAttemptRateLimiterTest' "$ROOT_DIR/backend/BE-02/src/test/java/com/growgate/auth/service/LoginAttemptRateLimiterTest.java" \
  || fail "login rate limiter must have tests"
grep -q 'in-memory for the single-instance Lightsail submission server' "$ROOT_DIR/docs/security-csrf-rate-limit-threat-model-2026-07-03.md" \
  || fail "rate limit scope must be documented as single-instance in-memory"
pass "auth uses HttpOnly cookie path and avoids localStorage token persistence"

search_quiet 'termsAccepted' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/auth/dto/SignupRequest.java" \
  || fail "signup request must include required terms consent"
search_quiet 'privacyAccepted' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/auth/dto/SignupRequest.java" \
  || fail "signup request must include required privacy consent"
search_quiet 'UserConsentRecord' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/auth" \
  || fail "signup consent evidence must be persisted"
grep -q 'user_consent_records' "$ROOT_DIR/backend/BE-02/src/main/resources/db/01_schema.sql" \
  || fail "schema must include user_consent_records"
grep -q 'user_consent_records' "$ROOT_DIR/database/mysql/migrations/2026-07-02_user_consent_records.sql" \
  || fail "deployable MySQL migration for user_consent_records must exist"
search_quiet 'ConsentChecklist' "$ROOT_DIR/frontend/FE-02/src/pages/auth" \
  || fail "signup UI must require explicit consent selections"
grep -q 'termsAccepted' "$ROOT_DIR/scripts/smoke-prod.sh" \
  || fail "production smoke signup must include consent fields"
grep -q 'termsAccepted' "$ROOT_DIR/scripts/setup-demo-scenario.sh" \
  || fail "demo scenario signup must include consent fields"
grep -q 'GOOGLE_SSO_ENABLED=false' "$ROOT_DIR/.env.example.presentation" \
  || fail "Google SSO must be explicitly disabled by default in presentation env"
grep -q 'Legal/Compliance Readiness' "$ROOT_DIR/docs/legal-compliance-readiness-2026-07-02.md" \
  || fail "legal/compliance readiness document must exist"
pass "signup consent and legal/compliance readiness are documented and gated"

if search_quiet 'findTop20ByOrderByStartedAtDesc|findTop50ByOrderByFetchedAtDesc|findTop50ByHsCodeAndTargetCountryOrderByFetchedAtDesc' \
  "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataCollectionService.java"; then
  fail "public-data service must not use global recent run/record queries"
fi
search_quiet 'findTop20ByRequestedBy_IdInOrderByStartedAtDesc' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataCollectionService.java" \
  || fail "public-data runs must be scoped by requestedBy"
search_quiet 'findTop50ByRun_RequestedBy_IdIn' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataCollectionService.java" \
  || fail "public-data records must be scoped by requestedBy"
pass "public data APIs use user/company scoped queries"

search_quiet 'PublicDataDailyRefreshService' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataDailyRefreshService.java" \
  || fail "daily public-data refresh service must exist"
search_quiet '@Scheduled' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataDailyRefreshService.java" \
  || fail "daily public-data refresh must run on a schedule"
search_quiet '/daily-refresh|refreshDaily' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/controller/PublicDataController.java" \
  || fail "public-data controller must expose manual daily refresh endpoint"
search_quiet '/health|getHealth' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/controller/PublicDataController.java" \
  || fail "public-data controller must expose health endpoint for live MOTIE evidence"
search_quiet 'motieVerifiedOperationLiveCount|motieRelatedScoreContribution|motieLiveEvidenceReady' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataCollectionService.java" \
  || fail "public-data health must separate MOTIE live operation evidence and contribution"
grep -q 'daily-refresh:' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml" \
  || fail "public-data daily refresh settings must be configurable"
pass "daily public-data refresh pipeline is wired"

search_quiet 'ProviderSchemaNormalizationTest' "$ROOT_DIR/backend/BE-02/src/test/java/com/growgate/publicdata/service/ProviderSchemaNormalizationTest.java" \
  || fail "provider schema normalization tests must exist"
search_quiet 'fieldMapping' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service" \
  || fail "provider connectors must expose field mapping metadata"
search_quiet 'guardedScoreContribution' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataCollectionService.java" \
  || fail "public data storage must clamp fallback contribution"
search_quiet 'ClampsFallbackPositiveContribution' "$ROOT_DIR/backend/BE-02/src/test/java/com/growgate/publicdata/service/PublicDataCollectionServiceTest.java" \
  || fail "fallback positive contribution guard must have a regression test"
grep -q 'seed-demo-not-live' "$ROOT_DIR/database/mysql/init/02_seed.sql" \
  || fail "seed diagnosis result must be watermarked as non-live demo data"
pass "provider schema normalization and fallback contribution guards are tested"

search_quiet 'MotieWebCrawlConnector' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/MotieWebCrawlConnector.java" \
  || fail "safe public web crawl connector must exist"
search_quiet 'ConditionalOnProperty' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/MotieWebCrawlConnector.java" \
  || fail "crawler must be disabled unless explicitly enabled"
search_quiet 'allowed-hosts' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml" \
  || fail "crawler must use configured allowlist hosts"
search_quiet 'EMAIL_REDACTED|PHONE_REDACTED|REDACTED' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/MotieWebCrawlConnector.java" \
  || fail "crawler must mask secrets and personal contact patterns"
search_quiet 'sourceType === '\''crawl'\''' "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js" \
  || fail "frontend source mapper must label crawl sourceType separately"
pass "safe public web crawl path is opt-in, allowlisted, masked, and source-typed"

search_quiet 'ExportEvidenceFeedConnector' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/ExportEvidenceFeedConnector.java" \
  || fail "export news/report feed connector must exist"
grep -q 'export-evidence:' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml" \
  || fail "export evidence feed connector must be configurable"
search_quiet 'sourceType === '\''external_feed'\''' "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js" \
  || fail "frontend source mapper must label external feed sourceType separately"
grep -q 'PUBLIC_DATA_EXPORT_EVIDENCE_URLS' "$ROOT_DIR/.env.example.presentation" \
  || fail "presentation env must document export evidence feed URLs"
pass "export news/report RSS/API feed collection is opt-in and source-typed"

grep -q 'SMOKE_CORS_ORIGIN' "$ROOT_DIR/scripts/smoke-prod.sh" \
  || fail "smoke-prod must support CORS preflight checks"
grep -q 'SMOKE_REQUIRE_LIVE_PUBLIC_DATA' "$ROOT_DIR/scripts/smoke-prod.sh" \
  || fail "smoke-prod must support live public-data enforcement"
grep -q 'live-public-data-smoke-evidence.json' "$ROOT_DIR/scripts/smoke-prod.sh" \
  || fail "smoke-prod must capture live public-data evidence summary"
grep -q 'diagnosisId=.*collectionRunId=.*liveCount' "$ROOT_DIR/scripts/smoke-prod.sh" \
  || fail "smoke-prod must print diagnosisId, collectionRunId, and live/cache/fallback counts"
grep -q 'gg_access_token=.*HttpOnly' "$ROOT_DIR/scripts/smoke-prod.sh" \
  || fail "smoke-prod must verify HttpOnly cookie login"
grep -q 'stageCards.s04-s07=present' "$ROOT_DIR/scripts/smoke-prod.sh" \
  || fail "smoke-prod must verify backend stageCards s04-s07"
grep -q 'X-GG-CSRF' "$ROOT_DIR/scripts/smoke-prod.sh" \
  || fail "smoke-prod must send CSRF header when csrf cookie is present"
grep -q 'operationEndpointLiveCount' "$ROOT_DIR/scripts/presentation-public-data-proof.sh" \
  || fail "presentation public-data proof script must capture operation endpoint evidence"
grep -q 'apiKeyConfiguredLiveCount' "$ROOT_DIR/scripts/presentation-public-data-proof.sh" \
  || fail "presentation public-data proof script must capture API key configured evidence"
grep -q 'officialOperationApiEvidenceCount' "$ROOT_DIR/scripts/presentation-public-data-proof.sh" \
  || fail "presentation public-data proof script must separate official operation API evidence"
pass "production smoke covers API/CORS/auth/stage/public-data risks"

grep -q 'VITE_APP_MODE=presentation' "$ROOT_DIR/.env.example.presentation" \
  || fail "presentation env must set VITE_APP_MODE=presentation"
grep -q 'REQUIRE_LIVE_PUBLIC_DATA=true' "$ROOT_DIR/.env.example.presentation" \
  || fail "presentation env must document live public-data enforcement"
grep -q 'fallback source는 positive contribution' "$ROOT_DIR/docs/public-data-field-mapping.md" \
  || fail "public-data field mapping must document fallback contribution policy"
grep -q '90-Point Ready Gates' "$ROOT_DIR/docs/judge-defense-90.md" \
  || fail "judge defense doc must include 90-point ready gates"
grep -q 'Evidence Workflow Redesign' "$ROOT_DIR/docs/evidence-workflow-redesign-2026-07-02.md" \
  || fail "evidence workflow redesign doc must describe the repositioning"
grep -q 'Security/SRE/Performance Persona Review' "$ROOT_DIR/docs/security-sre-performance-review-2026-07-02.md" \
  || fail "security/SRE/performance persona review must be documented"
grep -q 'Python Worker Engine Contract' "$ROOT_DIR/docs/python-worker-engine-contract-2026-07-02.md" \
  || fail "Python worker engine contract must be documented"
grep -q 'python-planning-tool-verification-retry' "$ROOT_DIR/docs/python-worker-engine-contract-2026-07-02.md" \
  || fail "Python worker contract must document bounded planning/tool/verification/retry loop"
grep -q 'live-public-data-smoke-evidence.json' "$ROOT_DIR/docs/live-public-data-smoke-checklist-2026-07-02.md" \
  || fail "live public-data smoke checklist must document evidence output"
grep -q 'font embedding' "$ROOT_DIR/docs/pdf-rendering-strategy-2026-07-02.md" \
  || fail "PDF strategy must document Korean font embedding gate"
grep -q 'Source Trust Bar screenshot' "$ROOT_DIR/docs/presentation-evidence-pack-2026-07-02.md" \
  || fail "presentation evidence pack must require Source Trust Bar screenshot"
pass "evidence pack documents presentation env, field mapping, and judge defense"

search_quiet 'public_api_proof_runs|public_api_raw_responses|public_data_score_contributions' "$ROOT_DIR/database/mysql/init/06_public_api_proof_ml_validation.sql" \
  || fail "public API proof schema must include proof run, raw response, and score contribution tables"
search_quiet 'rawResponseSha256|schemaValidationStatus|operationEndpointLiveCount|apiKeyConfiguredLiveCount' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataProofRunService.java" \
  || fail "PublicDataProofRunService must expose raw hash and operation/API key proof counts"
search_quiet '/api/admin/public-data/proof-runs|/api/diagnoses/.*/public-data-waterfall' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/controller/PublicDataProofController.java" \
  || fail "public-data proof controller must expose admin proof run and diagnosis waterfall endpoints"
search_quiet 'Public Data Proof|proofRunId|rawResponseSha256|schemaValidationStatus' "$ROOT_DIR/frontend/FE-02/src/components/diagnosis/S10Sources.jsx" \
  || fail "S10 must expose judge-facing public data proof fields"
pass "public-data proof run API and S10 proof surface are wired"

grep -q 'not real customer interviews' "$ROOT_DIR/docs/validation/README_SYNTHETIC_VALIDATION.md" \
  || fail "synthetic validation README must explicitly say it is not real customer interviews"
grep -q 'synthetic_disclaimer' "$ROOT_DIR/docs/validation/synthetic_validation_sessions.csv" \
  || fail "synthetic validation sessions must keep synthetic disclaimer"
grep -q ',true$' "$ROOT_DIR/docs/validation/synthetic_validation_sessions.csv" \
  || fail "synthetic validation sessions must mark rows as synthetic"
grep -q 'estimateType.*synthetic_persona_assumption' "$ROOT_DIR/frontend/FE-02/src/fixtures/syntheticPersonaValidationSummary.json" \
  || fail "frontend validation fixture must mark synthetic persona assumption"
pass "synthetic persona validation is separated from real customer evidence"

search_quiet 'synthetic_rule_assisted_not_real_customer_outcome|permutation_fallback_not_shap|final Gate를 override' "$ROOT_DIR/ml/pipelines/train_all.py" \
  || fail "ML pipeline must label synthetic data, non-SHAP fallback, and Gate ownership limits"
search_quiet '/api/ml|syntheticTraining|notRealCustomerOutcome' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/ml/controller/MlModelController.java" \
  || fail "backend must expose latest ML model metadata with limitations"
grep -q 'Official report output is Markdown' "$ROOT_DIR/docs/report/REPORT_FORMAT_POLICY.md" \
  || fail "report policy must make Markdown the official output"
if grep -Eq '자동 PDF 생성|수출 성공 확률|AI가 최종 판단' "$ROOT_DIR/README.md" "$ROOT_DIR/docs/report/REPORT_FORMAT_POLICY.md"; then
  fail "README/report policy must not include forbidden overclaims"
fi
pass "ML artifact and Markdown report truth contracts are wired"

search_quiet 'regionalIndustryProfile|buildRegionalIndustryProfile|지역 주력산업' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "diagnosis processor must score/trace regional industry matching"
search_quiet 'buyerRequestTrace|buildBuyerRequestTrace|USER-BUYER-REQUEST' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "diagnosis processor must trace buyer request impact"
search_quiet 'region: payload.region|industry: payload.industry' "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js" \
  || fail "frontend diagnosis request must send region and industry"
search_quiet '기업 지역|지역 업종' "$ROOT_DIR/frontend/FE-02/src/pages/diagnosis/DiagnosisNew.jsx" \
  || fail "diagnosis input must capture regional manufacturing context"
pass "regional industry and buyer-request trace are wired"

(cd "$ROOT_DIR/frontend/FE-02" && node src/services/diagnosisService.mapper.test.js) \
  || fail "frontend diagnosis mapper contract test failed"
pass "frontend diagnosis mapper contract test passed"

pass "all risk gates passed"
