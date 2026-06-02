"""Product knowledge storage for AI Agent, RAG, and ML feature enrichment."""
from __future__ import annotations

import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List

from .data_connectors import normalize_hs_code
from .ml_models import match_tags, token_similarity
from .schemas import ProductInput

PRODUCT_KNOWLEDGE_PATH = Path(__file__).resolve().parents[1] / "data" / "product_knowledge.json"


class ProductKnowledgeStore:
    """JSON-backed product knowledge repository.

    The file format mirrors the database tables we would move to later:
    product knowledge records plus append-only update events.
    """

    def __init__(self, path: Path | None = None) -> None:
        self.path = path or PRODUCT_KNOWLEDGE_PATH

    def list_items(self) -> List[Dict[str, Any]]:
        data = self._read()
        return [self.enrich_record(item) for item in data["items"]]

    def upsert(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        data = self._read()
        now = self._now()
        candidate = self._normalize_payload(payload)
        product_id = candidate.get("product_id") or self._product_id(candidate)

        items = data["items"]
        existing_index = self._find_index(items, product_id, candidate)
        if existing_index >= 0:
            previous = items[existing_index]
            record = {
                **candidate,
                "product_id": previous["product_id"],
                "created_at": previous.get("created_at", now),
                "updated_at": now,
                "version": int(previous.get("version", 1)) + 1,
            }
            items[existing_index] = record
            event_type = "updated"
        else:
            record = {
                **candidate,
                "product_id": product_id,
                "created_at": now,
                "updated_at": now,
                "version": 1,
            }
            items.append(record)
            event_type = "created"

        data["events"].append(
            {
                "event_type": event_type,
                "product_id": record["product_id"],
                "version": record["version"],
                "owner": record.get("owner", "미지정"),
                "created_at": now,
            }
        )
        self._write(data)
        return self.enrich_record(record)

    def find_match(self, request: ProductInput) -> Dict[str, Any] | None:
        best_score = 0.0
        best_record: Dict[str, Any] | None = None
        request_hs = normalize_hs_code(request.hs_code)
        for record in self.list_items():
            if record.get("status") == "archived":
                continue
            score = 0.0
            if normalize_hs_code(record.get("hs_code", "")) == request_hs:
                score += 0.42
            score += token_similarity(record.get("product_name", ""), request.product_name) * 0.28
            score += token_similarity(record.get("company_name", ""), request.company_name) * 0.18
            score += match_tags(request.features, record.get("features", [])) * 0.12
            if score > best_score:
                best_score = score
                best_record = record
        if best_record and best_score >= 0.38:
            return {**best_record, "match_score": round(best_score, 3)}
        return None

    def feature_preview(self, record: Dict[str, Any]) -> Dict[str, Any]:
        certifications = record.get("certifications", [])
        documents = record.get("documents", [])
        features = {item.lower() for item in record.get("features", [])}
        reliability_weight = {"high": 1.0, "medium": 0.72, "low": 0.45}
        document_score = round(
            min(
                1.0,
                sum(reliability_weight.get(doc.get("reliability", "medium"), 0.6) for doc in documents) / 4,
            ),
            3,
        )
        feature_row = {
            "registered_knowledge_flag": 1,
            "battery_flag": 1 if "battery" in features or "배터리" in record.get("product_name", "") else 0,
            "digital_flag": 1 if features & {"digital", "iot", "software"} else 0,
            "certification_count": len(certifications),
            "rag_document_count": len(documents),
            "buyer_requirement_count": len(record.get("buyer_requirements", [])),
            "recall_keyword_count": len(record.get("recall_keywords", [])),
            "fto_keyword_count": len(record.get("fto_keywords", [])),
            "knowledge_document_score": document_score,
        }
        return {
            "feature_row": feature_row,
            "signals": {
                "certification_count": len(certifications),
                "document_count": len(documents),
                "material_count": len(record.get("materials", [])),
                "knowledge_status": record.get("status", "draft"),
            },
            "explanation": "AI Agent/RAG/ML 사전 지식으로 등록된 제품 정보를 feature row로 변환했습니다.",
        }

    def rag_pack(self, record: Dict[str, Any]) -> Dict[str, Any]:
        documents = []
        for index, doc in enumerate(record.get("documents", []), start=1):
            documents.append(
                {
                    "doc_id": f"{record['product_id']}-doc-{index}",
                    "title": doc["title"],
                    "source_url": doc.get("source_url", ""),
                    "document_type": doc.get("document_type", "evidence"),
                    "summary": doc.get("summary", ""),
                    "reliability": doc.get("reliability", "medium"),
                    "chunk_text": (
                        f"{record['company_name']} {record['product_name']} 근거 문서: "
                        f"{doc['title']} - {doc.get('summary', '')}"
                    ),
                }
            )
        return {
            "product_id": record["product_id"],
            "retrieval_key": f"{record['company_name']} {record['product_name']} {record['hs_code']}",
            "documents": documents,
            "knowledge_terms": sorted(
                set(
                    record.get("materials", [])
                    + record.get("buyer_requirements", [])
                    + record.get("recall_keywords", [])
                    + record.get("fto_keywords", [])
                )
            ),
        }

    def enrich_record(self, record: Dict[str, Any]) -> Dict[str, Any]:
        enriched = dict(record)
        enriched["feature_preview"] = self.feature_preview(enriched)
        enriched["rag_pack"] = self.rag_pack(enriched)
        return enriched

    def _read(self) -> Dict[str, Any]:
        if not self.path.exists():
            return {"items": [], "events": []}
        with self.path.open("r", encoding="utf-8") as fp:
            data = json.load(fp)
        return {"items": data.get("items", []), "events": data.get("events", [])}

    def _write(self, data: Dict[str, Any]) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with self.path.open("w", encoding="utf-8") as fp:
            json.dump(data, fp, ensure_ascii=False, indent=2)

    def _normalize_payload(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        record = {
            "product_id": payload.get("product_id"),
            "company_name": str(payload.get("company_name", "")).strip(),
            "product_name": str(payload.get("product_name", "")).strip(),
            "hs_code": normalize_hs_code(str(payload.get("hs_code", ""))),
            "target_market": str(payload.get("target_market", "")).strip(),
            "product_family": str(payload.get("product_family", "")).strip(),
            "features": self._string_list(payload.get("features", [])),
            "certifications": self._string_list(payload.get("certifications", [])),
            "materials": self._string_list(payload.get("materials", [])),
            "buyer_requirements": self._string_list(payload.get("buyer_requirements", [])),
            "recall_keywords": self._string_list(payload.get("recall_keywords", [])),
            "fto_keywords": self._string_list(payload.get("fto_keywords", [])),
            "documents": self._documents(payload.get("documents", [])),
            "owner": str(payload.get("owner", "미지정")).strip() or "미지정",
            "status": str(payload.get("status", "draft")).strip() or "draft",
            "notes": str(payload.get("notes", "")).strip(),
        }
        required = ["company_name", "product_name", "hs_code", "target_market"]
        missing = [field for field in required if not record[field]]
        if missing:
            raise ValueError(f"Missing product knowledge fields: {', '.join(missing)}")
        return record

    def _find_index(self, items: List[Dict[str, Any]], product_id: str, candidate: Dict[str, Any]) -> int:
        candidate_key = self._identity_key(candidate)
        for index, item in enumerate(items):
            if item.get("product_id") == product_id or self._identity_key(item) == candidate_key:
                return index
        return -1

    def _identity_key(self, record: Dict[str, Any]) -> str:
        return "|".join(
            [
                record.get("company_name", "").lower(),
                record.get("product_name", "").lower(),
                normalize_hs_code(record.get("hs_code", "")),
            ]
        )

    def _product_id(self, record: Dict[str, Any]) -> str:
        digest = hashlib.sha1(self._identity_key(record).encode("utf-8")).hexdigest()[:12]
        return f"pk-{digest}"

    def _documents(self, values: Any) -> List[Dict[str, Any]]:
        documents: list[Dict[str, Any]] = []
        for value in values or []:
            title = str(value.get("title", "")).strip()
            if not title:
                continue
            documents.append(
                {
                    "title": title,
                    "source_url": str(value.get("source_url", "")).strip(),
                    "document_type": str(value.get("document_type", "evidence")).strip() or "evidence",
                    "summary": str(value.get("summary", "")).strip(),
                    "reliability": str(value.get("reliability", "medium")).strip() or "medium",
                }
            )
        return documents

    def _string_list(self, values: Any) -> List[str]:
        cleaned: list[str] = []
        for value in values or []:
            item = str(value).strip()
            if item and item not in cleaned:
                cleaned.append(item)
        return cleaned

    def _now(self) -> str:
        return datetime.now(timezone.utc).isoformat()
