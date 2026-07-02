#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

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
require_file ".github/workflows/ci.yml"
require_file "BUILD.md"
require_file ".env.example.presentation"
require_file "docs/public-data-field-mapping.md"
require_file "docs/judge-defense-90.md"
require_file "docs/evidence-workflow-redesign-2026-07-02.md"
require_file "docs/security-sre-performance-review-2026-07-02.md"
require_file "docs/python-worker-engine-contract-2026-07-02.md"
require_file "docs/live-public-data-smoke-checklist-2026-07-02.md"
require_file "docs/pdf-rendering-strategy-2026-07-02.md"
require_file "docs/presentation-evidence-pack-2026-07-02.md"
require_file "docs/security-csrf-rate-limit-threat-model-2026-07-03.md"
require_file "scripts/verify-fresh-db.sh"

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

grep -q 'enabled: ${PUBLIC_DATA_ENABLED:true}' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml" \
  || fail "public-data.enabled must default to true"
grep -q 'enabled: ${PUBLIC_DATA_ENABLED:true}' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml.example" \
  || fail "application.yml.example public-data.enabled must default to true"
pass "public data defaults to enabled"

if search_quiet 'MOCK_STAGES|S04_MARKETS|S05_REGULATIONS|S06_RECALLS|S07_PATENTS|S07_SUPPORTS' \
  "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js"; then
  fail "diagnosisService must not import or merge S04-S07 mock stage data"
fi
if search_quiet "import .*\\.\\./mock/" "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js" \
  "$ROOT_DIR/frontend/FE-02/src/services/productService.js"; then
  fail "presentation services must lazy-load mock data only in explicit dev mock mode"
fi
search_quiet 'MOCK_ALLOWED.*!PRESENTATION_MODE.*!viteEnv\.PROD' "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js" \
  || fail "diagnosisService must block mock fallback in production/presentation mode"
search_quiet 'MOCK_ALLOWED.*!PRESENTATION_MODE.*!viteEnv\.PROD' "$ROOT_DIR/frontend/FE-02/src/services/productService.js" \
  || fail "productService must block mock fallback in production/presentation mode"
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
search_quiet 'Markdown 메모 다운로드' "$ROOT_DIR/frontend/FE-02/src/components/diagnosis/S09Report.jsx" \
  || fail "report UI must describe the default customer artifact as Markdown, not ambiguous PDF"
require_file "frontend/FE-02/src/mock/README.md"
search_quiet 'Dev-only Fixtures' "$ROOT_DIR/frontend/FE-02/src/mock/README.md" \
  || fail "mock fixtures must be documented as dev-only"
search_quiet 'LEGACY DEMO - DO NOT JUDGE AS PRODUCTION APP' "$ROOT_DIR/GROW_GATE_Local_EX_standalone.html" \
  || fail "standalone static demo must be clearly marked as legacy and not production evidence"
if search_quiet 'AI Agent|수출 가능성 예측|성공 가능성' "$ROOT_DIR/GROW_GATE_Local_EX_standalone.html"; then
  fail "standalone legacy demo must not use overclaim wording"
fi
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
search_quiet 'SourceTrustBar' "$ROOT_DIR/frontend/FE-02/src/pages/diagnosis/DiagnosisResult.jsx" \
  || fail "diagnosis result must render SourceTrustBar near the top"
search_quiet 'Score contribution ledger' "$ROOT_DIR/frontend/FE-02/src/components/diagnosis/S10Sources.jsx" \
  || fail "S10 must render score contribution ledger"
search_quiet '/worker/diagnosis-v1' "$ROOT_DIR/grow_gate_local_ex_demo/backend/app.py" \
  || fail "Python worker diagnosis contract endpoint must exist"
search_quiet 'workerDiagnosis' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/FastApiAgentClient.java" \
  || fail "Spring FastAPI client must call the Python worker diagnosis contract"
search_quiet 'pythonWorker' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java" \
  || fail "diagnosis result must expose Python worker status"
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
grep -q 'daily-refresh:' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml" \
  || fail "public-data daily refresh settings must be configurable"
pass "daily public-data refresh pipeline is wired"

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
grep -q 'live-public-data-smoke-evidence.json' "$ROOT_DIR/docs/live-public-data-smoke-checklist-2026-07-02.md" \
  || fail "live public-data smoke checklist must document evidence output"
grep -q 'font embedding' "$ROOT_DIR/docs/pdf-rendering-strategy-2026-07-02.md" \
  || fail "PDF strategy must document Korean font embedding gate"
grep -q 'Source Trust Bar screenshot' "$ROOT_DIR/docs/presentation-evidence-pack-2026-07-02.md" \
  || fail "presentation evidence pack must require Source Trust Bar screenshot"
pass "evidence pack documents presentation env, field mapping, and judge defense"

pass "all risk gates passed"
