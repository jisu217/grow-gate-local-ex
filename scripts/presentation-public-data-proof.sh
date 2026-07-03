#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-https://growgate.54.180.56.24.sslip.io}"
BASE_URL="${BASE_URL%/}"
DIAGNOSIS_ID="${DIAGNOSIS_ID:-${1:-}}"
AUTH_TOKEN="${AUTH_TOKEN:-}"
COOKIE_FILE="${COOKIE_FILE:-}"
OUT_DIR="${OUT_DIR:-artifacts/presentation}"
REQUIRE_LIVE="${PRESENTATION_REQUIRE_LIVE_PUBLIC_DATA:-false}"
REQUIRE_OPERATION_API="${PRESENTATION_REQUIRE_OPERATION_API:-false}"
CONNECT_TIMEOUT="${CONNECT_TIMEOUT:-10}"
MAX_TIME="${MAX_TIME:-45}"

if [[ -z "$DIAGNOSIS_ID" ]]; then
  echo "usage: DIAGNOSIS_ID=<id> [AUTH_TOKEN=<jwt>|COOKIE_FILE=<jar>] $0" >&2
  exit 2
fi

mkdir -p "$OUT_DIR"

auth_args=()
cookie_args=()
if [[ -n "$AUTH_TOKEN" ]]; then
  auth_args=(-H "Authorization: Bearer $AUTH_TOKEN")
fi
if [[ -n "$COOKIE_FILE" ]]; then
  cookie_args=(-b "$COOKIE_FILE")
fi

request_json() {
  local path="$1"
  local output="$2"
  local status_file="$output.status"
  curl -sS -o "$output" -w "%{http_code}" "$BASE_URL$path" \
    --connect-timeout "$CONNECT_TIMEOUT" --max-time "$MAX_TIME" \
    "${auth_args[@]}" "${cookie_args[@]}" > "$status_file"
  local status
  status="$(cat "$status_file")"
  if [[ ! "$status" =~ ^2 ]]; then
    echo "request failed: $path -> HTTP $status" >&2
    [[ -s "$output" ]] && cat "$output" >&2 || true
    exit 1
  fi
}

health_json="$OUT_DIR/public-data-health-$DIAGNOSIS_ID.json"
detail_json="$OUT_DIR/diagnosis-detail-$DIAGNOSIS_ID.json"
sources_json="$OUT_DIR/diagnosis-sources-$DIAGNOSIS_ID.json"
proof_json="$OUT_DIR/live-public-data-proof-$DIAGNOSIS_ID.json"

request_json "/api/public-data/health" "$health_json"
request_json "/api/diagnosis/$DIAGNOSIS_ID" "$detail_json"
request_json "/api/diagnosis/$DIAGNOSIS_ID/sources" "$sources_json"

python3 - "$BASE_URL" "$health_json" "$detail_json" "$sources_json" "$proof_json" "$REQUIRE_LIVE" "$REQUIRE_OPERATION_API" <<'PY'
import json
import sys

base_url, health_path, detail_path, sources_path, out_path, require_live, require_operation = sys.argv[1:]
health = json.load(open(health_path, encoding="utf-8"))
detail = json.load(open(detail_path, encoding="utf-8"))
sources_payload = json.load(open(sources_path, encoding="utf-8"))
result = detail.get("result") or {}
summary = result.get("sourceTrustSummary") or {}
sources = sources_payload.get("sources") or result.get("sources") or []

def truthy(value):
    return value is True or str(value).lower() == "true"

def source_type(source):
    return source.get("sourceType") or "fallback"

def is_live_or_cache(source):
    return source_type(source) in {"live", "cache"}

def is_official_operation_api(source):
    evidence = source.get("collectionEvidence") or {}
    return (
        is_live_or_cache(source)
        and truthy(source.get("operationEndpoint", evidence.get("operationEndpoint")))
        and truthy(source.get("apiKeyConfigured", evidence.get("apiKeyConfigured")))
    )

