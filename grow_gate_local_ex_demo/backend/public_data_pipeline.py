"""Public-data collection and normalization pipeline for GROW-GATE Local EX.

The pipeline is intentionally runnable without credentials. When official API
keys are present it records live-call attempts; otherwise it emits seed records
with the same normalized contracts so the demo and tests remain deterministic.
"""
from __future__ import annotations

import json
import os
import socket
import time
import urllib.parse
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, Iterable, List, Tuple

from .data_connectors import load_xgboost_data_catalog

ROOT = Path(__file__).resolve().parents[1]
SOURCE_PATH = ROOT / "data" / "public_data_sources.json"
BUILD_DIR = ROOT / "data" / "build"
NORMALIZED_PATH = BUILD_DIR / "public_data_normalized.json"
ALLOWED_LIVE_HOSTS = {
    "www.data.go.kr",
    "data.go.kr",
    "www.safetykorea.kr",
    "safetykorea.kr",
    "plus.kipris.or.kr",
    "www.exportvoucher.com",
    "exportvoucher.com",
}


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def load_public_data_catalog() -> Dict[str, Any]:
    with SOURCE_PATH.open("r", encoding="utf-8") as fp:
        return json.load(fp)


@dataclass(frozen=True)
class FetchResult:
    source_id: str
    status: str
    mode: str
    records: List[Dict[str, Any]]
    evidence: Dict[str, Any]
    error: str | None = None


