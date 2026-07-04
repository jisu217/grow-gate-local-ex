#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT_DIR/docs/proofs"
LATEST_PUBLIC="$OUT_DIR/public-data/latest"
LATEST_ML="$OUT_DIR/ml/latest"
VALIDATION="$OUT_DIR/validation"
REPORT="$OUT_DIR/report"

mkdir -p "$LATEST_PUBLIC" "$LATEST_ML" "$VALIDATION" "$REPORT"

copy_or_placeholder() {
  local src="$1"
  local dest="$2"
  local placeholder="$3"
  if [[ -f "$src" ]]; then
    cp "$src" "$dest"
  else
    printf '%s\n' "$placeholder" > "$dest"
  fi
}

copy_or_placeholder "$ROOT_DIR/artifacts/live-public-data-smoke/latest/public-data-proof-run.json" \
  "$LATEST_PUBLIC/public-data-proof-run.json" \
  '{"proofStatus":"MISSING","message":"Run POST /api/admin/public-data/proof-runs on the deployed server and export this JSON."}'
copy_or_placeholder "$ROOT_DIR/artifacts/live-public-data-smoke/latest/public-data-health.json" \
  "$LATEST_PUBLIC/public-data-health.json" \
  '{"proofStatus":"MISSING","message":"Export /api/public-data/health from the deployed server."}'
copy_or_placeholder "$ROOT_DIR/artifacts/live-public-data-smoke/latest/raw-response-manifest-redacted.json" \
  "$LATEST_PUBLIC/raw-response-manifest-redacted.json" \
  '{"proofStatus":"MISSING","message":"Export /api/public-data/proof-runs/{proofRunId}/raw-responses."}'

copy_or_placeholder "$ROOT_DIR/ml/artifacts/growgate-gate-risk-v0.1.0/model_card.md" \
  "$LATEST_ML/model-card.md" \
  '# Missing model card\nRun `python3 ml/pipelines/train_all.py` first.'
copy_or_placeholder "$ROOT_DIR/ml/artifacts/growgate-gate-risk-v0.1.0/metrics.json" \
  "$LATEST_ML/metrics.json" \
  '{"status":"MISSING","message":"Run python3 ml/pipelines/train_all.py"}'
copy_or_placeholder "$ROOT_DIR/ml/artifacts/growgate-gate-risk-v0.1.0/shap_summary.json" \
  "$LATEST_ML/shap-summary.json" \
  '{"status":"MISSING","explanationMethod":"missing"}'

cp "$ROOT_DIR/docs/validation/synthetic_validation_summary.md" "$VALIDATION/synthetic-validation-summary.md"
cp "$ROOT_DIR/docs/report/sample_battery_germany.md" "$REPORT/sample-report.md"

(cd "$OUT_DIR" && find . -type f -print0 | sort -z | xargs -0 shasum -a 256) > "$LATEST_PUBLIC/checksums.txt"
echo "[proof-pack] wrote $OUT_DIR"
