"""Local SQLite database for realistic demo data and simulation persistence."""
from __future__ import annotations

import json
import sqlite3
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict

from .operational_pipeline import build_ml_features, collect_public_sources, evaluate_model, normalize_documents, train_xgboost
from .product_knowledge import ProductKnowledgeStore
from .public_data_pipeline import ROOT
from .rag_database import RAGDatabasePreview, stable_id

LOCAL_DEMO_DB_PATH = ROOT / "data" / "grow_gate_demo.db"


class LocalDemoDatabase:
    """Small SQLite database used as the demo's actual local persistence layer."""

    TABLES = [
        "product_knowledge",
        "rag_documents",
        "rag_chunks",
        "rag_embeddings",
        "pipeline_runs",
        "model_versions",
        "model_eval_results",
        "analysis_reports",
        "search_runs",
    ]

    def __init__(self, path: Path | None = None) -> None:
        self.path = path or LOCAL_DEMO_DB_PATH

    def seed(self) -> Dict[str, Any]:
        self.initialize()
        self.reset_seed_tables()
        product_store = ProductKnowledgeStore(self.path.with_name("product_knowledge_seed.json"))
        product = product_store.upsert(default_product_knowledge_payload())
        self.upsert_product_knowledge(product)

        run_id = "seed-public-data-rag-ml-v1"
        public_payload = collect_public_sources(use_live=False)
        rag_preview_path = self.path.with_name("rag_database_preview_seed.json")
        rag_preview = RAGDatabasePreview(rag_preview_path)
        rag_preview.index_public_payload(public_payload, run_id)
        rag_payload = json.loads(rag_preview_path.read_text(encoding="utf-8"))

        self.insert_rag_payload(rag_payload)
        model_version = train_xgboost()
        model_version["model_version_id"] = "model-xgboost-seed-v1"
        ml_features = build_ml_features(public_payload)
        evaluation = evaluate_model(model_version, ml_features)
        self.insert_pipeline_run(
            {
                "run_id": run_id,
                "run_type": "seed_public_data_ml",
                "status": "success",
                "payload": {
                    "normalized": normalize_documents(public_payload),
                    "rag_tables": {name: len(rows) for name, rows in rag_payload["tables"].items()},
                    "ml_features": ml_features,
                },
            }
        )
        self.insert_model_version(model_version)
        self.insert_model_eval(model_version["model_version_id"], evaluation)
        return {"status": "seeded", "database_path": str(self.path), "counts": self.counts()}

    def initialize(self) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with self.connect() as conn:
            conn.executescript(
                """
                CREATE TABLE IF NOT EXISTS product_knowledge (
                  product_id TEXT PRIMARY KEY,
                  company_name TEXT NOT NULL,
                  product_name TEXT NOT NULL,
                  hs_code TEXT NOT NULL,
                  target_market TEXT NOT NULL,
                  payload_json TEXT NOT NULL,
                  updated_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS rag_documents (
                  document_id TEXT PRIMARY KEY,
                  source_id TEXT,
                  title TEXT,
                  source_url TEXT,
                  document_type TEXT,
                  content TEXT,
                  payload_json TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS rag_chunks (
                  chunk_id TEXT PRIMARY KEY,
                  document_id TEXT,
                  chunk_text TEXT,
                  token_count INTEGER,
                  payload_json TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS rag_embeddings (
                  embedding_id TEXT PRIMARY KEY,
                  chunk_id TEXT,
                  model_name TEXT,
                  embedding_preview TEXT,
                  payload_json TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS pipeline_runs (
                  run_id TEXT PRIMARY KEY,
                  run_type TEXT,
                  status TEXT,
                  payload_json TEXT NOT NULL,
                  created_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS model_versions (
                  model_version_id TEXT PRIMARY KEY,
                  engine TEXT,
                  classifier_type TEXT,
                  regressor_type TEXT,
                  payload_json TEXT NOT NULL,
                  created_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS model_eval_results (
                  eval_id TEXT PRIMARY KEY,
                  model_version_id TEXT,
                  overall_status TEXT,
                  overall_score INTEGER,
                  payload_json TEXT NOT NULL,
                  created_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS analysis_reports (
                  report_id TEXT PRIMARY KEY,
                  company_name TEXT,
                  product_name TEXT,
                  readiness_score INTEGER,
                  review_status TEXT,
                  payload_json TEXT NOT NULL,
                  created_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS search_runs (
                  search_run_id TEXT PRIMARY KEY,
                  query TEXT,
                  ranking_strategy TEXT,
                  payload_json TEXT NOT NULL,
                  created_at TEXT NOT NULL
                );
                """
            )

    def reset_seed_tables(self) -> None:
        """Keep seed data idempotent while preserving report/search run history."""
        with self.connect() as conn:
            for table in ("rag_embeddings", "rag_chunks", "rag_documents", "model_eval_results", "model_versions"):
                conn.execute(f"DELETE FROM {table}")
            conn.execute("DELETE FROM pipeline_runs WHERE run_type = ?", ("seed_public_data_ml",))

    def status(self) -> Dict[str, Any]:
        self.initialize()
        return {"database_path": str(self.path), "counts": self.counts()}

    def counts(self) -> Dict[str, int]:
        self.initialize()
        with self.connect() as conn:
            return {table: int(conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]) for table in self.TABLES}

    def upsert_product_knowledge(self, record: Dict[str, Any]) -> None:
        with self.connect() as conn:
            conn.execute(
                """
                INSERT INTO product_knowledge(product_id, company_name, product_name, hs_code, target_market, payload_json, updated_at)
                VALUES(?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(product_id) DO UPDATE SET
                  company_name=excluded.company_name,
                  product_name=excluded.product_name,
                  hs_code=excluded.hs_code,
                  target_market=excluded.target_market,
                  payload_json=excluded.payload_json,
                  updated_at=excluded.updated_at
                """,
                (
                    record["product_id"],
                    record["company_name"],
                    record["product_name"],
                    record["hs_code"],
                    record["target_market"],
                    to_json(record),
                    utc_now(),
                ),
            )

    def insert_rag_payload(self, payload: Dict[str, Any]) -> None:
        tables = payload.get("tables", {})
        with self.connect() as conn:
            for doc in tables.get("rag_documents", []):
                conn.execute(
                    """
                    INSERT OR REPLACE INTO rag_documents(document_id, source_id, title, source_url, document_type, content, payload_json)
                    VALUES(?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        doc["document_id"],
                        doc.get("source_id"),
                        doc.get("title"),
                        doc.get("source_url"),
                        doc.get("document_type"),
                        doc.get("content"),
                        to_json(doc),
                    ),
                )
            for chunk in tables.get("rag_chunks", []):
                conn.execute(
                    "INSERT OR REPLACE INTO rag_chunks(chunk_id, document_id, chunk_text, token_count, payload_json) VALUES(?, ?, ?, ?, ?)",
                    (chunk["chunk_id"], chunk.get("document_id"), chunk.get("chunk_text"), chunk.get("token_count", 0), to_json(chunk)),
                )
            for embedding in tables.get("rag_embeddings", []):
                conn.execute(
                    "INSERT OR REPLACE INTO rag_embeddings(embedding_id, chunk_id, model_name, embedding_preview, payload_json) VALUES(?, ?, ?, ?, ?)",
                    (
                        embedding["embedding_id"],
                        embedding.get("chunk_id"),
                        embedding.get("model_name"),
                        to_json(embedding.get("embedding_preview", [])),
                        to_json(embedding),
                    ),
                )

    def insert_pipeline_run(self, run: Dict[str, Any]) -> None:
        with self.connect() as conn:
            conn.execute(
                "INSERT OR REPLACE INTO pipeline_runs(run_id, run_type, status, payload_json, created_at) VALUES(?, ?, ?, ?, ?)",
                (run["run_id"], run.get("run_type"), run.get("status"), to_json(run), utc_now()),
            )

    def insert_model_version(self, model_version: Dict[str, Any]) -> None:
        with self.connect() as conn:
            conn.execute(
                """
                INSERT OR REPLACE INTO model_versions(model_version_id, engine, classifier_type, regressor_type, payload_json, created_at)
                VALUES(?, ?, ?, ?, ?, ?)
                """,
                (
                    model_version["model_version_id"],
                    model_version.get("engine"),
                    model_version.get("classifier_type"),
                    model_version.get("regressor_type"),
                    to_json(model_version),
                    utc_now(),
                ),
            )

    def insert_model_eval(self, model_version_id: str, evaluation: Dict[str, Any]) -> None:
        eval_id = stable_id("eval", model_version_id, evaluation.get("overall_status", "review"))
        with self.connect() as conn:
            conn.execute(
                "INSERT OR REPLACE INTO model_eval_results(eval_id, model_version_id, overall_status, overall_score, payload_json, created_at) VALUES(?, ?, ?, ?, ?, ?)",
                (
                    eval_id,
                    model_version_id,
                    evaluation.get("overall_status"),
                    int(evaluation.get("overall_score", 0)),
                    to_json(evaluation),
                    utc_now(),
                ),
            )

    def insert_analysis_report(self, report: Dict[str, Any]) -> Dict[str, Any]:
        report_id = report.get("report_id") or stable_id("report", report.get("company_name", ""), report.get("product_name", ""), utc_now())
        record = {**report, "report_id": report_id}
        with self.connect() as conn:
            conn.execute(
                """
                INSERT OR REPLACE INTO analysis_reports(report_id, company_name, product_name, readiness_score, review_status, payload_json, created_at)
                VALUES(?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    report_id,
                    record.get("company_name"),
                    record.get("product_name"),
                    int(record.get("readiness_score", 0)),
                    record.get("review_status"),
                    to_json(record),
                    utc_now(),
                ),
            )
        return record

    def insert_search_run(self, search_run: Dict[str, Any]) -> Dict[str, Any]:
        search_run_id = search_run.get("search_run_id") or stable_id("search", search_run.get("query", ""), utc_now())
        record = {**search_run, "search_run_id": search_run_id}
        with self.connect() as conn:
            conn.execute(
                "INSERT OR REPLACE INTO search_runs(search_run_id, query, ranking_strategy, payload_json, created_at) VALUES(?, ?, ?, ?, ?)",
                (
                    search_run_id,
                    record.get("query"),
                    record.get("ranking_strategy"),
                    to_json(record),
                    utc_now(),
                ),
            )
        return record

    def connect(self) -> sqlite3.Connection:
        conn = sqlite3.connect(self.path)
        conn.row_factory = sqlite3.Row
        return conn


