#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SECRET_ENV="${1:-${LLM_SECRET_ENV:-/private/tmp/growgate-llm-secrets.env}}"

if [ ! -f "$SECRET_ENV" ]; then
  echo "[llm-smoke] missing secret env: $SECRET_ENV" >&2
  echo "[llm-smoke] create it outside git with GEMINI_API_KEY and OPENROUTER_API_KEY" >&2
  exit 2
fi

case "$SECRET_ENV" in
  "$ROOT_DIR"/*)
    echo "[llm-smoke] refusing to read secret env inside repository" >&2
    exit 2
    ;;
esac

set -a
# shellcheck disable=SC1090
source "$SECRET_ENV"
set +a

GEMINI_KEY="${APP_AGENT_GEMINI_API_KEY:-${GEMINI_API_KEY:-}}"
OPENROUTER_KEY="${APP_AGENT_OPENROUTER_API_KEY:-${OPENROUTER_API_KEY:-}}"
GEMINI_MODEL="${APP_AGENT_GEMINI_MODEL:-gemini-2.5-flash}"
OPENROUTER_MODEL="${APP_AGENT_OPENROUTER_MODEL:-google/gemini-2.5-flash}"
OPENROUTER_SITE_URL="${APP_AGENT_OPENROUTER_SITE_URL:-https://growgate.54.180.56.24.sslip.io}"
OPENROUTER_APP_TITLE="${APP_AGENT_OPENROUTER_APP_TITLE:-GROW-GATE Local EX}"

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/growgate-llm-smoke.XXXXXX")"
trap 'rm -rf "$TMP_DIR"' EXIT

if [ -z "$GEMINI_KEY" ] && [ -z "$OPENROUTER_KEY" ]; then
  echo "[llm-smoke] no Gemini or OpenRouter key configured" >&2
  exit 2
fi

if [ -n "$GEMINI_KEY" ]; then
  GEMINI_OUT="$TMP_DIR/gemini.json"
  GEMINI_PAYLOAD="$TMP_DIR/gemini-payload.json"
  printf '%s' '{"contents":[{"role":"user","parts":[{"text":"Return compact JSON only: {\"summary\":\"llm-smoke-ok\"}"}]}],"generationConfig":{"temperature":0,"maxOutputTokens":64}}' > "$GEMINI_PAYLOAD"
  GEMINI_CODE="$(
    curl -sS -m 30 -o "$GEMINI_OUT" -w "%{http_code}" \
      -H "Content-Type: application/json" \
      -H "x-goog-api-key: $GEMINI_KEY" \
      "https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent" \
      --data-binary "@$GEMINI_PAYLOAD"
  )"
  python3 - "$GEMINI_OUT" "$GEMINI_CODE" <<'PY'
import json, sys
path, code = sys.argv[1], sys.argv[2]
data = json.load(open(path))
ok = code.startswith("2") and bool(data.get("candidates"))
print("[llm-smoke] gemini=http_%s candidates=%s" % (code, len(data.get("candidates", []))))
sys.exit(0 if ok else 1)
PY
else
  echo "[llm-smoke] gemini=skipped"
fi

if [ -n "$OPENROUTER_KEY" ]; then
  OPENROUTER_OUT="$TMP_DIR/openrouter.json"
  OPENROUTER_PAYLOAD="$TMP_DIR/openrouter-payload.json"
  python3 - "$OPENROUTER_PAYLOAD" "$OPENROUTER_MODEL" <<'PY'
import json, sys
path, model = sys.argv[1], sys.argv[2]
payload = {
    "model": model,
    "messages": [{"role": "user", "content": "Return compact JSON only: {\"summary\":\"llm-smoke-ok\"}"}],
    "temperature": 0,
    "max_tokens": 64,
}
open(path, "w").write(json.dumps(payload))
PY
  OPENROUTER_CODE="$(
    curl -sS -m 30 -o "$OPENROUTER_OUT" -w "%{http_code}" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $OPENROUTER_KEY" \
      -H "HTTP-Referer: $OPENROUTER_SITE_URL" \
      -H "X-OpenRouter-Title: $OPENROUTER_APP_TITLE" \
      "https://openrouter.ai/api/v1/chat/completions" \
      --data-binary "@$OPENROUTER_PAYLOAD"
  )"
  python3 - "$OPENROUTER_OUT" "$OPENROUTER_CODE" <<'PY'
import json, sys
path, code = sys.argv[1], sys.argv[2]
data = json.load(open(path))
ok = code.startswith("2") and bool(data.get("choices"))
print("[llm-smoke] openrouter=http_%s choices=%s" % (code, len(data.get("choices", []))))
sys.exit(0 if ok else 1)
PY
else
  echo "[llm-smoke] openrouter=skipped"
fi

echo "[llm-smoke] pass"
