#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 path/to/growgate-submission.zip" >&2
  exit 64
fi

ZIP_PATH="$1"
[[ -f "$ZIP_PATH" ]] || { echo "[submission-zip] missing zip: $ZIP_PATH" >&2; exit 66; }

LISTING="$(unzip -l "$ZIP_PATH")"

fail() {
  echo "[submission-zip] FAIL: $*" >&2
  exit 1
}

echo "$LISTING" | grep -q 'docs/SYNTHETIC_DATA_POLICY.md' \
  || fail "SYNTHETIC_DATA_POLICY.md must be included"
echo "$LISTING" | grep -q 'SUBMISSION_SCOPE.md' \
  || fail "SUBMISSION_SCOPE.md must be included"
echo "$LISTING" | grep -q 'database/mysql/init/05_seed_synthetic_companies.sql' \
  || fail "synthetic seed SQL must be included"

if echo "$LISTING" | grep -E '(^|/)(node_modules|\.git|\.gradle|__pycache__)/' >/dev/null; then
  fail "zip contains local build/cache/git directory"
fi
if echo "$LISTING" | grep -E '(^|/)\.env($|[.])' >/dev/null; then
  fail "zip contains local env file"
fi
if echo "$LISTING" | grep -E 'artifacts/legacy-backups|artifacts/presentation|artifacts/submission|\.zip$' >/dev/null; then
  fail "zip contains archive/runtime artifact that should be excluded"
fi

echo "[submission-zip] PASS: $ZIP_PATH"
