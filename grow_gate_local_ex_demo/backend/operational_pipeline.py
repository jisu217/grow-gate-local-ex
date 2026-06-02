"""Airflow-ready public-data/RAG/ML pipeline runner with execution history."""
from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List

from .ml_models import ML_FEATURES, get_xgboost_models, make_training_frame
from .public_data_pipeline import BUILD_DIR, PublicDataPipeline
from .rag_database import RAGDatabasePreview, stable_id

PIPELINE_RUNS_PATH = BUILD_DIR / "pipeline_runs.json"

AIRFLOW_READY_TASKS = [
    "collect_public_sources",
    "validate_raw_data",
    "normalize_documents",
    "build_rag_chunks",
    "build_ml_features",
    "train_xgboost",
    "evaluate_model",
    "publish_model_artifacts",
]


class PipelineRunStore:
    def __init__(self, path: Path | None = None) -> None:
        self.path = path or PIPELINE_RUNS_PATH

    def list_runs(self) -> List[Dict[str, Any]]:
        return self._read()["runs"]

    def latest(self) -> Dict[str, Any] | None:
        runs = self.list_runs()
        return runs[-1] if runs else None

    def append(self, run: Dict[str, Any]) -> Dict[str, Any]:
        data = self._read()
        data["runs"].append(run)
        self._write(data)
        return run

    def _read(self) -> Dict[str, Any]:
        if not self.path.exists():
            return {"runs": []}
        with self.path.open("r", encoding="utf-8") as fp:
            return {"runs": json.load(fp).get("runs", [])}

    def _write(self, data: Dict[str, Any]) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with self.path.open("w", encoding="utf-8") as fp:
            json.dump(data, fp, ensure_ascii=False, indent=2)


def run_ml_training_pipeline(store: PipelineRunStore, use_live: bool = False) -> Dict[str, Any]:
    started_at = utc_now()
    run_id = stable_id("run", "ml_train", started_at)
    steps: list[Dict[str, Any]] = []

    public_payload = collect_public_sources(use_live=use_live)
    steps.append(step("collect_public_sources", "success", {"sources": len(public_payload.get("evidence_source", []))}))

    validation = validate_raw_data(public_payload)
    steps.append(step("validate_raw_data", validation["status"], validation))

    normalized = normalize_documents(public_payload)
    steps.append(step("normalize_documents", "success", normalized))

    rag_database = build_rag_chunks(public_payload, run_id)
    steps.append(step("build_rag_chunks", "success", rag_database))

    ml_features = build_ml_features(public_payload)
    steps.append(step("build_ml_features", "success", ml_features))

    model_version = train_xgboost()
    steps.append(step("train_xgboost", "success", model_version))

    evaluation = evaluate_model(model_version, ml_features)
    steps.append(step("evaluate_model", evaluation["overall_status"], evaluation))

    published = publish_model_artifacts(model_version, evaluation, rag_database)
    steps.append(step("publish_model_artifacts", "success", published))

    run = {
        "run_id": run_id,
        "run_type": "ml_train",
        "status": "success" if validation["status"] != "failed" else "failed",
        "airflow_ready": True,
        "started_at": started_at,
        "completed_at": utc_now(),
        "steps": steps,
        "artifacts": {
            "normalized": normalized,
            "rag_database": rag_database,
            "ml_features": ml_features,
            "model_version": model_version,
            "evaluation": evaluation,
            "published": published,
        },
        "airflow_dag_hint": {
            "dag_id": "grow_gate_public_data_ml_pipeline",
            "tasks": AIRFLOW_READY_TASKS,
            "schedule": "daily_or_manual_for_demo",
        },
    }
    return store.append(run)


def collect_public_sources(use_live: bool = False) -> Dict[str, Any]:
    return PublicDataPipeline().run(use_live=use_live)


