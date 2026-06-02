"""Supabase/pgvector-ready RAG database contract with JSON fallback artifacts."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any, Dict, Iterable, List

from .public_data_pipeline import BUILD_DIR

RAG_DATABASE_PATH = BUILD_DIR / "rag_database_preview.json"

RAG_TABLES = [
    {
        "name": "rag_documents",
        "purpose": "공공데이터, 제품 지식, 기업 공개정보 원문 단위 저장",
        "key_fields": ["document_id", "source_id", "title", "source_url", "retrieved_at"],
    },
    {
        "name": "rag_chunks",
        "purpose": "검색 가능한 문단·근거 단위 청크 저장",
        "key_fields": ["chunk_id", "document_id", "chunk_text", "metadata", "token_count"],
    },
    {
        "name": "rag_embeddings",
        "purpose": "pgvector embedding 저장 및 유사도 검색",
        "key_fields": ["embedding_id", "chunk_id", "embedding vector(384)", "model_name"],
    },
    {
        "name": "data_sources",
        "purpose": "공식 출처, 갱신 주기, 신뢰도, 화면 연결 기준 관리",
        "key_fields": ["source_id", "provider", "official_url", "update_cycle", "confidence"],
    },
    {
        "name": "retrieval_runs",
        "purpose": "질의별 검색 결과, 점수, 사용된 chunk 기록",
        "key_fields": ["retrieval_run_id", "query", "top_k", "created_at"],
    },
    {
        "name": "agent_outputs",
        "purpose": "AI Agent 답변, fallback 여부, prompt version, citations 기록",
        "key_fields": ["agent_output_id", "provider", "used_fallback", "citations", "created_at"],
    },
    {
        "name": "eval_cases",
        "purpose": "RAG/프롬프트 harness 평가 케이스와 결과 관리",
        "key_fields": ["eval_case_id", "scenario", "expected_citations", "status"],
    },
]


def rag_schema() -> Dict[str, Any]:
    return {
        "storage_mode": "json_fallback_supabase_ready",
        "recommended_provider": "Supabase Free Postgres",
        "vector_extension": "pgvector",
        "embedding_dimension": 384,
        "tables": RAG_TABLES,
        "migration_notes": [
            "enable extension vector;",
            "rag_embeddings.embedding은 vector(384)로 생성",
            "공모전 데모는 JSON fallback을 유지하고 Supabase 연결 시 동일 테이블명으로 이관",
        ],
    }


class RAGDatabasePreview:
    """Builds table-shaped RAG artifacts without requiring a hosted database."""

    def __init__(self, path: Path | None = None) -> None:
        self.path = path or RAG_DATABASE_PATH

    def index_public_payload(self, payload: Dict[str, Any], run_id: str) -> Dict[str, Any]:
        documents = self._documents(payload, run_id)
        chunks = self._chunks(documents)
        embeddings = self._embeddings(chunks)
        database = {
            "schema": rag_schema(),
            "run_id": run_id,
            "tables": {
                "rag_documents": documents,
                "rag_chunks": chunks,
                "rag_embeddings": embeddings,
                "data_sources": payload.get("evidence_source", []),
                "retrieval_runs": [],
                "agent_outputs": [],
                "eval_cases": [],
            },
        }
        self._write(database)
        return {
            "path": str(self.path),
            "tables": {name: len(rows) for name, rows in database["tables"].items()},
        }

    def _documents(self, payload: Dict[str, Any], run_id: str) -> List[Dict[str, Any]]:
        docs: list[Dict[str, Any]] = []
        for index, item in enumerate(payload.get("normalized_signal", []), start=1):
            source_id = item.get("source_id", "unknown")
            table = item.get("target_table", "normalized_signal")
            text = json.dumps(item.get("payload", {}), ensure_ascii=False)
            document_id = stable_id("doc", run_id, source_id, table, str(index))
            docs.append(
                {
                    "document_id": document_id,
                    "source_id": source_id,
                    "title": f"{table} 근거 {index}",
                    "source_url": source_url(payload.get("evidence_source", []), source_id),
                    "retrieved_at": item.get("payload", {}).get("retrieved_at") or payload.get("metadata", {}).get("completed_at"),
                    "document_type": table,
                    "content": text,
                    "metadata": {"screen_ids": item.get("screen_ids", []), "run_id": run_id},
                }
            )
        return docs

    def _chunks(self, documents: Iterable[Dict[str, Any]]) -> List[Dict[str, Any]]:
        chunks: list[Dict[str, Any]] = []
        for doc in documents:
            text = doc["content"]
            chunk_texts = [text[i : i + 640] for i in range(0, max(1, len(text)), 640)] or [text]
            for index, chunk_text in enumerate(chunk_texts, start=1):
                chunks.append(
                    {
                        "chunk_id": stable_id("chunk", doc["document_id"], str(index)),
                        "document_id": doc["document_id"],
                        "chunk_index": index,
                        "chunk_text": chunk_text,
                        "token_count": max(1, len(chunk_text.split())),
                        "metadata": doc["metadata"],
                    }
                )
        return chunks

    def _embeddings(self, chunks: Iterable[Dict[str, Any]]) -> List[Dict[str, Any]]:
        rows: list[Dict[str, Any]] = []
        for chunk in chunks:
            rows.append(
                {
                    "embedding_id": stable_id("emb", chunk["chunk_id"]),
                    "chunk_id": chunk["chunk_id"],
                    "model_name": "demo-hash-embedding-384",
                    "embedding_preview": hash_embedding_preview(chunk["chunk_text"]),
                }
            )
        return rows

    def _write(self, payload: Dict[str, Any]) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with self.path.open("w", encoding="utf-8") as fp:
            json.dump(payload, fp, ensure_ascii=False, indent=2)


def source_url(evidence_sources: List[Dict[str, Any]], source_id: str) -> str:
    for source in evidence_sources:
        if source.get("evidence_id") == source_id:
            return source.get("url") or ""
    return ""


def hash_embedding_preview(text: str, size: int = 12) -> List[float]:
    digest = hashlib.sha256(text.encode("utf-8")).digest()
    return [round((digest[index % len(digest)] / 255.0) * 2 - 1, 4) for index in range(size)]


def stable_id(prefix: str, *parts: str) -> str:
    digest = hashlib.sha1("|".join(parts).encode("utf-8")).hexdigest()[:14]
    return f"{prefix}-{digest}"
