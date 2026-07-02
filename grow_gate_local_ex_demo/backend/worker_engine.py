"""Contract-based Python worker engine for Spring diagnosis runs.

This module is deliberately separate from the legacy demo `/analyze` path.
Spring remains the source of truth for auth, ownership, run status, and DB
persistence. The worker produces data/AI pipeline artifacts that are safe to
merge into a diagnosis result when `APP_FASTAPI_ENABLED=true`.
"""
from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Dict, List


def build_diagnosis_engine_result(payload: Dict[str, Any]) -> Dict[str, Any]:
    product = payload.get("productSnapshot") or {}
    records = payload.get("publicDataRecords") or []
    knowledge = payload.get("knowledgeSnapshot") or {}
    product_name = str(product.get("productName") or payload.get("productName") or "입력 제품")
    hs_code = str(product.get("hsCode") or payload.get("hsCode") or "")
    target_country = str(product.get("targetCountry") or payload.get("targetCountry") or "")

    normalized_metrics = _normalized_metrics(records)
    retrieval_items = _top_evidence(records)
    timeline = _timeline(records)
    guardrails = _guardrails(records, knowledge)

    return {
        "engineVersion": "python-worker-diagnosis-v1",
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "diagnosisId": payload.get("diagnosisId"),
        "productSnapshot": {
            "productName": product_name,
            "hsCode": hs_code,
            "targetCountry": target_country,
        },
        "normalizedMetrics": normalized_metrics,
        "evidenceRetrieval": {
            "mode": "python-worker-source-bound",
            "label": "Python data/AI worker evidence pack",
            "claimBoundary": "Only source-linked metrics, records, and manager-owned knowledge are used.",
            "query": " ".join(item for item in [product_name, hs_code, target_country] if item),
            "topK": retrieval_items,
        },
        "exportKnowledgeTimeline": timeline,
        "evidenceGuardrails": guardrails,
        "compiledWiki": _compiled_wiki(product_name, hs_code, target_country, normalized_metrics, guardrails),
        "preprocessingTrace": [
            {"step": "ingest", "status": "completed", "recordCount": len(records)},
            {"step": "normalize_metrics", "status": "completed", "metricBuckets": list(normalized_metrics.keys())},
            {"step": "compile_timeline", "status": "completed", "eventCount": len(timeline)},
            {"step": "verify_claim_boundaries", "status": guardrails["verdict"]},
        ],
    }


def _compiled_wiki(
    product_name: str,
    hs_code: str,
    target_country: str,
    normalized_metrics: Dict[str, Any],
    guardrails: Dict[str, Any],
) -> Dict[str, Any]:
    title = f"{target_country}-{hs_code}-{product_name} evidence page"
    metric_lines = []
    for bucket, values in normalized_metrics.items():
        metric_lines.append(
            f"- {bucket}: records={values.get('recordCount', 0)}, live/cache={values.get('liveOrCacheCount', 0)}, "
            f"fallback={values.get('fallbackCount', 0)}, contribution={values.get('scoreContributionSum', 0)}"
        )
    if not metric_lines:
        metric_lines.append("- no public-data metrics collected")

    log_markdown = "\n".join([
        "# Evidence collection log",
        f"- Product: {product_name}",
        f"- HS Code: {hs_code}",
        f"- Target: {target_country}",
        *metric_lines,
    ])
    error_book = "\n".join([
        "# Error book",
        f"- Guardrail verdict: {guardrails.get('verdict')}",
        f"- Unsupported claims: {guardrails.get('unsupportedClaimCount', 0)}",
        f"- Fallback positive contribution: {guardrails.get('fallbackPositiveContributionCount', 0)}",
    ])
    return {
        "knowledgePage": {
            "title": title,
            "country": target_country,
            "hsCode": hs_code,
            "markdown": "\n".join(["# " + title, "", *metric_lines]),
        },
        "logMarkdown": log_markdown,
        "errorBookMarkdown": error_book,
    }


