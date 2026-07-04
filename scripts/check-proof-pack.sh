#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PROOF="$ROOT_DIR/docs/proofs/public-data/latest/public-data-proof-run.json"
HEALTH="$ROOT_DIR/docs/proofs/public-data/latest/public-data-health.json"
RAW="$ROOT_DIR/docs/proofs/public-data/latest/raw-response-manifest-redacted.json"
VALIDATION="$ROOT_DIR/docs/proofs/validation/synthetic-validation-summary.md"
README="$ROOT_DIR/README.md"

fail() {
  echo "[proof-pack] FAIL: $1" >&2
  exit 1
}

[[ -f "$PROOF" ]] || fail "missing public-data-proof-run.json"
[[ -f "$HEALTH" ]] || fail "missing public-data-health.json"
[[ -f "$RAW" ]] || fail "missing raw-response-manifest-redacted.json"
[[ -f "$VALIDATION" ]] || fail "missing synthetic validation summary"

python3 - "$PROOF" <<'PY'
import json, sys
proof = json.load(open(sys.argv[1], encoding="utf-8"))
if proof.get("proofStatus") == "MISSING":
    raise SystemExit("proof run missing; export deployed server proof first")
if int(proof.get("liveProviderCount") or proof.get("live_provider_count") or 0) < 3:
    raise SystemExit("liveProviderCount < 3")
if int(proof.get("motieRelatedLiveCount") or proof.get("motie_related_live_count") or 0) < 2:
    raise SystemExit("motieRelatedLiveCount < 2")
providers = proof.get("providers") or []
if any((p.get("sourceType") in ("live", "cache")) and not p.get("rawResponseSha256") for p in providers):
    raise SystemExit("live/cache provider missing rawResponseSha256")
PY

if grep -Eiq '(serviceKey|crtfcKey|accessKey|apiKey)=([^R][^E][^D][^A][^C][^T][^E][^D][^&]*)' "$RAW"; then
  fail "raw manifest may contain an unredacted API key"
fi
if grep -q '실제 고객 인터뷰' "$VALIDATION"; then
  fail "synthetic validation summary must not claim real customer interviews"
fi
if grep -Eq '자동 PDF 생성|수출 성공 확률|AI가 최종 판단' "$README" "$ROOT_DIR/docs"/*.md 2>/dev/null; then
  fail "forbidden claim found in README/docs"
fi
echo "[proof-pack] PASS"
