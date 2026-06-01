"""Run the local ETL/RAG indexing preview.

Usage:
    python -m backend.run_pipeline
"""
from __future__ import annotations

import json
from pathlib import Path

from .data_connectors import PipelineJob
from .public_data_pipeline import run_and_write


def main() -> None:
    output_dir = Path(__file__).resolve().parents[1] / "data" / "build"
    output_dir.mkdir(parents=True, exist_ok=True)
    payload = PipelineJob().run()
    output_path = output_dir / "rag_documents_preview.json"
    with output_path.open("w", encoding="utf-8") as fp:
        json.dump(payload, fp, ensure_ascii=False, indent=2)
    print(f"Wrote {output_path}")
    print(f"Regulation docs: {len(payload['regulation_documents'])}")
    print(f"Recall docs: {len(payload['recall_documents'])}")
    public_data_path, public_data_payload = run_and_write(use_live=False)
    print(f"Wrote {public_data_path}")
    print(f"Public data sources: {len(public_data_payload['evidence_source'])}")
    print(f"Normalized signals: {len(public_data_payload['normalized_signal'])}")


if __name__ == "__main__":
    main()
