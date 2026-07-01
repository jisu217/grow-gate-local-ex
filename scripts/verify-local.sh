#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

echo "[verify] backend tests"
cd "$ROOT_DIR/backend/BE-02"
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}" GRADLE_USER_HOME="${GRADLE_USER_HOME:-.gradle-home}" ./gradlew test --no-daemon

echo "[verify] risk gates"
cd "$ROOT_DIR"
"$ROOT_DIR/scripts/check-risk-gates.sh"

echo "[verify] frontend build"
cd "$ROOT_DIR/frontend/FE-02"
NPM_BIN="${NPM_BIN:-$(command -v npm || true)}"
if [[ -z "$NPM_BIN" ]]; then
  echo "[verify] npm not found. Install Node.js 20 or set NPM_BIN=/path/to/npm." >&2
  exit 127
fi
if [[ -d node_modules ]]; then
  "$NPM_BIN" run build
else
  "$NPM_BIN" ci
  "$NPM_BIN" run build
fi

echo "[verify] done"
