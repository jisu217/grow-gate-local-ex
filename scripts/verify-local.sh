#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "[verify] backend tests"
cd "$ROOT_DIR/backend/BE-02"
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}" GRADLE_USER_HOME="${GRADLE_USER_HOME:-.gradle-home}" ./gradlew test --no-daemon

echo "[verify] frontend build"
cd "$ROOT_DIR/frontend/FE-02"
if [[ -d node_modules ]]; then
  npm run build
else
  npm ci
  npm run build
fi

echo "[verify] done"
