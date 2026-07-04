#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_URL="${1:-${BASE_URL:-http://localhost:8080}}"
OUT_DIR="${SMOKE_OUT_DIR:-$ROOT_DIR/reports/deploy-proof}"
JSON_OUT="${SMOKE_JSON_OUT:-$OUT_DIR/latest-smoke-result.json}"
MD_OUT="${SMOKE_MD_OUT:-$OUT_DIR/latest-smoke-result.md}"
mkdir -p "$OUT_DIR"

tmp_dir="$(mktemp -d)"
cleanup() {
  rm -rf "$tmp_dir"
}
trap cleanup EXIT

request() {
  local name="$1"
  local path="$2"
  local body="$tmp_dir/$name.json"
  local status="$tmp_dir/$name.status"
  echo "[smoke] GET $path"
  curl -sS -m "${SMOKE_TIMEOUT_SECONDS:-15}" \
    -H "X-Correlation-Id: smoke-public-data-local" \
    -o "$body" \
    -w "%{http_code}" \
    "$BASE_URL$path" > "$status" || {
      echo "[smoke] FAIL: curl failed for $path" >&2
      return 1
    }
  if [[ "$(cat "$status")" != "200" ]]; then
    echo "[smoke] FAIL: $path returned HTTP $(cat "$status")" >&2
    echo "[smoke] Body: $(head -c 500 "$body")" >&2
    return 1
  fi
}

request health /health
request public_data_health /api/public-data/health

python3 - "$BASE_URL" "$tmp_dir/health.json" "$tmp_dir/public_data_health.json" "$JSON_OUT" "$MD_OUT" <<'PY'
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

base_url, health_path, public_path, json_out, md_out = sys.argv[1:]
health = json.load(open(health_path, encoding="utf-8"))
public = json.load(open(public_path, encoding="utf-8"))

providers = public.get("providers") or public.get("configuration", {}).get("providers") or []
rows = []
for provider in providers:
    row = {
        "provider": provider.get("provider"),
        "status": provider.get("status"),
        "sourceType": provider.get("sourceType"),
        "endpointType": provider.get("endpointType"),
        "apiKeyConfigured": bool(provider.get("apiKeyConfigured")),
        "apiKeyMasked": provider.get("apiKeyMasked") or "",
        "rawCount": int(provider.get("rawCount") or 0),
        "normalizedCount": int(provider.get("normalizedCount") or 0),
        "fallbackUsed": bool(provider.get("fallbackUsed")),
        "zeroReason": provider.get("zeroReason") or "",
    }
    rows.append(row)

result = {
    "baseUrl": base_url,
    "checkedAt": datetime.now(timezone.utc).isoformat(),
    "health": {
        "status": health.get("status"),
        "app": health.get("app"),
        "profile": health.get("profile"),
        "version": health.get("version"),
        "database": health.get("database"),
        "correlationHeader": "X-Correlation-Id",
    },
    "publicData": {
        "enabled": public.get("enabled"),
        "providerCount": len(rows),
        "sourceTypeCounts": {
            key: sum(1 for row in rows if row["sourceType"] == key)
            for key in ["live", "cache", "fallback", "seed", "empty"]
        },
        "providers": rows,
        "policy": public.get("policy") or public.get("configuration", {}).get("policy") or {},
    },
    "proofFiles": {
        "json": json_out,
        "markdown": md_out,
    },
}

Path(json_out).write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

lines = [
    "# GrowGate Smoke Result",
    "",
    f"- Base URL: `{base_url}`",
    f"- Checked at: `{result['checkedAt']}`",
    f"- Health: `{result['health']['status']}`",
    f"- Profile: `{result['health']['profile']}`",
    f"- Database: `{result['health']['database']}`",
    "",
    "## Public Data Providers",
    "",
    "| Provider | Source | Endpoint | API Key | Raw | Normalized | Fallback | Zero Reason |",
    "| --- | --- | --- | --- | ---: | ---: | --- | --- |",
]
for row in rows:
    lines.append(
        f"| {row['provider']} | {row['sourceType']} | {row['endpointType']} | "
        f"{'configured' if row['apiKeyConfigured'] else 'not_configured'} | "
        f"{row['rawCount']} | {row['normalizedCount']} | "
        f"{str(row['fallbackUsed']).lower()} | {row['zeroReason']} |"
    )

lines.extend([
    "",
    "## Policy",
    "",
    "```json",
    json.dumps(result["publicData"]["policy"], ensure_ascii=False, indent=2),
    "```",
])
Path(md_out).write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"[smoke] wrote {json_out}")
print(f"[smoke] wrote {md_out}")
PY