live_count = int(summary.get("liveCount") or sum(1 for source in sources if source_type(source) == "live"))
cache_count = int(summary.get("cacheCount") or sum(1 for source in sources if source_type(source) == "cache"))
fallback_count = int(summary.get("fallbackCount") or sum(1 for source in sources if source_type(source) == "fallback"))
external_feed_count = int(summary.get("externalFeedCount") or sum(1 for source in sources if source_type(source) in {"external_feed", "external_reconstructed"}))
crawl_count = int(summary.get("crawlCount") or sum(1 for source in sources if source_type(source) == "crawl"))
operation_endpoint_live_count = int(summary.get("operationEndpointLiveCount") or sum(
    1 for source in sources
    if is_live_or_cache(source) and truthy(source.get("operationEndpoint", (source.get("collectionEvidence") or {}).get("operationEndpoint")))
))
api_key_configured_live_count = int(summary.get("apiKeyConfiguredLiveCount") or sum(
    1 for source in sources
    if is_live_or_cache(source) and truthy(source.get("apiKeyConfigured", (source.get("collectionEvidence") or {}).get("apiKeyConfigured")))
))
official_operation_count = sum(1 for source in sources if is_official_operation_api(source))
motie_official_operation_count = sum(
    1 for source in sources
    if is_official_operation_api(source) and truthy(source.get("motieRelated"))
)
collection_run_ids = list(summary.get("collectionRunIds") or [])
if not collection_run_ids and result.get("publicDataCollectionRunId"):
    collection_run_ids = [result["publicDataCollectionRunId"]]
if not collection_run_ids:
    for source in sources:
        run_id = source.get("collectionRunId")
        if run_id and run_id not in collection_run_ids:
            collection_run_ids.append(run_id)

proof_sources = []
for source in sources:
    proof_sources.append({
        "sourceId": source.get("sourceId"),
        "recordId": source.get("recordId"),
        "provider": source.get("provider"),
        "sourceType": source_type(source),
        "usedFor": source.get("usedFor"),
        "motieRelated": source.get("motieRelated"),
        "collectionRunId": source.get("collectionRunId"),
        "operationEndpoint": truthy(source.get("operationEndpoint", (source.get("collectionEvidence") or {}).get("operationEndpoint"))),
        "apiKeyConfigured": truthy(source.get("apiKeyConfigured", (source.get("collectionEvidence") or {}).get("apiKeyConfigured"))),
        "endpointType": source.get("endpointType") or (source.get("collectionEvidence") or {}).get("endpointType"),
        "fetchedAt": source.get("fetchedAt") or source.get("retrievedAt"),
        "sourcePeriodStart": source.get("sourcePeriodStart"),
        "sourcePeriodEnd": source.get("sourcePeriodEnd"),
        "metricSummary": source.get("metricSummary"),
        "decisionImpact": source.get("decisionImpact"),
        "fallbackUsed": source.get("fallbackUsed"),
        "reason": source.get("reason") or source.get("errorMessage"),
    })

proof = {
    "baseUrl": base_url,
    "diagnosisId": detail.get("id"),
    "status": detail.get("status"),
    "capturedFrom": {
        "health": "/api/public-data/health",
        "detail": f"/api/diagnosis/{detail.get('id')}",
        "sources": f"/api/diagnosis/{detail.get('id')}/sources",
    },
    "collectionRunId": collection_run_ids[0] if collection_run_ids else None,
    "collectionRunIds": collection_run_ids,
    "liveCount": live_count,
    "cacheCount": cache_count,
    "fallbackCount": fallback_count,
    "crawlCount": crawl_count,
    "externalFeedCount": external_feed_count,
    "operationEndpointLiveCount": operation_endpoint_live_count,
    "apiKeyConfiguredLiveCount": api_key_configured_live_count,
    "officialOperationApiEvidenceCount": official_operation_count,
    "motieOfficialOperationApiEvidenceCount": motie_official_operation_count,
    "health": {
        "motieVerifiedOperationLiveCount": health.get("motieVerifiedOperationLiveCount"),
        "motieLiveEvidenceReady": health.get("motieLiveEvidenceReady"),
        "motieRelatedScoreContribution": health.get("motieRelatedScoreContribution"),
    },
    "sourceIds": [source.get("sourceId") for source in sources if source.get("sourceId")][:30],
    "sources": proof_sources,
}
with open(out_path, "w", encoding="utf-8") as handle:
    json.dump(proof, handle, ensure_ascii=False, indent=2)

print(f"proof={out_path}")
print(
    "diagnosisId={diagnosisId} collectionRunId={collectionRunId} liveCount={liveCount} "
    "cacheCount={cacheCount} fallbackCount={fallbackCount} operationEndpointLiveCount={operationEndpointLiveCount} "
    "apiKeyConfiguredLiveCount={apiKeyConfiguredLiveCount} officialOperationApiEvidenceCount={officialOperationApiEvidenceCount}"
    .format(**proof)
)

if require_live.lower() == "true" and live_count + cache_count <= 0:
    raise SystemExit("PRESENTATION_REQUIRE_LIVE_PUBLIC_DATA=true but diagnosis has no live/cache public-data sources")
if require_operation.lower() == "true" and official_operation_count <= 0:
    raise SystemExit("PRESENTATION_REQUIRE_OPERATION_API=true but no live/cache source has both operationEndpoint and apiKeyConfigured evidence")
PY
