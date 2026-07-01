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

grep -q 'scripts/check-risk-gates.sh' "$ROOT_DIR/.github/workflows/ci.yml" \
  || fail "CI must run scripts/check-risk-gates.sh"
pass "CI runs risk gates"

grep -q 'enabled: ${PUBLIC_DATA_ENABLED:true}' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml" \
  || fail "public-data.enabled must default to true"
grep -q 'enabled: ${PUBLIC_DATA_ENABLED:true}' "$ROOT_DIR/backend/BE-02/src/main/resources/application.yml.example" \
  || fail "application.yml.example public-data.enabled must default to true"
pass "public data defaults to enabled"

if search_quiet 'MOCK_STAGES|S04_MARKETS|S05_REGULATIONS|S06_RECALLS|S07_PATENTS|S07_SUPPORTS' \
  "$ROOT_DIR/frontend/FE-02/src/services/diagnosisService.js"; then
  fail "diagnosisService must not import or merge S04-S07 mock stage data"
fi
pass "S04-S07 diagnosis service mapping is backend-driven"

search_quiet 'reportError|try \{|catch \(err\)' "$ROOT_DIR/frontend/FE-02/src/pages/diagnosis/DiagnosisResult.jsx" \
  || fail "DiagnosisResult must surface report download errors"
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
pass "auth uses HttpOnly cookie path and avoids localStorage token persistence"

if search_quiet 'findTop20ByOrderByStartedAtDesc|findTop50ByOrderByFetchedAtDesc|findTop50ByHsCodeAndTargetCountryOrderByFetchedAtDesc' \
  "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataCollectionService.java"; then
  fail "public-data service must not use global recent run/record queries"
fi
search_quiet 'findTop20ByRequestedBy_IdInOrderByStartedAtDesc' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataCollectionService.java" \
  || fail "public-data runs must be scoped by requestedBy"
search_quiet 'findTop50ByRun_RequestedBy_IdIn' "$ROOT_DIR/backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataCollectionService.java" \
  || fail "public-data records must be scoped by requestedBy"
pass "public data APIs use user/company scoped queries"

grep -q 'SMOKE_CORS_ORIGIN' "$ROOT_DIR/scripts/smoke-prod.sh" \
  || fail "smoke-prod must support CORS preflight checks"
grep -q 'SMOKE_REQUIRE_LIVE_PUBLIC_DATA' "$ROOT_DIR/scripts/smoke-prod.sh" \
  || fail "smoke-prod must support live public-data enforcement"
grep -q 'gg_access_token=.*HttpOnly' "$ROOT_DIR/scripts/smoke-prod.sh" \
  || fail "smoke-prod must verify HttpOnly cookie login"
grep -q 'stageCards.s04-s07=present' "$ROOT_DIR/scripts/smoke-prod.sh" \
  || fail "smoke-prod must verify backend stageCards s04-s07"
grep -q 'X-GG-CSRF' "$ROOT_DIR/scripts/smoke-prod.sh" \
  || fail "smoke-prod must send CSRF header when csrf cookie is present"
pass "production smoke covers API/CORS/auth/stage/public-data risks"

pass "all risk gates passed"
