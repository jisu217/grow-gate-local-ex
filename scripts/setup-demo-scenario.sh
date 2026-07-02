#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-${BASE_URL:-https://growgate.54.180.56.24.sslip.io}}"
BASE_URL="${BASE_URL%/}"
DEMO_EMAIL="${DEMO_EMAIL:-demo-pmgrow@growgate.local}"
DEMO_PASSWORD="${DEMO_PASSWORD:-GrowGateDemo123!}"
DEMO_NAME="${DEMO_NAME:-GrowGate Demo Owner}"
DEMO_RUN_ID="${DEMO_RUN_ID:-$(date +%Y%m%d-%H%M)}"
PRODUCT_NAME="${DEMO_PRODUCT_NAME:-PMGrow 리튬이온 배터리팩 $DEMO_RUN_ID}"
HS_CODE="${DEMO_HS_CODE:-850760}"
TARGET_COUNTRY="${DEMO_TARGET_COUNTRY:-DE}"
DEMO_ACCEPT_LOOP_ITEMS="${DEMO_ACCEPT_LOOP_ITEMS:-true}"
DEMO_REQUIRE_LIVE_PUBLIC_DATA="${DEMO_REQUIRE_LIVE_PUBLIC_DATA:-false}"
DEMO_CONNECT_TIMEOUT="${DEMO_CONNECT_TIMEOUT:-10}"
DEMO_MAX_TIME="${DEMO_MAX_TIME:-45}"

LOG_DIR="${DEMO_LOG_DIR:-/tmp/growgate-demo-$DEMO_RUN_ID}"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/setup.log"
COOKIE_JAR="$LOG_DIR/cookies.txt"
SUMMARY_MD="$LOG_DIR/demo-summary.md"

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
  local body_file="${3:-}"
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
  if [[ -n "$body_file" ]]; then
    curl -sS -o "$output" -D "$header_file" -w "%{http_code}" -X "$method" "$BASE_URL$path" \
      --connect-timeout "$DEMO_CONNECT_TIMEOUT" --max-time "$DEMO_MAX_TIME" \
      -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
      -H "Content-Type: application/json" ${auth_args[@]+"${auth_args[@]}"} ${csrf_args[@]+"${csrf_args[@]}"} --data-binary @"$body_file" > "$status_file"
  else
    curl -sS -o "$output" -D "$header_file" -w "%{http_code}" -X "$method" "$BASE_URL$path" \
      --connect-timeout "$DEMO_CONNECT_TIMEOUT" --max-time "$DEMO_MAX_TIME" \
      -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
      ${auth_args[@]+"${auth_args[@]}"} ${csrf_args[@]+"${csrf_args[@]}"} > "$status_file"
  fi
}

csrf_token() {
  [[ -f "$COOKIE_JAR" ]] || return 0
  awk '$6 == "gg_csrf_token" { print $7 }' "$COOKIE_JAR" | tail -1
}

json_get() {
  python3 - "$1" "$2" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
value = data
for part in sys.argv[2].split("."):
    if isinstance(value, dict):
        value = value.get(part, "")
    else:
        value = ""
    if value == "":
        break
print(value if value is not None else "")
PY
}