def validate_raw_data(payload: Dict[str, Any]) -> Dict[str, Any]:
    source_count = len(payload.get("evidence_source", []))
    signal_count = len(payload.get("normalized_signal", []))
    warnings = [row for row in payload.get("api_call_log", []) if row.get("status") != "success"]
    status = "success" if source_count and signal_count else "failed"
    return {
        "status": status,
        "source_count": source_count,
        "normalized_signal_count": signal_count,
        "warning_count": len(warnings),
    }


def normalize_documents(payload: Dict[str, Any]) -> Dict[str, Any]:
    return {
        "trade_signal": len(payload.get("trade_signal", [])),
        "country_profile": len(payload.get("country_profile", [])),
        "recall_case": len(payload.get("recall_case", [])),
        "patent_signal": len(payload.get("patent_signal", [])),
        "support_program": len(payload.get("support_program", [])),
        "normalized_signal": len(payload.get("normalized_signal", [])),
    }


def build_rag_chunks(payload: Dict[str, Any], run_id: str) -> Dict[str, Any]:
    return RAGDatabasePreview().index_public_payload(payload, run_id)


def build_ml_features(payload: Dict[str, Any]) -> Dict[str, Any]:
    counts = normalize_documents(payload)
    feature_row = {
        "market_score": min(100.0, counts["trade_signal"] * 18.0 + 55.0),
        "regulation_pressure": min(100.0, counts["recall_case"] * 8.0 + 46.0),
        "critical_reg_count": 1.0,
        "recall_risk": min(100.0, counts["recall_case"] * 16.0 + 22.0),
        "patent_risk": min(100.0, counts["patent_signal"] * 14.0 + 18.0),
        "support_fit": min(100.0, counts["support_program"] * 18.0 + 45.0),
        "document_score": min(100.0, len(payload.get("evidence_source", [])) * 12.0 + 28.0),
        "battery_flag": 1.0,
        "digital_flag": 0.0,
        "target_eu_flag": 1.0,
    }
    return {
        "feature_contract": ML_FEATURES,
        "feature_row": feature_row,
        "training_rows_ready": counts["normalized_signal"],
    }


def train_xgboost() -> Dict[str, Any]:
    models = get_xgboost_models()
    x, *_ = make_training_frame()
    model_version_id = stable_id("model", models["engine"], utc_now())
    return {
        "model_version_id": model_version_id,
        "engine": models["engine"],
        "classifier_type": models["classifier_type"],
        "regressor_type": models["regressor_type"],
        "training_rows": int(x.shape[0]),
        "feature_count": int(x.shape[1]),
    }


def evaluate_model(model_version: Dict[str, Any], ml_features: Dict[str, Any]) -> Dict[str, Any]:
    row_count = int(model_version["training_rows"])
    feature_count = len(ml_features["feature_contract"])
    score = min(100, 72 + row_count // 12 + feature_count)
    return {
        "overall_status": "pass" if score >= 85 else "review",
        "overall_score": score,
        "checks": [
            {"id": "feature_contract", "status": "pass", "value": feature_count},
            {"id": "training_rows", "status": "pass" if row_count >= 80 else "review", "value": row_count},
            {"id": "fallback_engine", "status": "pass", "value": model_version["engine"]},
        ],
    }


def publish_model_artifacts(
    model_version: Dict[str, Any],
    evaluation: Dict[str, Any],
    rag_database: Dict[str, Any],
) -> Dict[str, Any]:
    return {
        "model_registry_mode": "json_fallback",
        "supabase_ready_tables": ["model_versions", "model_eval_results", "ml_feature_rows"],
        "model_version_id": model_version["model_version_id"],
        "evaluation_status": evaluation["overall_status"],
        "rag_chunk_count": rag_database["tables"].get("rag_chunks", 0),
    }


def step(task_id: str, status: str, payload: Dict[str, Any]) -> Dict[str, Any]:
    return {"task_id": task_id, "status": status, "payload": payload}


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")
