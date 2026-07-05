#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend/BE-02"
FRONTEND_DIR="$ROOT_DIR/frontend/FE-02"
BASE_URL="${BASE_URL:-http://localhost:8080}"
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

step() {
  printf '[%s/5] %s: ' "$1" "$2"
}

ok() {
  echo "OK"
}

fail() {
  echo "FAIL"
  echo "[verify] $1" >&2
  exit "${2:-1}"
}

step 1 "Java check"
if [[ -n "${JAVA_HOME:-}" ]]; then
  [[ -x "$JAVA_HOME/bin/java" ]] || fail "JAVA_HOME is set but JAVA_HOME/bin/java is not executable: $JAVA_HOME" 127
  "$JAVA_HOME/bin/java" -version >/dev/null 2>&1 || fail "JAVA_HOME points to a Java install that cannot run: $JAVA_HOME" 127
elif command -v java >/dev/null 2>&1; then
  java -version >/dev/null 2>&1 || fail "java exists but java -version failed. Install JDK 17+ and retry." 127
else
  fail "JDK not found. Install JDK 17+ or set JAVA_HOME to a valid JDK path." 127
fi
ok

step 2 "Backend test"
cd "$BACKEND_DIR"
if [[ -x ./gradlew ]]; then
  GRADLE_CMD=(./gradlew)
elif command -v gradle >/dev/null 2>&1; then
  GRADLE_CMD=(gradle)
else
  fail "Gradle wrapper is missing and gradle is not on PATH. Restore backend/BE-02/gradlew or install Gradle." 127
fi
if ! GRADLE_USER_HOME="${GRADLE_USER_HOME:-$ROOT_DIR/.gradle-home}" "${GRADLE_CMD[@]}" test --no-daemon; then
  fail "Backend test failed. If this stopped while downloading Gradle, check network access to services.gradle.org and Maven Central." 1
fi
ok

step 3 "Frontend test"
cd "$FRONTEND_DIR"
NPM_BIN="${NPM_BIN:-$(command -v npm || true)}"
[[ -n "$NPM_BIN" ]] || fail "npm not found. Install Node.js 20+ or set NPM_BIN=/path/to/npm." 127
if [[ -d node_modules ]]; then
  "$NPM_BIN" run test:mappers
  "$NPM_BIN" run build
else
  "$NPM_BIN" ci
  "$NPM_BIN" run test:mappers
  "$NPM_BIN" run build
fi
ok

step 4 "Public data smoke"
cd "$ROOT_DIR"
if [[ "${VERIFY_RUN_HTTP_SMOKE:-false}" == "true" ]]; then
  "$ROOT_DIR/scripts/smoke-public-data.sh" "$BASE_URL" >/dev/null
else
  "$ROOT_DIR/scripts/check_public_data_providers.py" >/dev/null
fi
ok

step 5 "Health check"
if curl -fsS "$BASE_URL/health" >/dev/null 2>&1; then
  ok
elif [[ "${VERIFY_REQUIRE_HEALTH:-false}" == "true" ]]; then
  fail "Health endpoint is not reachable at $BASE_URL/health. Start the app with docker compose up --build or set BASE_URL." 1
else
  echo "SKIP (set VERIFY_REQUIRE_HEALTH=true after starting the app)"
fi
