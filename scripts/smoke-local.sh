#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_URL="${1:-${BASE_URL:-http://localhost:8080}}"

echo "[smoke-local] backend/public-data smoke against $BASE_URL"
"$ROOT_DIR/scripts/smoke-public-data.sh" "$BASE_URL"

FRONTEND_URL="${FRONTEND_URL:-http://localhost:5173}"
if curl -fsS -m "${SMOKE_TIMEOUT_SECONDS:-15}" "$FRONTEND_URL" >/dev/null 2>&1; then
  echo "[smoke-local] frontend: OK ($FRONTEND_URL)"
else
  echo "[smoke-local] frontend: SKIP/FAIL ($FRONTEND_URL not reachable)" >&2
fi
