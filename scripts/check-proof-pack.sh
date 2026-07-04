#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PROOF="$ROOT_DIR/docs/proofs/public-data/latest/public-data-proof-run.json"
HEALTH="$ROOT_DIR/docs/proofs/public-data/latest/public-data-health.json"
RAW="$ROOT_DIR/docs/proofs/public-data/latest/raw-response-manifest-redacted.json"
VALIDATION="$ROOT_DIR/docs/proofs/validation/synthetic-validation-summary.md"
README="$ROOT_DIR/README.md"
CHECKSUMS="$ROOT_DIR/docs/proofs/public-data/latest/checksums.txt"

fail() {
  echo "[proof-pack] FAIL: $1" >&2
  exit 1
}

[[ -f "$PROOF" ]] || fail "missing public-data-proof-run.json"
[[ -f "$HEALTH" ]] || fail "missing public-data-health.json"
[[ -f "$RAW" ]] || fail "missing raw-response-manifest-redacted.json"
[[ -f "$VALIDATION" ]] || fail "missing synthetic validation summary"
[[ -f "$ROOT_DIR/docs/proofs/ml/latest/model-card.md" ]] || fail "missing model-card.md"
[[ -f "$ROOT_DIR/docs/proofs/ml/latest/metrics.json" ]] || fail "missing metrics.json"
[[ -f "$ROOT_DIR/docs/proofs/ml/latest/shap-summary.json" ]] || fail "missing shap-summary.json"
[[ -f "$ROOT_DIR/docs/proofs/report/sample-report.md" ]] || fail "missing sample-report.md"
[[ -f "$CHECKSUMS" ]] || fail "missing checksums.txt"

python3 - "$PROOF" "$RAW" <<'PY'
import json, sys
import re

proof = json.load(open(sys.argv[1], encoding="utf-8"))
raw = json.load(open(sys.argv[2], encoding="utf-8"))
if proof.get("proofStatus") == "MISSING":
    raise SystemExit("proof run missing; export deployed server proof first")
if int(proof.get("liveProviderCount") or proof.get("live_provider_count") or 0) < 3:
    raise SystemExit("liveProviderCount < 3")
if int(proof.get("motieRelatedLiveCount") or proof.get("motie_related_live_count") or 0) < 2:
    raise SystemExit("motieRelatedLiveCount < 2")
providers = proof.get("providers") or []
hash_re = re.compile(r"^[a-fA-F0-9]{64}$")
for provider in providers:
    if provider.get("sourceType") in ("live", "cache"):
        digest = provider.get("rawResponseSha256")
        if not digest or not hash_re.match(str(digest)):
            raise SystemExit("live/cache provider missing valid rawResponseSha256")
        if provider.get("operationEndpoint") is not True:
            raise SystemExit("live/cache provider missing operationEndpoint=true")

raw_entries = raw.get("entries") or []
if not raw_entries:
    raise SystemExit("raw manifest has no entries")
for entry in raw_entries:
    digest = entry.get("rawResponseSha256")
    if not digest or not hash_re.match(str(digest)):
        raise SystemExit("raw manifest entry missing valid rawResponseSha256")
PY

python3 - "$ROOT_DIR/docs/proofs" <<'PY'
import pathlib
import re
import sys

root = pathlib.Path(sys.argv[1])
secret_re = re.compile(r'(?i)\b(serviceKey|crtfcKey|accessKey|apiKey|secret|token)\b["\']?\s*[:=]\s*["\']?([A-Za-z0-9_./+=:-]{12,})')
allowed_keys = {"apiKeyConfigured", "apiKeyStored"}
allowed_values = {"REDACTED", "true", "false", "null", "optional_print_export", "optional_browser_print_or_ci_conversion"}
for path in root.rglob("*"):
    if not path.is_file():
        continue
    text = path.read_text(encoding="utf-8", errors="ignore")
    for match in secret_re.finditer(text):
        key, value = match.group(1), match.group(2).strip('"\'')
        if key in allowed_keys or value in allowed_values:
            continue
        raise SystemExit(f"possible unredacted API key in {path}: {key}")
PY

if grep -q '실제 고객 인터뷰' "$VALIDATION"; then
  fail "synthetic validation summary must not claim real customer interviews"
fi

presentation_files=("$README")
while IFS= read -r file; do
  presentation_files+=("$file")
done < <(find "$ROOT_DIR/docs" -maxdepth 1 -type f \( -name 'presentation*.md' -o -name '*presentation*.md' \) | sort)
if grep -Eq '자동 PDF 생성|수출 성공 확률|AI가 최종 판단' "${presentation_files[@]}" 2>/dev/null; then
  fail "forbidden claim found in README/docs"
fi

grep -q 'public-data-proof-run.json' "$CHECKSUMS" || fail "checksums.txt missing public-data-proof-run.json"
grep -q 'raw-response-manifest-redacted.json' "$CHECKSUMS" || fail "checksums.txt missing raw-response-manifest-redacted.json"
grep -q 'model-card.md' "$CHECKSUMS" || fail "checksums.txt missing model-card.md"

echo "[proof-pack] PASS"
