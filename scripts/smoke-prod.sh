#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-${BASE_URL:-https://growgate.54.180.56.24.sslip.io}}"
BASE_URL="${BASE_URL%/}"
EMAIL="${SMOKE_EMAIL:-smoke-$(date +%s)@growgate.local}"
PASSWORD="${SMOKE_PASSWORD:-SmokePass123!}"
PRODUCT_NAME="${SMOKE_PRODUCT_NAME:-리튬이온 배터리팩}"
HS_CODE="${SMOKE_HS_CODE:-850760}"
TARGET_COUNTRY="${SMOKE_TARGET_COUNTRY:-DE}"
SMOKE_CORS_ORIGIN="${SMOKE_CORS_ORIGIN:-}"
SMOKE_REQUIRE_LIVE_PUBLIC_DATA="${SMOKE_REQUIRE_LIVE_PUBLIC_DATA:-false}"
SMOKE_CONNECT_TIMEOUT="${SMOKE_CONNECT_TIMEOUT:-10}"
SMOKE_MAX_TIME="${SMOKE_MAX_TIME:-45}"

LOG_DIR="${SMOKE_LOG_DIR:-/tmp/growgate-smoke}"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/smoke-$(date +%Y%m%d-%H%M%S).log"
COOKIE_JAR="$LOG_DIR/cookies.txt"

step() {
  echo "[$(date -u +%H:%M:%S)] $*" | tee -a "$LOG_FILE"
}

fail() {
  step "FAIL: $*"
  exit 1
}

request() {
  local method="$1"
  local path="$2"
  local body="${3:-}"
  local token="${4:-}"
  local output="$5"
  local status_file="$output.status"
  local header_file="$output.headers"
  local auth_args=()
  local csrf_args=()
  if [[ -n "$token" ]]; then
    auth_args=(-H "Authorization: Bearer $token")
  fi
  if [[ ! "$method" =~ ^(GET|HEAD|OPTIONS|TRACE)$ ]]; then
    local csrf
    csrf="$(csrf_token)"
    if [[ -n "$csrf" ]]; then
      csrf_args=(-H "X-GG-CSRF: $csrf")
    fi
  fi
  if [[ -n "$body" ]]; then
    curl -sS -o "$output" -D "$header_file" -w "%{http_code}" -X "$method" "$BASE_URL$path" \
      --connect-timeout "$SMOKE_CONNECT_TIMEOUT" --max-time "$SMOKE_MAX_TIME" \
      -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
      -H "Content-Type: application/json" ${auth_args[@]+"${auth_args[@]}"} ${csrf_args[@]+"${csrf_args[@]}"} -d "$body" > "$status_file"
  else
    curl -sS -o "$output" -D "$header_file" -w "%{http_code}" -X "$method" "$BASE_URL$path" \
      --connect-timeout "$SMOKE_CONNECT_TIMEOUT" --max-time "$SMOKE_MAX_TIME" \
      -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
      ${auth_args[@]+"${auth_args[@]}"} ${csrf_args[@]+"${csrf_args[@]}"} > "$status_file"
  fi
}

csrf_token() {
  [[ -f "$COOKIE_JAR" ]] || return 0
  awk '$6 == "gg_csrf_token" { print $7 }' "$COOKIE_JAR" | tail -1
}

json_get() {
  python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get(sys.argv[2], ""))' "$1" "$2"
}

step "Smoke target: $BASE_URL"

if [[ -n "$SMOKE_CORS_ORIGIN" ]]; then
  step "Checking CORS preflight for origin: $SMOKE_CORS_ORIGIN"
  CORS_OUT="$LOG_DIR/cors-preflight.txt"
  CORS_HEADERS="$LOG_DIR/cors-preflight.headers"
  curl -sS -o "$CORS_OUT" -D "$CORS_HEADERS" -w "%{http_code}" -X OPTIONS "$BASE_URL/api/users/me" \
    --connect-timeout "$SMOKE_CONNECT_TIMEOUT" --max-time "$SMOKE_MAX_TIME" \
    -H "Origin: $SMOKE_CORS_ORIGIN" \
    -H "Access-Control-Request-Method: GET" \
    -H "Access-Control-Request-Headers: authorization,content-type" > "$CORS_OUT.status"
  [[ "$(cat "$CORS_OUT.status")" =~ ^(200|204)$ ]] || fail "CORS preflight failed"
  grep -qi "^Access-Control-Allow-Origin: $SMOKE_CORS_ORIGIN" "$CORS_HEADERS" \
    || fail "CORS allow-origin missing for $SMOKE_CORS_ORIGIN"
  grep -qi "^Access-Control-Allow-Credentials: true" "$CORS_HEADERS" \
    || fail "CORS allow-credentials missing"
fi

