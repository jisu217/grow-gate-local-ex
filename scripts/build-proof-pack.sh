#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT_DIR/docs/proofs"
LATEST_PUBLIC="$OUT_DIR/public-data/latest"
LATEST_ML="$OUT_DIR/ml/latest"
VALIDATION="$OUT_DIR/validation"
REPORT="$OUT_DIR/report"

mkdir -p "$LATEST_PUBLIC" "$LATEST_ML" "$VALIDATION" "$REPORT"

copy_or_sample() {
  local src="$1"
  local dest="$2"
  local sample="$3"
  if [[ -f "$src" ]]; then
    cp "$src" "$dest"
  else
    printf '%s\n' "$sample" > "$dest"
  fi
}

copy_or_sample "$ROOT_DIR/artifacts/live-public-data-smoke/latest/public-data-proof-run.json" \
  "$LATEST_PUBLIC/public-data-proof-run.json" \
  '{
  "proofPackVersion": "2026-07-04",
  "proofStatus": "SAMPLE_READY",
  "proofSource": "packaged-redacted-sample_not_deployed_live_export",
  "operatorNote": "Replace with deployed server export from /api/admin/public-data/proof-runs before final live presentation.",
  "proofRunId": "proof-sample-20260704",
  "diagnosisId": "sample-diagnosis-redacted",
  "collectionRunId": "sample-collection-redacted",
  "liveProviderCount": 3,
  "motieRelatedLiveCount": 2,
  "motieRelatedScoreContribution": 12,
  "providers": [
    {
      "provider": "KOTRA_MARKET",
      "dataFamily": "MOTIE_RELATED",
      "sourceType": "live",
      "operationEndpoint": true,
      "apiKeyConfigured": true,
      "apiKeyStored": false,
      "rawResponseSha256": "b2d6f18d6f5e4f0d9b66a7b0e7d7f6a9f8f1c0d2a3b4c5d6e7f8091a2b3c4d5e",
      "schemaValidationStatus": "PASS"
    },
    {
      "provider": "SAFETY_RECALL",
      "dataFamily": "MOTIE_RELATED",
      "sourceType": "live",
      "operationEndpoint": true,
      "apiKeyConfigured": true,
      "apiKeyStored": false,
      "rawResponseSha256": "c7f4e5d6a3b291807f6e5d4c3b2a1908f7e6d5c4b3a291807f6e5d4c3b2a1908",
      "schemaValidationStatus": "PASS"
    },
    {
      "provider": "KCS_TRADE",
      "dataFamily": "CUSTOMS_TRADE",
      "sourceType": "live",
      "operationEndpoint": true,
      "apiKeyConfigured": true,
      "apiKeyStored": false,
      "rawResponseSha256": "d9a1b2c3e4f506172839405162738495a6b7c8d9e0f11223344556677889900a",
      "schemaValidationStatus": "PASS"
    }
  ]
}'
copy_or_sample "$ROOT_DIR/artifacts/live-public-data-smoke/latest/public-data-health.json" \
  "$LATEST_PUBLIC/public-data-health.json" \
  '{
  "proofStatus": "SAMPLE_READY",
  "proofSource": "packaged-redacted-sample_not_deployed_live_export",
  "liveProviderCount": 3,
  "motieRelatedLiveCount": 2,
  "motieVerifiedOperationLiveCount": 2,
  "operationEndpointLiveCount": 3,
  "apiKeyConfiguredLiveCount": 3,
  "fallbackCount": 0,
  "lastCollectedAt": "2026-07-04T00:00:00Z"
}'
copy_or_sample "$ROOT_DIR/artifacts/live-public-data-smoke/latest/raw-response-manifest-redacted.json" \
  "$LATEST_PUBLIC/raw-response-manifest-redacted.json" \
  '{
  "proofStatus": "SAMPLE_READY",
  "proofSource": "packaged-redacted-sample_not_deployed_live_export",
  "redactionPolicy": "no raw payload, no service keys, hashes only",
  "entries": [
    {
      "provider": "KOTRA_MARKET",
      "endpointType": "operation",
      "requestKeyNames": ["serviceKey"],
      "requestKeyValues": ["REDACTED"],
      "rawResponseSha256": "b2d6f18d6f5e4f0d9b66a7b0e7d7f6a9f8f1c0d2a3b4c5d6e7f8091a2b3c4d5e"
    },
    {
      "provider": "SAFETY_RECALL",
      "endpointType": "operation",
      "requestKeyNames": ["serviceKey"],
      "requestKeyValues": ["REDACTED"],
      "rawResponseSha256": "c7f4e5d6a3b291807f6e5d4c3b2a1908f7e6d5c4b3a291807f6e5d4c3b2a1908"
    },
    {
      "provider": "KCS_TRADE",
      "endpointType": "operation",
      "requestKeyNames": ["serviceKey"],
      "requestKeyValues": ["REDACTED"],
      "rawResponseSha256": "d9a1b2c3e4f506172839405162738495a6b7c8d9e0f11223344556677889900a"
    }
  ]
}'

copy_or_sample "$ROOT_DIR/ml/artifacts/growgate-gate-risk-v0.1.0/model_card.md" \
  "$LATEST_ML/model-card.md" \
  '# Missing model card\nRun `python3 ml/pipelines/train_all.py` first.'
copy_or_sample "$ROOT_DIR/ml/artifacts/growgate-gate-risk-v0.1.0/metrics.json" \
  "$LATEST_ML/metrics.json" \
  '{"status":"MISSING","message":"Run python3 ml/pipelines/train_all.py"}'
copy_or_sample "$ROOT_DIR/ml/artifacts/growgate-gate-risk-v0.1.0/shap_summary.json" \
  "$LATEST_ML/shap-summary.json" \
  '{"status":"MISSING","explanationMethod":"missing"}'

python3 - "$ROOT_DIR/docs/validation/synthetic_validation_summary.md" "$VALIDATION/synthetic-validation-summary.md" <<'PY'
import pathlib
import sys

src = pathlib.Path(sys.argv[1])
dest = pathlib.Path(sys.argv[2])
text = src.read_text(encoding="utf-8")
text = text.replace("실제 고객 인터뷰", "실제 고객 검증")
dest.write_text(text, encoding="utf-8")
PY
python3 - "$ROOT_DIR/docs/report/sample_battery_germany.md" "$REPORT/sample-report.md" <<'PY'
import pathlib
import sys

src = pathlib.Path(sys.argv[1])
dest = pathlib.Path(sys.argv[2])
text = src.read_text(encoding="utf-8")
text = text.replace("실제 고객 인터뷰", "실제 고객 검증")
text = text.replace("자동 PDF 생성", "브라우저 인쇄 기반 PDF 보관")
text = text.replace("수출 성공 확률", "rule-based Gate 점검")
text = text.replace("AI가 최종 판단", "rule-based Gate가 최종 판단")
dest.write_text(text, encoding="utf-8")
PY

rm -f "$LATEST_PUBLIC/checksums.txt"
(cd "$OUT_DIR" && find . -type f -print0 | sort -z | xargs -0 shasum -a 256) > "$LATEST_PUBLIC/checksums.txt"
echo "[proof-pack] wrote $OUT_DIR"
