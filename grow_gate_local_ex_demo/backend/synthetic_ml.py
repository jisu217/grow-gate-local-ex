"""Synthetic-limited ML contract for judge demos.

This module intentionally avoids claiming production ML quality. It reads the
provided synthetic CSV, finds the closest row for the current product/context,
and returns the same response shape a future trained model endpoint will use.
"""
from __future__ import annotations

import csv
from functools import lru_cache
from pathlib import Path
from typing import Any, Dict, List

PROJECT_ROOT = Path(__file__).resolve().parents[2]
CSV_PATH = PROJECT_ROOT / "docs" / "judge-solution-pack" / "01_synthetic_data" / "ml_training_synthetic_cases.csv"


FEATURE_COLUMNS = [
    "cert_gap_count",
    "fto_risk_score",
    "public_live_count",
    "public_cache_count",
    "public_fallback_count",
    "knowledge_trust_score",
    "buyer_requirement_complexity",
    "regional_support_density",
    "leadtime_pressure_score",
    "doc_completeness_score",
    "market_signal_score",
    "regulation_base_score",
]


def predict_gate_v1(payload: Dict[str, Any]) -> Dict[str, Any]:
    rows = _rows()
    query_key = _company_key(payload)
    candidates = [row for row in rows if row.get("company_key") == query_key] or rows
    row = min(candidates, key=lambda item: _distance(item, payload))
    gate = row.get("gate_label", "HOLD")
    top_features = _top_features(row)
    return {
        "predictedGate": gate,
        "probabilities": _probabilities(gate),
        "delayRiskDays": _int(row.get("delay_risk_days"), 14),
        "quoteReviewHours": _int(row.get("quote_review_hours"), 8),
        "supportMatchProbability": 0.78 if row.get("support_match_label") == "1" else 0.42,
        "shapTopFeatures": top_features,
        "modelVersion": "synthetic-csv-nearest-neighbor-v1",
        "syntheticTraining": True,
        "limitations": [
            "synthetic_demo_not_real_customer 데이터로 만든 계약 검증용 응답입니다.",
            "SHAP/permutation 설명 형식은 제공하지만 실제 고객 성과 정확도 주장에는 사용할 수 없습니다.",
            "최종 Gate는 Spring rule-based Gate가 소유하며 이 응답은 override하지 않습니다.",
        ],
    }


@lru_cache(maxsize=1)
def _rows() -> List[Dict[str, str]]:
    if not CSV_PATH.exists():
        raise FileNotFoundError(f"synthetic ML CSV not found: {CSV_PATH}")
    with CSV_PATH.open(newline="", encoding="utf-8-sig") as handle:
        return list(csv.DictReader(handle))


def _company_key(payload: Dict[str, Any]) -> str:
    text = " ".join(str(payload.get(key, "")) for key in ("companyName", "productName", "industry", "region"))
    if "marine" in text.lower() or "조선" in text or "액추에이터" in text:
        return "marine"
    if "medical" in text.lower() or "의료" in text or "웨어러블" in text or "센서" in text:
        return "medical"
    return "battery"


def _distance(row: Dict[str, str], payload: Dict[str, Any]) -> float:
    hs_penalty = 0 if str(payload.get("hsCode", "")).replace(" ", "")[:4] == row.get("hs_code", "")[:4] else 20
    country = str(payload.get("targetCountry", "")).lower()
    country_penalty = 0 if country and country in row.get("target_country", "").lower() else 8
    score = hs_penalty + country_penalty
    if payload.get("ruleScore") is not None:
        score += abs(_float(row.get("final_score"), 60) - _float(payload.get("ruleScore"), 60)) / 5
    return score


def _top_features(row: Dict[str, str]) -> List[Dict[str, Any]]:
    primary = row.get("primary_shap_expected") or "cert_gap_count"
    ranked = [primary, *[feature for feature in FEATURE_COLUMNS if feature != primary]]
    features = []
    for index, feature in enumerate(ranked[:5]):
        value = _float(row.get(feature), 0)
        features.append({
            "feature": feature,
            "importance": round(max(0.05, (5 - index) / 15), 3),
            "value": value,
            "direction": "positive" if feature in {"public_live_count", "knowledge_trust_score", "doc_completeness_score", "market_signal_score"} else "negative",
            "explanation": row.get("explanation_label") if index == 0 else f"{feature} synthetic feature contribution",
        })
    return features


def _probabilities(gate: str) -> Dict[str, float]:
    gates = ["GO", "CONDITIONAL_GO", "HOLD", "STOP"]
    return {item: (0.7 if item == gate else 0.1) for item in gates}


def _float(value: Any, fallback: float) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return fallback


def _int(value: Any, fallback: int) -> int:
    return int(round(_float(value, fallback)))