make_json() {
  local output="$1"
  local kind="$2"
  shift 2
  python3 - "$output" "$kind" "$@" <<'PY'
import json, sys
out, kind, *args = sys.argv[1:]

def arg(name, default=""):
    try:
        return args[args.index(name) + 1]
    except ValueError:
        return default

payloads = {
    "signup": {
        "type": "personal",
        "email": arg("email"),
        "password": arg("password"),
        "name": arg("name"),
        "termsAccepted": True,
        "privacyAccepted": True,
        "marketingAccepted": False,
        "consentPolicyVersion": "2026-07-privacy-v1",
    },
    "login": {
        "email": arg("email"),
        "password": arg("password"),
    },
    "product": {
        "name": arg("name"),
        "category": "Industrial lithium-ion battery pack",
        "hsCode": arg("hsCode"),
        "targetCountry": arg("targetCountry"),
        "description": (
            "독일/EU B2B 납품을 목표로 하는 산업용 리튬이온 배터리팩. "
            "UN38.3 운송시험, IEC 62133-2, CE/EMC, RoHS, EU Battery Regulation "
            "2023/1542, 배터리 여권 준비 상태를 함께 점검한다."
        ),
    },
    "knowledge": {
        "name": arg("name"),
        "category": "Industrial lithium-ion battery pack",
        "hsCode": arg("hsCode"),
        "targetCountry": arg("targetCountry"),
        "description": (
            "독일 전동 모빌리티 부품사에 납품하는 48V/72V 모듈형 배터리팩. "
            "BMS, 열폭주 완화 구조, 위험물 운송 라벨링, 재활용/탄소발자국 "
            "자료 준비가 수출 승인과 바이어 계약의 핵심 리스크다."
        ),
        "knowledge": {
            "trustGrade": "high",
            "sourceType": "manager_verified",
            "notes": (
                "담당자 검증 입력: 셀은 KC 인증 이력 보유, 팩 조립은 국내 공장, "
                "UN38.3 시험성적서는 보유하나 EU Battery Regulation 문서와 "
                "배터리 여권 항목은 보완 필요. 기존 독일 바이어는 CE/EMC, MSDS, "
                "운송 포장 라벨 증빙을 요구했다."
            ),
        },
        "reason": "발표 데모용 제품 지식 초기화",
    },
    "cert-un38": {
        "name": "UN38.3 운송시험성적서",
        "issuedAt": "2025-03-20",
        "expiresAt": "2027-03-20",
        "renewalCycle": 3,
        "reason": "위험물 운송 필수 증빙 등록",
    },
    "cert-iec": {
        "name": "IEC 62133-2 시험 계획",
        "issuedAt": "2025-06-15",
        "expiresAt": "2026-11-30",
        "renewalCycle": 1,
        "reason": "EU 바이어 요구 인증 준비 상태 등록",
    },
    "cert-rohs": {
        "name": "RoHS 자체 적합성 선언",
        "issuedAt": "2025-05-10",
        "expiresAt": "2027-05-10",
        "renewalCycle": 2,
        "reason": "EU 유해물질 제한 대응 증빙 등록",
    },
    "fto": {
        "targetCountry": arg("targetCountry"),
        "riskLevel": "medium",
        "summary": (
            "BMS 충방전 제어와 열폭주 차단 구조는 경쟁사 특허와 청구항 비교가 필요하다. "
            "팩 하우징 구조와 냉각 채널은 KIPRIS/Espacenet 2차 검토 대상이다."
        ),
        "expiresAt": "2026-12-31",
        "reason": "독일 수출 전 FTO 검토 항목 등록",
    },
    "buyer-1": {
        "buyerName": "Bayern Mobility GmbH",
        "country": arg("targetCountry"),
        "requirements": (
            "UN38.3, IEC 62133-2, CE/EMC, MSDS, RoHS 적합성, EU Battery Regulation "
            "2023/1542 문서, 배터리 여권 데이터 준비 현황 제출"
        ),
        "reason": "핵심 바이어 요구사항 등록",
    },
    "buyer-2": {
        "buyerName": "Nord Logistics Partner",
        "country": arg("targetCountry"),
        "requirements": (
            "위험물 운송 포장 사양서, 낙하/충격 시험 체크리스트, 포장 라벨, "
            "SDS, 선적 전 온도/SoC 관리 기준 제출"
        ),
        "reason": "물류 파트너 요구사항 등록",
    },
    "source-1": {
        "url": "https://eur-lex.europa.eu/eli/reg/2023/1542/oj",
        "documentName": "EU Battery Regulation 2023/1542",
        "retrievedAt": "2026-07-02",
        "reason": "EU 배터리 규정 공식 출처 등록",
    },
    "source-2": {
        "url": "https://unece.org/transport/dangerous-goods/un-manual-tests-and-criteria",
        "documentName": "UN Manual of Tests and Criteria",
        "retrievedAt": "2026-07-02",
        "reason": "UN38.3 위험물 운송시험 공식 출처 등록",
    },
    "source-3": {
        "url": "https://www.kats.go.kr/",
        "documentName": "국가기술표준원 인증/표준 참고",
        "retrievedAt": "2026-07-02",
        "reason": "국내 인증/표준 확인 출처 등록",
    },
    "collect": {
        "hsCode": arg("hsCode"),
        "targetCountry": arg("targetCountry"),
        "productName": arg("name"),
    },
    "diagnosis": {
        "productId": arg("productId"),
        "targetCountry": arg("targetCountry"),
    },
}

json.dump(payloads[kind], open(out, "w"), ensure_ascii=False)
PY
}

