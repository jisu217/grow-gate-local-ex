"""Data connector layer for GROW-GATE Local EX.

This demo uses a local JSON dataset so that the service can run without API keys.
In production, each method can be replaced by public-data API calls, caching, and
RAG indexing jobs.
"""
from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path
from typing import Any, Dict, List

DATA_PATH = Path(__file__).resolve().parents[1] / "data" / "sample_datasets.json"
PUBLIC_DATA_SOURCE_PATH = Path(__file__).resolve().parents[1] / "data" / "public_data_sources.json"


@lru_cache(maxsize=1)
def load_dataset() -> Dict[str, Any]:
    """Load the local demo dataset once."""
    with DATA_PATH.open("r", encoding="utf-8") as fp:
        return json.load(fp)


@lru_cache(maxsize=1)
def load_public_data_sources() -> Dict[str, Any]:
    """Load the public-data source catalog used by the screen evidence map."""
    with PUBLIC_DATA_SOURCE_PATH.open("r", encoding="utf-8") as fp:
        return json.load(fp)


def normalize_hs_code(hs_code: str) -> str:
    """Return digits-only HS code prefix suitable for rule matching."""
    return "".join(ch for ch in (hs_code or "") if ch.isdigit())


class PublicDataConnector:
    """Facade for trade, regulation, recall, patent, and support datasets.

    Replace the private `_dataset` accessors with real API clients when moving
    beyond the demo. Keeping this facade stable lets the Agent layer stay the same.
    """

    def __init__(self) -> None:
        self._dataset = load_dataset()

    def fetch_markets(self) -> List[Dict[str, Any]]:
        return list(self._dataset["markets"])

    def fetch_target_market(self, target_market: str) -> Dict[str, Any]:
        markets = self.fetch_markets()
        query = (target_market or "").strip().lower()
        for market in markets:
            if query in market["name"].lower() or market["name"].lower() in query:
                return market
            if query == market["code"].lower() or query == market["region"].lower():
                return market
        # Fallback to Germany / EU because it is the default demo path.
        return markets[0]

    def fetch_regulations(self) -> List[Dict[str, Any]]:
        return list(self._dataset["regulations"])

    def fetch_recalls(self) -> List[Dict[str, Any]]:
        return list(self._dataset["recalls"])

    def fetch_patents(self) -> List[Dict[str, Any]]:
        return list(self._dataset["patents"])

    def fetch_support_programs(self) -> List[Dict[str, Any]]:
        return list(self._dataset["support_programs"])

    def fetch_regions(self) -> List[Dict[str, Any]]:
        return list(self._dataset["regions"])

    def fetch_region_profile(self, region_name: str) -> Dict[str, Any]:
        query = (region_name or "").strip().lower()
        for region in self.fetch_regions():
            if query == region["name"].lower():
                return region
        return {"name": region_name or "기타", "industry_focus": [], "weight": 1.0}


class PipelineJob:
    """Simple ETL-style job used by demo scripts and tests.

    It returns denormalized records that could be written to a vector DB or
    relational DB in a production pipeline.
    """

    def __init__(self, connector: PublicDataConnector | None = None) -> None:
        self.connector = connector or PublicDataConnector()

    def build_regulation_documents(self) -> List[Dict[str, Any]]:
        docs: List[Dict[str, Any]] = []
        for item in self.connector.fetch_regulations():
            text = " ".join(
                [
                    item["title"],
                    item["scope"],
                    item["action"],
                    " ".join(item.get("typical_evidence", [])),
                ]
            )
            docs.append(
                {
                    "doc_id": item["id"],
                    "source": "regulation_demo_dataset",
                    "title": item["title"],
                    "text": text,
                    "metadata": item,
                }
            )
        return docs

    def build_recall_documents(self) -> List[Dict[str, Any]]:
        docs: List[Dict[str, Any]] = []
        for item in self.connector.fetch_recalls():
            text = " ".join(
                [
                    item["product"],
                    item["hazard"],
                    " ".join(item.get("keywords", [])),
                    " ".join(item.get("recommended_tests", [])),
                ]
            )
            docs.append(
                {
                    "doc_id": item["id"],
                    "source": "recall_demo_dataset",
                    "title": item["product"],
                    "text": text,
                    "metadata": item,
                }
            )
        return docs

    def run(self) -> Dict[str, Any]:
        """Build all demo artifacts for downstream agent use."""
        source_catalog = load_public_data_sources()
        return {
            "regulation_documents": self.build_regulation_documents(),
            "recall_documents": self.build_recall_documents(),
            "markets": self.connector.fetch_markets(),
            "patents": self.connector.fetch_patents(),
            "support_programs": self.connector.fetch_support_programs(),
            "public_data_sources": source_catalog["sources"],
            "screen_data_map": source_catalog["screen_data_map"],
        }
