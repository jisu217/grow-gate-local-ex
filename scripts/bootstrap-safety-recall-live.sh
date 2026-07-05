#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
LOGIN_EMAIL="${GROWGATE_BOOTSTRAP_EMAIL:-}"
LOGIN_PASSWORD="${GROWGATE_BOOTSTRAP_PASSWORD:-}"
HS_CODE="${SAFETY_RECALL_BOOTSTRAP_HS_CODE:-850760}"
TARGET_COUNTRY="${SAFETY_RECALL_BOOTSTRAP_COUNTRY:-DE}"
PRODUCT_NAME="${SAFETY_RECALL_BOOTSTRAP_PRODUCT:-리튬이온 배터리팩}"
WORK_DIR="${WORK_DIR:-/tmp/growgate-safety-recall-bootstrap}"

if [[ -z "$LOGIN_EMAIL" || -z "$LOGIN_PASSWORD" ]]; then
  echo "GROWGATE_BOOTSTRAP_EMAIL and GROWGATE_BOOTSTRAP_PASSWORD are required." >&2
  exit 2
fi

mkdir -p "$WORK_DIR"
COOKIE_JAR="$WORK_DIR/cookies.txt"
LOGIN_JSON="$WORK_DIR/login.json"
COLLECT_JSON="$WORK_DIR/collect.json"
CREATE_PROOF_JSON="$WORK_DIR/create-proof.json"
PROOF_JSON="$WORK_DIR/latest-proof.json"

printf '{"email":"%s","password":"%s"}' "$LOGIN_EMAIL" "$LOGIN_PASSWORD" > "$LOGIN_JSON"

curl -sS -c "$COOKIE_JAR" \
  -H 'Content-Type: application/json' \
  -X POST "$BASE_URL/api/auth/login" \
  --data-binary "@$LOGIN_JSON" > "$WORK_DIR/login-response.json"

printf '{"hsCode":"%s","targetCountry":"%s","productName":"%s"}' "$HS_CODE" "$TARGET_COUNTRY" "$PRODUCT_NAME" > "$WORK_DIR/collect-request.json"

curl -sS -b "$COOKIE_JAR" \
  -H 'Content-Type: application/json' \
  -X POST "$BASE_URL/api/public-data/collect" \
  --data-binary "@$WORK_DIR/collect-request.json" > "$COLLECT_JSON"

curl -sS -b "$COOKIE_JAR" \
  -X POST "$BASE_URL/api/admin/public-data/proof-runs?triggerType=safety-recall-bootstrap" > "$CREATE_PROOF_JSON"

curl -sS -b "$COOKIE_JAR" \
  "$BASE_URL/api/public-data/proof-runs/latest" > "$PROOF_JSON"

python3 - "$COLLECT_JSON" "$PROOF_JSON" <<'PY'
import json
import sys

collect = json.load(open(sys.argv[1]))
proof = json.load(open(sys.argv[2]))
providers = proof.get("providers") or []
safety = [p for p in providers if p.get("providerCode") == "SAFETY_RECALL"]

print("[safety-recall-bootstrap] collectionRunId=", collect.get("runId") or collect.get("collectionRunId"))
if not safety:
    print("[safety-recall-bootstrap] SAFETY_RECALL proof row not found yet. Create a proof run after collection if needed.")
    sys.exit(0)

row = safety[0]
print("[safety-recall-bootstrap] provider=SAFETY_RECALL")
print("[safety-recall-bootstrap] source=", row.get("sourceType"))
print("[safety-recall-bootstrap] apiProof=endpoint", "yes" if row.get("operationEndpoint") else "no",
      "/ key", "configured" if row.get("apiKeyConfigured") else "missing")
print("[safety-recall-bootstrap] records=", row.get("recordCount"))
print("[safety-recall-bootstrap] rawResponseSha256=", row.get("rawResponseSha256"))
print("[safety-recall-bootstrap] schemaValidation=", row.get("schemaValidationStatus"))
print("[safety-recall-bootstrap] reason=", row.get("fallbackReason") or "-")
PY

echo "[safety-recall-bootstrap] artifacts: $WORK_DIR"
