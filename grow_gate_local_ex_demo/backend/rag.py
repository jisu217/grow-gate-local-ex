"""Deterministic RAG layer for screen-level AI Agent evidence.

The demo keeps retrieval local and auditable: source records are normalized into
RAG documents, filtered by metadata, then ranked with lexical similarity. The
same contract can be backed by embeddings or a vector DB later.
"""
from __future__ import annotations

from typing import Any, Dict, Iterable, List, Sequence

from .data_connectors import PipelineJob, PublicDataConnector, normalize_hs_code
from .ml_models import match_tags, token_similarity, tokenize
from .schemas import EvidenceDocument, ProductInput


class DemoRAGIndex:
    """Small metadata-first retrieval index built from the local data facade."""

    def __init__(self, connector: PublicDataConnector | None = None) -> None:
        self.connector = connector or PublicDataConnector()
        self.documents = self._build_documents()

    def _build_documents(self) -> List[Dict[str, Any]]:
        pipeline = PipelineJob(self.connector)
        documents: List[Dict[str, Any]] = []

        for doc in pipeline.build_regulation_documents():
            metadata = doc["metadata"]
            trigger = metadata.get("triggers", {})
            documents.append(
                {
                    "doc_id": f"reg-{metadata['id']}",
                    "source_type": "regulation",
                    "title": metadata["title"],
                    "source_url": metadata.get("source_url"),
                    "chunk_text": doc["text"],
                    "regions": trigger.get("regions", []),
                    "features": trigger.get("features", []),
                    "hs_prefixes": trigger.get("hs_prefixes", []),
                    "metadata": metadata,
                }
            )

        for doc in pipeline.build_recall_documents():
            metadata = doc["metadata"]
            documents.append(
                {
                    "doc_id": f"recall-{metadata['id']}",
                    "source_type": "recall",
                    "title": metadata["product"],
                    "source_url": metadata.get("source_url"),
                    "chunk_text": doc["text"],
                    "regions": metadata.get("regions", ["ALL"]),
                    "features": [metadata.get("category", "")],
                    "hs_prefixes": metadata.get("hs_prefixes", []),
                    "metadata": metadata,
                }
            )

        for item in self.connector.fetch_patents():
            text = " ".join(
                [
                    item.get("keyword", ""),
                    item.get("korean_keyword", ""),
                    " ".join(item.get("ipc", [])),
                    " ".join(item.get("avoidance_hints", [])),
                ]
            )
            documents.append(
                {
                    "doc_id": f"patent-{item['keyword']}",
                    "source_type": "patent",
                    "title": item.get("korean_keyword") or item.get("keyword", ""),
                    "source_url": item.get("source_url"),
                    "chunk_text": text,
                    "regions": item.get("regions", ["ALL"]),
                    "features": item.get("fit_tags", item.get("keywords", [])),
                    "hs_prefixes": item.get("hs_prefixes", []),
                    "metadata": item,
                }
            )

        for item in self.connector.fetch_support_programs():
            text = " ".join(
                [
                    item.get("name", ""),
                    item.get("benefit", ""),
                    item.get("priority_rule", ""),
                    " ".join(item.get("fit_tags", [])),
                ]
            )
            documents.append(
                {
                    "doc_id": f"support-{item['id']}",
                    "source_type": "support",
                    "title": item.get("name", ""),
                    "source_url": item.get("source_url"),
                    "chunk_text": text,
                    "regions": item.get("regions", ["ALL"]),
                    "features": item.get("fit_tags", []),
                    "hs_prefixes": item.get("hs_prefixes", []),
                    "metadata": item,
                }
            )

        return documents

    def search(
        self,
        request: ProductInput,
        *,
        source_types: Sequence[str],
        query: str,
        target_region: str,
        top_k: int = 3,
    ) -> List[EvidenceDocument]:
        source_set = {item.lower() for item in source_types}
        query_text = f"{request.product_name} {request.hs_code} {' '.join(request.features)} {query}"
        ranked: List[tuple[float, Dict[str, Any], List[str]]] = []
        for doc in self.documents:
            if doc["source_type"] not in source_set:
                continue
            metadata_score = self._metadata_score(doc, request, target_region)
            if metadata_score <= 0:
                continue
            text_score = token_similarity(query_text, f"{doc['title']} {doc['chunk_text']}")
            matched_terms = self._matched_terms(query_text, doc)
            score = min(1.0, metadata_score * 0.58 + text_score * 0.36 + min(len(matched_terms), 4) * 0.015)
            ranked.append((score, doc, matched_terms))

        ranked.sort(key=lambda row: row[0], reverse=True)
        return [
            EvidenceDocument(
                doc_id=doc["doc_id"],
                source_type=doc["source_type"],
                title=doc["title"],
                source_url=doc.get("source_url"),
                confidence=round(score, 3),
                matched_terms=terms,
                chunk_text=doc["chunk_text"][:420],
            )
            for score, doc, terms in ranked[:top_k]
        ]

    def context_for(self, request: ProductInput, target_region: str) -> Dict[str, List[Dict[str, Any]]]:
        return {
            "regulation_documents": [item.model_dump() for item in self.search(request, source_types=["regulation"], query="regulation certification test", target_region=target_region, top_k=5)],
            "recall_documents": [item.model_dump() for item in self.search(request, source_types=["recall"], query="recall hazard safety test", target_region=target_region, top_k=4)],
            "patent_documents": [item.model_dump() for item in self.search(request, source_types=["patent"], query="patent FTO IPC avoidance", target_region=target_region, top_k=3)],
            "support_documents": [item.model_dump() for item in self.search(request, source_types=["support"], query="support export certification subsidy", target_region=target_region, top_k=4)],
        }

    def _metadata_score(self, doc: Dict[str, Any], request: ProductInput, target_region: str) -> float:
        regions = doc.get("regions", [])
        region_hit = target_region in regions or "ALL" in regions or request.target_market in regions
        feature_score = match_tags(request.features, [item for item in doc.get("features", []) if item])
        hs_prefixes = doc.get("hs_prefixes", [])
        hs_hit = bool(hs_prefixes) and any(normalize_hs_code(request.hs_code).startswith(prefix) for prefix in hs_prefixes)
        if doc["source_type"] == "regulation" and not region_hit:
            return 0.0
        if not (region_hit or feature_score > 0 or hs_hit):
            return 0.0
        return min(1.0, (0.34 if region_hit else 0.0) + feature_score * 0.36 + (0.30 if hs_hit else 0.0))

    def _matched_terms(self, query: str, doc: Dict[str, Any]) -> List[str]:
        doc_tokens = set(tokenize(f"{doc['title']} {doc['chunk_text']}"))
        terms: List[str] = []
        for token in tokenize(query):
            if token in doc_tokens and token not in terms:
                terms.append(token)
        return terms[:8]


def attach_evidence_by_title(items: Iterable[Any], evidence: Sequence[EvidenceDocument]) -> None:
    """Attach the strongest title/text-matching evidence to each Pydantic item."""
    for item in items:
        query = " ".join(str(getattr(item, attr, "")) for attr in ("title", "product", "keyword", "korean_keyword", "name"))
        ranked = sorted(
            evidence,
            key=lambda doc: token_similarity(query, f"{doc.title} {doc.chunk_text}"),
            reverse=True,
        )
        item.evidence_documents = list(ranked[:2])
