#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/artifacts/submission"
STAMP="$(date +%Y%m%d%H%M%S)"
OUT_ZIP="${OUT_DIR}/growgate-submission-${STAMP}.zip"

mkdir -p "$OUT_DIR"

cd "$ROOT_DIR"
zip -qr "$OUT_ZIP" . \
  -x ".git/*" \
  -x ".DS_Store" \
  -x "*/.DS_Store" \
  -x ".env" \
  -x "*/.env" \
  -x ".gstack/*" \
  -x ".superpowers/*" \
  -x ".pytest_cache/*" \
  -x "node_modules/*" \
  -x "frontend/FE-02/node_modules/*" \
  -x "frontend/FE-02/dist/*" \
  -x "backend/BE-02/build/*" \
  -x "backend/BE-02/.gradle/*" \
  -x "**/.gradle-home/*" \
  -x "**/.venv/*" \
  -x "**/.venv312/*" \
  -x ".venv-*/*" \
  -x ".venv-aws/*" \
  -x "**/__pycache__/*" \
  -x "**/.pytest_cache/*" \
  -x "artifacts/legacy-backups/*" \
  -x "artifacts/presentation/*" \
  -x "artifacts/submission/*" \
  -x "*.zip"

echo "$OUT_ZIP"