def _normalized_metrics(records: List[Dict[str, Any]]) -> Dict[str, Any]:
    buckets: Dict[str, Dict[str, Any]] = {}
    for record in records:
        used_for = str(record.get("usedFor") or "unknown")
        provider = str(record.get("provider") or "UNKNOWN")
        source_type = str(record.get("sourceType") or "")
        confidence = _float(record.get("confidence"), 0.0)
        contribution = _int(record.get("scoreContribution"), 0)
        bucket = buckets.setdefault(
            used_for,
            {
                "recordCount": 0,
                "liveOrCacheCount": 0,
                "fallbackCount": 0,
                "providerCount": {},
                "scoreContributionSum": 0,
                "averageConfidence": 0.0,
                "metricKeys": [],
            },
        )
        bucket["recordCount"] += 1
        bucket["scoreContributionSum"] += contribution
        bucket["averageConfidence"] += confidence
        bucket["providerCount"][provider] = bucket["providerCount"].get(provider, 0) + 1
        if source_type in {"live", "cache", "crawl"}:
            bucket["liveOrCacheCount"] += 1
        if source_type == "fallback" or record.get("fallbackUsed"):
            bucket["fallbackCount"] += 1
        metrics = record.get("metrics") if isinstance(record.get("metrics"), dict) else {}
        for key in metrics.keys():
            if key not in bucket["metricKeys"]:
                bucket["metricKeys"].append(key)

    for bucket in buckets.values():
        count = max(1, bucket["recordCount"])
        bucket["averageConfidence"] = round(bucket["averageConfidence"] / count, 3)
    return buckets


def _top_evidence(records: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    ranked = sorted(
        records,
        key=lambda item: (_float(item.get("confidence"), 0.0), abs(_int(item.get("scoreContribution"), 0))),
        reverse=True,
    )
    evidence: List[Dict[str, Any]] = []
    for record in ranked[:5]:
        evidence.append(
            {
                "sourceId": record.get("id") or record.get("sourceId"),
                "provider": record.get("provider"),
                "sourceType": record.get("sourceType"),
                "usedFor": record.get("usedFor"),
                "confidence": _float(record.get("confidence"), 0.0),
                "metricSummary": record.get("metricSummary") or _metric_summary(record.get("metrics")),
            }
        )
    return evidence


def _timeline(records: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    events: List[Dict[str, Any]] = []
    for record in records[:30]:
        source_id = str(record.get("id") or record.get("sourceId") or "")
        events.append(
            {
                "eventId": f"PY-{source_id}" if source_id else "PY-UNSUPPORTED",
                "eventType": _event_type(str(record.get("sourceType") or ""), str(record.get("usedFor") or "")),
                "provider": record.get("provider"),
                "sourceId": source_id,
                "sourceType": record.get("sourceType"),
                "usedFor": record.get("usedFor"),
                "title": record.get("datasetName") or record.get("matchedKey") or record.get("provider"),
                "fetchedAt": record.get("fetchedAt"),
                "summary": record.get("metricSummary") or _metric_summary(record.get("metrics")),
                "scoreImpact": _int(record.get("scoreContribution"), 0),
                "confidence": _float(record.get("confidence"), 0.0),
                "status": "review_required" if record.get("fallbackUsed") else "linked",
            }
        )
    return events


def _guardrails(records: List[Dict[str, Any]], knowledge: Dict[str, Any]) -> Dict[str, Any]:
    fallback_positive = sum(
        1
        for record in records
        if (record.get("sourceType") == "fallback" or record.get("fallbackUsed"))
        and _int(record.get("scoreContribution"), 0) > 0
    )
    unsupported = sum(1 for record in records if not (record.get("id") or record.get("sourceId")))
    live_or_cache = sum(1 for record in records if record.get("sourceType") in {"live", "cache", "crawl"})
    return {
        "unsupportedClaimCount": unsupported,
        "fallbackPositiveContributionCount": fallback_positive,
        "officialLiveOrCacheRecordCount": live_or_cache,
        "managerKnowledgeCount": _int(knowledge.get("knowledgeSourceCount"), 0),
        "policies": [
            "Python worker output is advisory and must be persisted/served by Spring after auth and scope checks.",
            "Fallback records cannot create positive contribution.",
            "Every timeline event should link back to a provider/source id when available.",
        ],
        "verdict": "pass" if unsupported == 0 and fallback_positive == 0 else "review_required",
    }


def _event_type(source_type: str, used_for: str) -> str:
    if source_type == "fallback":
        return "SOURCE_FALLBACK_RECORDED"
    if used_for == "market":
        return "PY_MARKET_METRIC_NORMALIZED"
    if used_for == "recall":
        return "PY_RECALL_SIGNAL_NORMALIZED"
    if used_for == "fto":
        return "PY_FTO_SIGNAL_NORMALIZED"
    if used_for == "support":
        return "PY_SUPPORT_SIGNAL_NORMALIZED"
    return "PY_EVIDENCE_NORMALIZED"


def _metric_summary(metrics: Any) -> str:
    if not isinstance(metrics, dict) or not metrics:
        return "normalized metrics unavailable"
    parts = [f"{key}={value}" for key, value in list(metrics.items())[:4]]
    return ", ".join(parts)


def _int(value: Any, default: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _float(value: Any, default: float = 0.0) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return default