first_pending_loop_item() {
  python3 - "$1" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
for item in data.get("items", []):
    if str(item.get("status", "")).lower() == "pending":
        print(item.get("id", ""))
        break
PY
}

summarize_diagnosis() {
  python3 - "$1" "$2" "$3" <<'PY'
import json, sys
detail = json.load(open(sys.argv[1]))
sources = json.load(open(sys.argv[2]))
dashboard = json.load(open(sys.argv[3]))
result = detail.get("result") or {}
score = result.get("diagnosisScore") or {}
stage_cards = result.get("stageCards") or {}
source_items = sources.get("sources") or []
source_types = {}
for item in source_items:
    source_types[item.get("sourceType", "unknown")] = source_types.get(item.get("sourceType", "unknown"), 0) + 1
print(json.dumps({
    "status": detail.get("status"),
    "overall": score.get("overall"),
    "market": score.get("market"),
    "regulation": score.get("regulation"),
    "recall": score.get("recall"),
    "fto": score.get("fto"),
    "support": score.get("support"),
    "stageCards": sorted(stage_cards.keys()),
    "sourceTypes": source_types,
    "publicData": {
        "successCount": dashboard.get("successCount"),
        "fallbackCount": dashboard.get("fallbackCount"),
        "liveRatio": dashboard.get("liveRatio"),
        "fallbackRatio": dashboard.get("fallbackRatio"),
    },
}, ensure_ascii=False, indent=2))
PY
}

step "Demo target: $BASE_URL"
step "Log directory: $LOG_DIR"

request GET "/health" "" "" "$LOG_DIR/health.json"
[[ "$(cat "$LOG_DIR/health.json.status")" == "200" ]] || fail "/health did not return 200"

make_json "$LOG_DIR/signup-body.json" signup email "$DEMO_EMAIL" password "$DEMO_PASSWORD" name "$DEMO_NAME"
request POST "/api/auth/signup" "$LOG_DIR/signup-body.json" "" "$LOG_DIR/signup.json"
[[ "$(cat "$LOG_DIR/signup.json.status")" =~ ^(200|201|409)$ ]] || fail "signup failed"

make_json "$LOG_DIR/login-body.json" login email "$DEMO_EMAIL" password "$DEMO_PASSWORD"
request POST "/api/auth/login" "$LOG_DIR/login-body.json" "" "$LOG_DIR/login.json"
[[ "$(cat "$LOG_DIR/login.json.status")" == "200" ]] || fail "login failed"
if grep -qi "^Set-Cookie: gg_access_token=.*HttpOnly" "$LOG_DIR/login.json.headers"; then
  step "Login set HttpOnly gg_access_token cookie"
else
  step "WARN: deployed login did not set HttpOnly gg_access_token cookie; continuing with Bearer token for demo setup"
fi
TOKEN="$(json_get "$LOG_DIR/login.json" "accessToken")"
[[ -n "$TOKEN" ]] || fail "login response has no accessToken"
step "Demo account is ready: $DEMO_EMAIL"

make_json "$LOG_DIR/product-body.json" product name "$PRODUCT_NAME" hsCode "$HS_CODE" targetCountry "$TARGET_COUNTRY"
request POST "/api/products" "$LOG_DIR/product-body.json" "$TOKEN" "$LOG_DIR/product.json"
[[ "$(cat "$LOG_DIR/product.json.status")" == "201" ]] || fail "product create failed"
PRODUCT_ID="$(json_get "$LOG_DIR/product.json" "productId")"
[[ -n "$PRODUCT_ID" ]] || fail "product response has no productId"
step "Product created: $PRODUCT_ID / $PRODUCT_NAME"

make_json "$LOG_DIR/knowledge-body.json" knowledge name "$PRODUCT_NAME" hsCode "$HS_CODE" targetCountry "$TARGET_COUNTRY"
request PUT "/api/products/$PRODUCT_ID/knowledge" "$LOG_DIR/knowledge-body.json" "$TOKEN" "$LOG_DIR/knowledge-update.json"
[[ "$(cat "$LOG_DIR/knowledge-update.json.status")" == "200" ]] || fail "knowledge update failed"