request GET "/health" "" "" "$LOG_DIR/health.json"
[[ "$(cat "$LOG_DIR/health.json.status")" == "200" ]] || fail "/health did not return 200"

request GET "/api/diagnosis" "" "" "$LOG_DIR/diagnosis-unauth.json"
[[ "$(cat "$LOG_DIR/diagnosis-unauth.json.status")" == "401" ]] || fail "/api/diagnosis without token did not return 401"

SIGNUP_BODY="{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"name\":\"Smoke Tester\",\"type\":\"personal\",\"termsAccepted\":true,\"privacyAccepted\":true,\"marketingAccepted\":false,\"consentPolicyVersion\":\"2026-07-privacy-v1\"}"
request POST "/api/auth/signup" "$SIGNUP_BODY" "" "$LOG_DIR/signup.json"
[[ "$(cat "$LOG_DIR/signup.json.status")" =~ ^(200|201|409)$ ]] || fail "signup failed"

LOGIN_BODY="{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"
request POST "/api/auth/login" "$LOGIN_BODY" "" "$LOG_DIR/login.json"
[[ "$(cat "$LOG_DIR/login.json.status")" == "200" ]] || fail "login failed"
grep -qi "^Set-Cookie: gg_access_token=.*HttpOnly" "$LOG_DIR/login.json.headers" \
  || fail "login did not set HttpOnly gg_access_token cookie"
TOKEN="$(json_get "$LOG_DIR/login.json" "accessToken")"
[[ -n "$TOKEN" ]] || fail "login response has no accessToken"

request GET "/api/users/me" "" "$TOKEN" "$LOG_DIR/me.json"
[[ "$(cat "$LOG_DIR/me.json.status")" == "200" ]] || fail "/api/users/me failed"

request GET "/api/users/me" "" "" "$LOG_DIR/me-cookie.json"
[[ "$(cat "$LOG_DIR/me-cookie.json.status")" == "200" ]] || fail "/api/users/me with HttpOnly cookie failed"

PRODUCT_BODY="{\"name\":\"$PRODUCT_NAME\",\"hsCode\":\"$HS_CODE\",\"description\":\"Smoke test battery pack\"}"
request POST "/api/products" "$PRODUCT_BODY" "$TOKEN" "$LOG_DIR/product.json"
PRODUCT_STATUS="$(cat "$LOG_DIR/product.json.status")"
[[ "$PRODUCT_STATUS" =~ ^(200|201)$ ]] || fail "product create failed with $PRODUCT_STATUS"
PRODUCT_ID="$(json_get "$LOG_DIR/product.json" "productId")"
if [[ -z "$PRODUCT_ID" ]]; then
  PRODUCT_ID="$(json_get "$LOG_DIR/product.json" "id")"
fi
[[ -n "$PRODUCT_ID" ]] || fail "product response has no id/productId"

DIAG_BODY="{\"productId\":\"$PRODUCT_ID\",\"targetCountry\":\"$TARGET_COUNTRY\"}"
request POST "/api/diagnosis" "$DIAG_BODY" "$TOKEN" "$LOG_DIR/diagnosis-create.json"
[[ "$(cat "$LOG_DIR/diagnosis-create.json.status")" == "202" ]] || fail "diagnosis create failed"
DIAGNOSIS_ID="$(json_get "$LOG_DIR/diagnosis-create.json" "diagnosisId")"
[[ -n "$DIAGNOSIS_ID" ]] || fail "diagnosis response has no diagnosisId"

for i in {1..30}; do
  request GET "/api/diagnosis/$DIAGNOSIS_ID" "" "$TOKEN" "$LOG_DIR/diagnosis-detail.json"
  [[ "$(cat "$LOG_DIR/diagnosis-detail.json.status")" == "200" ]] || fail "diagnosis detail failed"
  STATUS="$(json_get "$LOG_DIR/diagnosis-detail.json" "status")"
  step "diagnosis status: $STATUS"
  [[ "$STATUS" == "done" || "$STATUS" == "failed" ]] && break
  sleep 2
done
[[ "$STATUS" == "done" ]] || fail "diagnosis did not complete successfully, final status=$STATUS"

python3 - "$LOG_DIR/diagnosis-detail.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
stage_cards = (data.get("result") or {}).get("stageCards") or {}
missing = [key for key in ("s04", "s05", "s06", "s07") if key not in stage_cards]
if missing:
    raise SystemExit(f"missing backend stageCards: {', '.join(missing)}")
print("stageCards.s04-s07=present")
PY

request GET "/api/diagnosis/$DIAGNOSIS_ID/sources" "" "$TOKEN" "$LOG_DIR/sources.json"
[[ "$(cat "$LOG_DIR/sources.json.status")" == "200" ]] || fail "sources failed"