class PublicDataPipeline:
    """Collect, normalize, and persist public-data records by screen/table."""

    def __init__(self, catalog: Dict[str, Any] | None = None, timeout_sec: int = 8) -> None:
        self.catalog = catalog or load_public_data_catalog()
        self.timeout_sec = timeout_sec

    def run(self, use_live: bool = False) -> Dict[str, Any]:
        started_at = utc_now()
        fetch_results = [self.fetch_source(source, use_live=use_live) for source in self.catalog["sources"]]
        normalized = self.normalize(fetch_results)
        xgboost_catalog = load_xgboost_data_catalog()
        normalized.update(self.normalize_xgboost_catalog(xgboost_catalog))
        payload = {
            "metadata": {
                "service": "GROW-GATE Local EX",
                "pipeline_version": self.catalog.get("metadata", {}).get("version", "0.2.0-public-data"),
                "started_at": started_at,
                "completed_at": utc_now(),
                "use_live_requested": use_live,
            },
            "api_call_log": [self.api_log_row(item) for item in fetch_results],
            "evidence_source": [item.evidence for item in fetch_results],
            "screen_data_map": self.catalog.get("screen_data_map", []),
            "xgboost_catalog_metadata": xgboost_catalog["metadata"],
            "session_playbooks": xgboost_catalog.get("session_playbooks", []),
            **normalized,
        }
        return payload

    def write(self, payload: Dict[str, Any], output_path: Path = NORMALIZED_PATH) -> Path:
        output_path.parent.mkdir(parents=True, exist_ok=True)
        with output_path.open("w", encoding="utf-8") as fp:
            json.dump(payload, fp, ensure_ascii=False, indent=2)
        return output_path

    def fetch_source(self, source: Dict[str, Any], use_live: bool) -> FetchResult:
        evidence = {
            "evidence_id": source["source_id"],
            "source_type": source["connector"],
            "source_name": source["name"],
            "provider": source["provider"],
            "title": source["use_in_product"],
            "url": source.get("official_url"),
            "version": source.get("update_cycle", "unknown"),
            "retrieved_at": utc_now(),
            "confidence": 0.72,
            "screen_ids": source.get("screen_ids", []),
        }
        key_name = source.get("env_key")
        has_key = bool(key_name and os.getenv(key_name))
        if not use_live or (source.get("requires_key") and not has_key):
            return FetchResult(
                source_id=source["source_id"],
                status="warning" if source.get("requires_key") else "success",
                mode="seed",
                records=list(source.get("seed_records", [])),
                evidence=evidence,
                error=None if not source.get("requires_key") else f"missing env {key_name}",
            )

        try:
            records = self.fetch_live(source)
            return FetchResult(
                source_id=source["source_id"],
                status="success",
                mode="live",
                records=records or list(source.get("seed_records", [])),
                evidence={**evidence, "confidence": 0.88},
            )
        except Exception as exc:  # pragma: no cover - live network is optional
            return FetchResult(
                source_id=source["source_id"],
                status="warning",
                mode="seed_after_live_error",
                records=list(source.get("seed_records", [])),
                evidence=evidence,
                error=str(exc),
            )

    def fetch_live(self, source: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Minimal live fetch hook.

        Most Korean public APIs require per-service Swagger endpoint details and
        approval. The implementation records a reachable metadata probe and keeps
        exact service transforms isolated behind connector-specific branches.
        """
        connector = source["connector"]
        if connector == "cert_recall" and source.get("api_home"):
            return self.fetch_json_or_empty(source["api_home"])
        if connector in {"trade", "country"}:
            key = os.getenv(source.get("env_key", "DATAGO_SERVICE_KEY"), "")
            query = urllib.parse.urlencode({"serviceKey": key, "pageNo": 1, "numOfRows": 10, "type": "json"})
            probe_url = f"{source['official_url']}?{query}"
            return self.fetch_json_or_empty(probe_url)
        return []

    def fetch_json_or_empty(self, url: str) -> List[Dict[str, Any]]:
        validate_live_url(url)
        req = urllib.request.Request(url, headers={"User-Agent": "GrowGateLocalEX/0.2"})
        with urllib.request.urlopen(req, timeout=self.timeout_sec) as resp:
            content_type = resp.headers.get("content-type", "")
            body = resp.read(1024 * 256)
        if "json" not in content_type.lower():
            return []
        parsed = json.loads(body.decode("utf-8"))
        if isinstance(parsed, list):
            return [x for x in parsed if isinstance(x, dict)]
        if isinstance(parsed, dict):
            return extract_dict_records(parsed)
        return []

    def normalize(self, results: Iterable[FetchResult]) -> Dict[str, List[Dict[str, Any]]]:
        tables: Dict[str, List[Dict[str, Any]]] = {
            "trade_signal": [],
            "country_profile": [],
            "recall_case": [],
            "patent_signal": [],
            "support_program": [],
            "normalized_signal": [],
        }
        for result in results:
            source = self.source_by_id(result.source_id)
            target_table = source["target_table"]
            for idx, record in enumerate(result.records, start=1):
                normalized = self.normalize_record(source, record, idx)
                if target_table in tables:
                    tables[target_table].append(normalized)
                tables["normalized_signal"].append(
                    {
                        "signal_id": f"{result.source_id}-{idx:03d}",
                        "source_id": result.source_id,
                        "target_table": target_table,
                        "screen_ids": source.get("screen_ids", []),
                        "payload": normalized,
                    }
                )
        return tables

    def normalize_xgboost_catalog(self, catalog: Dict[str, Any]) -> Dict[str, List[Dict[str, Any]]]:
        """Expose workbook-derived data specs as pipeline serving tables."""
        sources = catalog.get("sources", [])
        feature_mappings = catalog.get("feature_mappings", [])
        collection_plan = catalog.get("collection_plan", [])
        internal_labels = catalog.get("internal_labels", [])
        return {
            "xgboost_catalog_sources": [self.catalog_source_row(source) for source in sources],
            "xgboost_feature_mappings": [self.feature_mapping_row(item) for item in feature_mappings],
            "xgboost_collection_plan": list(collection_plan),
            "xgboost_internal_labels": list(internal_labels),
            "external_fetch_candidates": [
                self.external_fetch_candidate(source)
                for source in sources
                if source.get("catalog_id", "").startswith("E-")
            ],
        }

    def catalog_source_row(self, source: Dict[str, Any]) -> Dict[str, Any]:
        return {
            "source_id": source["catalog_id"],
            "source_group": source["source_group"],
            "priority": source["priority"],
            "connector": source["connector"],
            "target_table": source["target_table"],
            "name": source["name"],
            "provider": source["provider"],
            "collection_method": source["collection_method"],
            "update_cycle": source["update_cycle"],
            "screen_ids": source["screen_ids"],
            "model_targets": source["model_targets"],
            "join_keys": source["join_keys"],
            "feature_candidates": source["feature_candidates"],
            "official_url": source["official_url"],
            "collection_status": source["collection_status"],
            "reliability": source["reliability"],
        }

    def feature_mapping_row(self, item: Dict[str, Any]) -> Dict[str, Any]:
        return {
            "model_target": item["model_target"],
            "prediction_type": item["prediction_type"],
            "label_definition": item["label_definition"],
            "source_ids": item["public_sources"] + item["external_sources"],
            "input_features": item["input_features"],
            "leakage_guardrail": item["leakage_guardrail"],
            "priority": item["priority"],
        }

    def external_fetch_candidate(self, source: Dict[str, Any]) -> Dict[str, Any]:
        return {
            "source_id": source["catalog_id"],
            "name": source["name"],
            "connector": source["connector"],
            "collection_method": source["collection_method"],
            "official_url": source["official_url"],
            "requires_key": source["requires_key"],
            "update_cycle": source["update_cycle"],
            "status": source["collection_status"],
            "live_enabled_by_default": False,
        }

    def normalize_record(self, source: Dict[str, Any], record: Dict[str, Any], index: int) -> Dict[str, Any]:
        source_id = source["source_id"]
        base = {"source_id": source_id, "evidence_id": source_id, "retrieved_at": utc_now()}
        table = source["target_table"]
        if table == "trade_signal":
            return {
                **base,
                "signal_id": f"TS-{index:03d}",
                "product_id": "demo-battery-pack",
                "country_code": record.get("country_code", "DE"),
                "hs_code": record.get("hs_code", "850760"),
                "import_growth_yoy": float(record.get("import_growth_yoy", 0)),
                "tariff": float(record.get("tariff", 0)),
                "logistics_days": record.get("logistics_days", ""),
                "opportunity_score": int(record.get("opportunity_score", 70)),
                "reason": record.get("reason", source["use_in_product"]),
            }
        if table == "country_profile":
            return {**base, "country_code": record.get("country_code", "DE"), **record}
        if table == "recall_case":
            return {
                **base,
                "recall_id": f"RC-{index:03d}",
                "product_category": record.get("category", "battery"),
                "hazard": record.get("hazard", ""),
                "keywords": record.get("keywords", ["battery", "pack"]),
                "severity": float(record.get("severity", 0.5)),
                "source_url": source.get("api_home") or source.get("official_url"),
                "recall_date": record.get("recall_date", ""),
                **record,
            }
        if table == "patent_signal":
            return {
                **base,
                "patent_signal_id": f"PS-{index:03d}",
                "keyword": record.get("keyword", ""),
                "ipc": record.get("ipc", []),
                "filings": int(record.get("filings", 0)),
                "recent_growth": float(record.get("recent_growth", 0)),
                "jurisdiction": record.get("jurisdiction", "KR/WO"),
                **record,
            }
        if table == "support_program":
            return {
                **base,
                "program_id": f"SP-{index:03d}",
                "name": record.get("name", ""),
                "agency": record.get("agency", source["provider"]),
                "region": record.get("region", "전국"),
                "eligibility": record.get("eligibility", "중소 제조기업"),
                "benefit": record.get("benefit", ""),
                "deadline": record.get("deadline", "공고별 상이"),
                "source_url": source.get("official_url"),
                **record,
            }
        return {**base, **record}

    def source_by_id(self, source_id: str) -> Dict[str, Any]:
        for source in self.catalog["sources"]:
            if source["source_id"] == source_id:
                return source
        raise KeyError(source_id)

    def api_log_row(self, item: FetchResult) -> Dict[str, Any]:
        now_ms = int(time.time() * 1000)
        return {
            "call_id": f"CALL-{item.source_id}",
            "connector_name": item.source_id,
            "status": item.status,
            "mode": item.mode,
            "started_at": now_ms,
            "ended_at": now_ms,
            "error_code": item.error,
            "record_count": len(item.records),
        }


def extract_dict_records(value: Any) -> List[Dict[str, Any]]:
    if isinstance(value, dict):
        records: List[Dict[str, Any]] = []
        for child in value.values():
            records.extend(extract_dict_records(child))
        if not records and value:
            return [value]
        return records
    if isinstance(value, list):
        rows: List[Dict[str, Any]] = []
        for item in value:
            rows.extend(extract_dict_records(item))
        return rows
    return []


def validate_live_url(url: str) -> None:
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme != "https":
        raise ValueError("live public-data URL must use https")
    host = (parsed.hostname or "").lower()
    if host not in ALLOWED_LIVE_HOSTS:
        raise ValueError(f"live public-data host is not allowed: {host}")
    try:
        addresses = socket.getaddrinfo(host, None, type=socket.SOCK_STREAM)
    except socket.gaierror as exc:
        raise ValueError(f"live public-data host cannot be resolved: {host}") from exc
    for family, _, _, _, sockaddr in addresses:
        ip = sockaddr[0]
        if _is_private_or_local_address(ip):
            raise ValueError(f"live public-data host resolves to a private/local address: {host}")


def _is_private_or_local_address(ip: str) -> bool:
    import ipaddress

    address = ipaddress.ip_address(ip)
    return address.is_private or address.is_loopback or address.is_link_local or address.is_multicast


def run_and_write(use_live: bool = False) -> Tuple[Path, Dict[str, Any]]:
    pipeline = PublicDataPipeline()
    payload = pipeline.run(use_live=use_live)
    path = pipeline.write(payload)
    return path, payload


if __name__ == "__main__":
    out, data = run_and_write(use_live=os.getenv("GROW_GATE_USE_LIVE_PUBLIC_DATA") == "1")
    print(json.dumps({"output": str(out), "counts": {k: len(v) for k, v in data.items() if isinstance(v, list)}}, ensure_ascii=False, indent=2))