for cert in cert-un38 cert-iec cert-rohs; do
  make_json "$LOG_DIR/$cert.json" "$cert"
  request POST "/api/products/$PRODUCT_ID/certifications" "$LOG_DIR/$cert.json" "$TOKEN" "$LOG_DIR/$cert.response.json"
  [[ "$(cat "$LOG_DIR/$cert.response.json.status")" == "201" ]] || fail "$cert create failed"
done

make_json "$LOG_DIR/fto-body.json" fto targetCountry "$TARGET_COUNTRY"
request POST "/api/products/$PRODUCT_ID/fto" "$LOG_DIR/fto-body.json" "$TOKEN" "$LOG_DIR/fto.json"
[[ "$(cat "$LOG_DIR/fto.json.status")" == "201" ]] || fail "FTO create failed"

for buyer in buyer-1 buyer-2; do
  make_json "$LOG_DIR/$buyer.json" "$buyer" targetCountry "$TARGET_COUNTRY"
  request POST "/api/products/$PRODUCT_ID/buyer-requirements" "$LOG_DIR/$buyer.json" "$TOKEN" "$LOG_DIR/$buyer.response.json"
  [[ "$(cat "$LOG_DIR/$buyer.response.json.status")" == "201" ]] || fail "$buyer create failed"
done

for source in source-1 source-2 source-3; do
  make_json "$LOG_DIR/$source.json" "$source"
  request POST "/api/products/$PRODUCT_ID/knowledge/sources" "$LOG_DIR/$source.json" "$TOKEN" "$LOG_DIR/$source.response.json"
  [[ "$(cat "$LOG_DIR/$source.response.json.status")" == "201" ]] || fail "$source create failed"
done

request GET "/api/products/$PRODUCT_ID/knowledge" "" "$TOKEN" "$LOG_DIR/knowledge-detail.json"
[[ "$(cat "$LOG_DIR/knowledge-detail.json.status")" == "200" ]] || fail "knowledge detail failed"

make_json "$LOG_DIR/public-collect-body.json" collect name "$PRODUCT_NAME" hsCode "$HS_CODE" targetCountry "$TARGET_COUNTRY"
request POST "/api/public-data/collect" "$LOG_DIR/public-collect-body.json" "$TOKEN" "$LOG_DIR/public-collect.json"
[[ "$(cat "$LOG_DIR/public-collect.json.status")" == "202" ]] || fail "public data collect failed"
step "Public-data collection run requested"

make_json "$LOG_DIR/diagnosis-body.json" diagnosis productId "$PRODUCT_ID" targetCountry "$TARGET_COUNTRY"
request POST "/api/diagnosis" "$LOG_DIR/diagnosis-body.json" "$TOKEN" "$LOG_DIR/diagnosis-create.json"
[[ "$(cat "$LOG_DIR/diagnosis-create.json.status")" == "202" ]] || fail "diagnosis create failed"
DIAGNOSIS_ID="$(json_get "$LOG_DIR/diagnosis-create.json" "diagnosisId")"
[[ -n "$DIAGNOSIS_ID" ]] || fail "diagnosis response has no diagnosisId"
step "Diagnosis created: $DIAGNOSIS_ID"

STATUS=""
for _ in {1..45}; do
  request GET "/api/diagnosis/$DIAGNOSIS_ID" "" "$TOKEN" "$LOG_DIR/diagnosis-detail.json"
  [[ "$(cat "$LOG_DIR/diagnosis-detail.json.status")" == "200" ]] || fail "diagnosis detail failed"
  STATUS="$(json_get "$LOG_DIR/diagnosis-detail.json" "status")"
  step "diagnosis status: $STATUS"
  [[ "$STATUS" == "done" || "$STATUS" == "completed" || "$STATUS" == "failed" ]] && break
  sleep 2
done
[[ "$STATUS" == "done" || "$STATUS" == "completed" ]] || fail "diagnosis did not complete successfully, final status=$STATUS"

python3 - "$LOG_DIR/diagnosis-detail.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
stage_cards = (data.get("result") or {}).get("stageCards") or {}
missing = [key for key in ("s04", "s05", "s06", "s07", "knowledgeLoop") if key not in stage_cards]
if missing:
    raise SystemExit("missing stageCards: " + ", ".join(missing))
