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
XGBOOST_CATALOG_PATH = Path(__file__).resolve().parents[1] / "data" / "xgboost_data_collection_catalog.json"


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


@lru_cache(maxsize=1)
def load_xgboost_data_catalog() -> Dict[str, Any]:
    """Load the XGBoost data-collection catalog normalized from the workbook."""
    with XGBOOST_CATALOG_PATH.open("r", encoding="utf-8") as fp:
        catalog = json.load(fp)
    catalog.setdefault("session_playbooks", build_session_playbooks(catalog))
    return catalog


def build_session_playbooks(catalog: Dict[str, Any]) -> List[Dict[str, Any]]:
    """Create copy-ready collaboration session templates for data work."""
    meta = catalog.get("metadata", {})
    total_sources = meta.get("total_sources", len(catalog.get("sources", [])))
    return [
        {
            "session_id": "excel-catalog-update",
            "title": "엑셀 데이터 카탈로그 변경 세션",
            "owner_role": "데이터 기획/PM",
            "goal": "xgboost_data_collection_catalog.xlsx 변경 내용을 JSON 명세, API, 데이터 명세 화면까지 일관되게 반영합니다.",
            "primary_files": [
                "grow_gate_local_ex_demo/data/xgboost_data_collection_catalog.json",
                "grow_gate_local_ex_demo/backend/data_connectors.py",
                "grow_gate_local_ex_demo/tests/test_agent.py",
                "grow_gate_local_ex_demo/frontend/team.js",
            ],
            "expected_outputs": [
                "엑셀의 데이터_카탈로그/피처_매핑/수집_계획/내부라벨_필수 시트가 JSON으로 정규화됨",
                "GET /xgboost-data/catalog 응답의 total_sources, feature_mappings, collection_plan, internal_labels가 최신값과 일치",
                "데이터 명세 화면에서 변경된 출처와 피처 후보가 검색/검토 가능",
            ],
            "acceptance_checks": [
                ".venv312/bin/python -m pytest tests/test_agent.py",
                ".venv312/bin/python -m backend.run_pipeline",
                "node --check grow_gate_local_ex_demo/frontend/team.js",
            ],
            "kickoff_prompt": (
                "xgboost_data_collection_catalog.xlsx가 변경되었습니다. "
                "엑셀의 데이터_카탈로그, 피처_매핑, 수집_계획, 내부라벨_필수 시트를 읽어 "
                "grow_gate_local_ex_demo/data/xgboost_data_collection_catalog.json과 관련 API/프론트 명세 화면을 최신화해 주세요. "
                f"현재 기준 카탈로그는 {total_sources}개 소스입니다. 테스트와 파이프라인 검증까지 실행해 주세요."
            ),
        },
        {
            "session_id": "external-data-collection",
            "title": "외부 데이터 수집 세션",
            "owner_role": "데이터 엔지니어",
            "goal": "UN Comtrade, WTO ePing, EU Safety Gate, CPSC, openFDA 등 외부 데이터 후보를 안전한 connector 후보와 seed fallback으로 연결합니다.",
            "primary_files": [
                "grow_gate_local_ex_demo/backend/public_data_pipeline.py",
                "grow_gate_local_ex_demo/data/xgboost_data_collection_catalog.json",
                "grow_gate_local_ex_demo/data/public_data_sources.json",
                "grow_gate_local_ex_demo/docs/architecture.md",
            ],
            "expected_outputs": [
                "external_fetch_candidates에서 선택한 E-* 소스의 수집 방식, 인증 필요 여부, target_table이 명확함",
                "live 호출은 허용 호스트/HTTPS/키 존재 여부를 검증하고 실패 시 seed로 fallback",
                "수집 결과가 feature 후보와 model_target에 연결되는 normalized_signal 또는 별도 serving table로 노출",
            ],
            "acceptance_checks": [
                ".venv312/bin/python -m pytest tests/test_agent.py",
                ".venv312/bin/python -m backend.run_pipeline",
                "curl -s http://127.0.0.1:8000/xgboost-data/catalog",
            ],
            "kickoff_prompt": (
                "외부 데이터 수집 세션입니다. xgboost 카탈로그의 E-* 후보 중 우선순위 A/B를 검토해 "
                "외부 API/웹/다운로드 수집 후보를 public_data_pipeline에 안전하게 연결해 주세요. "
                "네트워크/키가 없는 환경에서도 seed fallback으로 같은 스키마가 유지되어야 하며, "
                "external_fetch_candidates와 XGBoost 피처 후보가 화면/리포트에서 추적 가능해야 합니다."
            ),
        },
        {
            "session_id": "public-data-enhancement",
            "title": "공공데이터 강화 세션",
            "owner_role": "공공데이터/백엔드 담당",
            "goal": "관세청, KOTRA, 기업마당, Safety Korea, K-SURE, ECOS 등 P-* 공공데이터를 제품·국가·HS Code 기반 분석 피처로 강화합니다.",
            "primary_files": [
                "grow_gate_local_ex_demo/data/public_data_sources.json",
                "grow_gate_local_ex_demo/backend/public_data_pipeline.py",
                "grow_gate_local_ex_demo/backend/ml_models.py",
                "grow_gate_local_ex_demo/frontend/data-catalog.html",
            ],
            "expected_outputs": [
                "P-* 공공데이터 후보가 trade/regulation/recall/support/cost_logistics 등 target_table로 정규화됨",
                "시장성, 규제 압력, 인증 리드타임, 지원사업 적합도에 연결되는 파생 변수가 명세에 남음",
                "API 키가 없어도 seed 데이터로 시연 결과와 데이터 명세 화면이 깨지지 않음",
            ],
            "acceptance_checks": [
                ".venv312/bin/python -m pytest",
                ".venv312/bin/python -m backend.run_pipeline",
                "node --check grow_gate_local_ex_demo/frontend/team.js",
            ],
            "kickoff_prompt": (
                "공공데이터 강화 세션입니다. xgboost 카탈로그의 P-* 후보를 기준으로 "
                "관세청/KOTRA/기업마당/Safety Korea/K-SURE/ECOS 데이터를 제품, HS Code, 국가, 기간 단위로 정규화해 주세요. "
                "모든 변경은 seed fallback, 출처 URL, update_cycle, feature_candidates, leakage guardrail을 유지해야 합니다."
            ),
        },
    ]


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
            "xgboost_data_catalog": load_xgboost_data_catalog(),
        }