request GET "/api/diagnosis/$DIAGNOSIS_ID/report" "" "$TOKEN" "$LOG_DIR/report.json"
[[ "$(cat "$LOG_DIR/report.json.status")" == "200" ]] || fail "Markdown report API failed"

python3 - "$LOG_DIR/report.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
content_type = data.get("contentType", "")
file_name = data.get("fileName", "")
markdown = data.get("markdown", "")
if "text/markdown" not in content_type or not file_name.endswith(".md") or "GROW-GATE" not in markdown:
    raise SystemExit("Markdown report payload is not presentation-ready")
PY

request GET "/api/public-data/runs" "" "$TOKEN" "$LOG_DIR/public-runs.json"
[[ "$(cat "$LOG_DIR/public-runs.json.status")" == "200" ]] || fail "public data runs failed"

request GET "/api/public-data/dashboard" "" "$TOKEN" "$LOG_DIR/public-dashboard.json"
[[ "$(cat "$LOG_DIR/public-dashboard.json.status")" == "200" ]] || fail "public data dashboard failed"

python3 - "$LOG_DIR/public-runs.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
runs = data.get("runs", [])
success = sum(r.get("successCount", 0) for r in runs)
fallback = sum(r.get("fallbackCount", 0) for r in runs)
print(f"publicData.successCount={success} fallbackCount={fallback}")
PY

python3 - "$LOG_DIR/public-dashboard.json" "$SMOKE_REQUIRE_LIVE_PUBLIC_DATA" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
require_live = sys.argv[2].lower() == "true"
success = int(data.get("successCount") or 0)
fallback = int(data.get("fallbackCount") or 0)
live_ratio = float(data.get("liveRatio") or 0)
fallback_ratio = float(data.get("fallbackRatio") or 0)
print(f"publicData.liveRatio={live_ratio} fallbackRatio={fallback_ratio}")
if require_live and success <= 0:
    raise SystemExit("SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true but no live public-data success was recorded")
PY

EVIDENCE_OUT="$LOG_DIR/live-public-data-smoke-evidence.json"
python3 - "$LOG_DIR/diagnosis-detail.json" "$LOG_DIR/sources.json" "$LOG_DIR/public-dashboard.json" "$EVIDENCE_OUT" "$SMOKE_REQUIRE_LIVE_PUBLIC_DATA" <<'PY'
import json, sys
detail = json.load(open(sys.argv[1]))
sources_payload = json.load(open(sys.argv[2]))
dashboard = json.load(open(sys.argv[3]))
out_path = sys.argv[4]
require_live = sys.argv[5].lower() == "true"
result = detail.get("result") or {}
summary = result.get("sourceTrustSummary") or {}
sources = sources_payload.get("sources") or result.get("sources") or []
source_types = [s.get("sourceType") for s in sources]
live_count = int(summary.get("liveCount") or source_types.count("live"))
cache_count = int(summary.get("cacheCount") or source_types.count("cache"))
fallback_count = int(summary.get("fallbackCount") or source_types.count("fallback"))
source_ids = summary.get("sourceIds") or [s.get("sourceId") for s in sources if s.get("sourceId")]
collection_run_ids = summary.get("collectionRunIds") or []
if not collection_run_ids and result.get("publicDataCollectionRunId"):
    collection_run_ids = [result.get("publicDataCollectionRunId")]
evidence = {
    "baseUrl": None,
    "diagnosisId": detail.get("id"),
    "status": detail.get("status"),
    "collectionRunId": collection_run_ids[0] if collection_run_ids else result.get("publicDataCollectionRunId"),
    "collectionRunIds": collection_run_ids,
    "liveCount": live_count,
    "cacheCount": cache_count,
    "fallbackCount": fallback_count,
    "dashboardSuccessCount": dashboard.get("successCount"),
    "dashboardFallbackCount": dashboard.get("fallbackCount"),
    "sourceIds": source_ids[:20],
}
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(evidence, f, ensure_ascii=False, indent=2)
print("smokeEvidence=" + out_path)
print(f"diagnosisId={evidence['diagnosisId']} collectionRunId={evidence['collectionRunId']} liveCount={live_count} cacheCount={cache_count} fallbackCount={fallback_count}")
if require_live and live_count <= 0 and cache_count <= 0:
    raise SystemExit("SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true but diagnosis sources have no live/cache records")
if require_live and not evidence["collectionRunId"]:
    raise SystemExit("SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true but collectionRunId was not captured")
PY
python3 - "$EVIDENCE_OUT" "$BASE_URL" <<'PY'
import json, sys
path, base_url = sys.argv[1], sys.argv[2]
data = json.load(open(path))
data["baseUrl"] = base_url
with open(path, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
PY

step "PASS: smoke completed. Logs: $LOG_FILE"