PY

request GET "/api/diagnosis/$DIAGNOSIS_ID/sources" "" "$TOKEN" "$LOG_DIR/sources.json"
[[ "$(cat "$LOG_DIR/sources.json.status")" == "200" ]] || fail "sources failed"

request GET "/api/diagnosis/$DIAGNOSIS_ID/report" "" "$TOKEN" "$LOG_DIR/report.json"
[[ "$(cat "$LOG_DIR/report.json.status")" == "200" ]] || fail "Markdown report failed"

request GET "/api/diagnosis/$DIAGNOSIS_ID/report.pdf" "" "$TOKEN" "$LOG_DIR/report.pdf"
[[ "$(cat "$LOG_DIR/report.pdf.status")" == "200" ]] || fail "PDF report failed"
head -c 5 "$LOG_DIR/report.pdf" | grep -q "%PDF-" || fail "report.pdf is not a PDF"

request GET "/api/products/$PRODUCT_ID/knowledge/loop-items" "" "$TOKEN" "$LOG_DIR/loop-items.json"
[[ "$(cat "$LOG_DIR/loop-items.json.status")" == "200" ]] || fail "knowledge loop items failed"

ACCEPTED_LOOP_ITEM=""
if [[ "$DEMO_ACCEPT_LOOP_ITEMS" == "true" ]]; then
  LOOP_ITEM_ID="$(first_pending_loop_item "$LOG_DIR/loop-items.json")"
  if [[ -n "$LOOP_ITEM_ID" ]]; then
    request POST "/api/products/$PRODUCT_ID/knowledge/loop-items/$LOOP_ITEM_ID/accept" "" "$TOKEN" "$LOG_DIR/loop-accept.json"
    [[ "$(cat "$LOG_DIR/loop-accept.json.status")" == "200" ]] || fail "knowledge loop accept failed"
    ACCEPTED_LOOP_ITEM="$LOOP_ITEM_ID"
    step "Accepted one knowledge-loop item: $ACCEPTED_LOOP_ITEM"
  else
    step "No pending knowledge-loop item was available to accept"
  fi
fi

request GET "/api/public-data/runs" "" "$TOKEN" "$LOG_DIR/public-runs.json"
[[ "$(cat "$LOG_DIR/public-runs.json.status")" == "200" ]] || fail "public data runs failed"

request GET "/api/public-data/dashboard" "" "$TOKEN" "$LOG_DIR/public-dashboard.json"
[[ "$(cat "$LOG_DIR/public-dashboard.json.status")" == "200" ]] || fail "public data dashboard failed"

python3 - "$LOG_DIR/public-dashboard.json" "$DEMO_REQUIRE_LIVE_PUBLIC_DATA" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
require_live = sys.argv[2].lower() == "true"
success = int(data.get("successCount") or 0)
fallback = int(data.get("fallbackCount") or 0)
print(f"publicData.successCount={success} fallbackCount={fallback}")
if require_live and success <= 0:
    raise SystemExit("DEMO_REQUIRE_LIVE_PUBLIC_DATA=true but no live public-data success was recorded")
PY

SUMMARY_JSON="$LOG_DIR/diagnosis-summary.json"
summarize_diagnosis "$LOG_DIR/diagnosis-detail.json" "$LOG_DIR/sources.json" "$LOG_DIR/public-dashboard.json" > "$SUMMARY_JSON"

cat > "$SUMMARY_MD" <<EOF
# GrowGate Demo Scenario

- Base URL: $BASE_URL
- Demo account: $DEMO_EMAIL
- Demo password: $DEMO_PASSWORD
- Product: $PRODUCT_NAME
- Product ID: $PRODUCT_ID
- Diagnosis ID: $DIAGNOSIS_ID
- Accepted knowledge-loop item: ${ACCEPTED_LOOP_ITEM:-none}
- Login: $BASE_URL/login
- My Page: $BASE_URL/mypage
- Diagnosis result: $BASE_URL/diagnosis/$DIAGNOSIS_ID
- Logs: $LOG_DIR

## Diagnosis Summary

\`\`\`json
$(cat "$SUMMARY_JSON")
\`\`\`
EOF

step "PASS: demo scenario is ready"
step "Summary: $SUMMARY_MD"