def default_product_knowledge_payload() -> Dict[str, Any]:
    return {
        "company_name": "경남 배터리테크",
        "product_name": "리튬이온 배터리팩",
        "hs_code": "850760",
        "target_market": "Germany / EU",
        "product_family": "industrial battery pack",
        "features": ["battery", "electronics", "industrial"],
        "certifications": ["KC", "UN38.3", "IEC 62133"],
        "materials": ["NCM cell", "BMS", "aluminum case", "cell holder"],
        "buyer_requirements": ["EU battery passport", "carbon footprint", "MSDS", "DoC"],
        "recall_keywords": ["thermal runaway", "short circuit", "overheating", "drop impact"],
        "fto_keywords": ["battery pack cooling", "BMS balancing", "thermal protection"],
        "documents": [
            {
                "title": "UN38.3 시험성적서",
                "source_url": "https://example.test/un38",
                "document_type": "test_report",
                "summary": "리튬이온 배터리팩 항공·해상 운송 안전 시험 근거",
                "reliability": "high",
            },
            {
                "title": "BOM 및 소재정보",
                "source_url": "https://example.test/bom",
                "document_type": "bom",
                "summary": "셀, BMS, 케이스, 보호회로 구성 정보",
                "reliability": "medium",
            },
        ],
        "owner": "품질·인증팀",
        "status": "active",
    }


def to_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True)


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")
