"""Lightweight ML/scoring functions for the GROW-GATE Local EX demo.

The functions are deterministic and dependency-light. They are intended to be
transparent for judges during a live demonstration, while still behaving like an
AI decision layer: feature extraction, similarity, prioritization, and scoring.
"""
from __future__ import annotations

import math
import re
from functools import lru_cache
from typing import Any, Dict, Iterable, List, Sequence, Tuple

import numpy as np

from .data_connectors import normalize_hs_code

TOKEN_RE = re.compile(r"[A-Za-z0-9가-힣]+")
ML_FEATURES = [
    "market_score",
    "regulation_pressure",
    "critical_reg_count",
    "recall_risk",
    "patent_risk",
    "support_fit",
    "document_score",
    "battery_flag",
    "digital_flag",
    "target_eu_flag",
]
ML_FEATURE_LABELS = {
    "market_score": "시장성 점수",
    "regulation_pressure": "규제 압력",
    "critical_reg_count": "긴급 규제 수",
    "recall_risk": "리콜 위험도",
    "patent_risk": "특허/FTO 위험도",
    "support_fit": "지원사업 적합도",
    "document_score": "문서 준비도",
    "battery_flag": "배터리 제품",
    "digital_flag": "디지털 기능",
    "target_eu_flag": "EU 대상국",
}


def clamp(value: float, low: float = 0.0, high: float = 100.0) -> float:
    return max(low, min(high, value))


def tokenize(text: str) -> List[str]:
    """Tokenize Korean/English/numeric strings for similarity matching."""
    return [token.lower() for token in TOKEN_RE.findall(text or "")]


def token_similarity(a: str, b: str) -> float:
    """Jaccard + substring similarity for short product descriptions."""
    a_tokens = set(tokenize(a))
    b_tokens = set(tokenize(b))
    if not a_tokens or not b_tokens:
        return 0.0
    intersection = len(a_tokens & b_tokens)
    union = len(a_tokens | b_tokens)
    jaccard = intersection / union
    substring_bonus = 0.0
    a_lower = a.lower()
    b_lower = b.lower()
    for token in a_tokens:
        if len(token) >= 3 and token in b_lower:
            substring_bonus += 0.04
    for token in b_tokens:
        if len(token) >= 3 and token in a_lower:
            substring_bonus += 0.04
    return min(1.0, jaccard + substring_bonus)


def match_tags(features: Sequence[str], tags: Sequence[str]) -> float:
    """Return how much a feature set overlaps with a market/regulation tag set."""
    feature_set = {f.lower() for f in features}
    tag_set = {t.lower() for t in tags}
    if not feature_set or not tag_set:
        return 0.0
    return len(feature_set & tag_set) / max(1, len(feature_set | tag_set))


def hs_prefix_matches(hs_code: str, prefixes: Iterable[str]) -> bool:
    hs = normalize_hs_code(hs_code)
    return any(hs.startswith(prefix) for prefix in prefixes)


def score_market(
    market: Dict[str, Any],
    product_features: Sequence[str],
    region_weight: float = 1.0,
    preferred_region: str | None = None,
) -> Tuple[int, str]:
    """Score export opportunity for a product-market pair."""
    growth = clamp(float(market.get("import_growth_yoy", 0)) * 3.2, 0, 45)
    tariff = clamp(20 - float(market.get("tariff_mfn", 0)) * 2.2, 0, 20)
    demand = match_tags(product_features, market.get("demand_tags", [])) * 25
    base = float(market.get("market_opportunity", 0.5)) * 25
    region_bonus = 8 if preferred_region and preferred_region.lower() in market.get("name", "").lower() else 0
    score = int(clamp((growth + tariff + demand + base + region_bonus) * region_weight, 1, 99))
    reason = (
        f"수입 증가율 {market.get('import_growth_yoy')}%, MFN 관세 {market.get('tariff_mfn')}%, "
        f"제품 태그 적합도 {match_tags(product_features, market.get('demand_tags', [])):.2f}를 반영했습니다."
    )
    return score, reason


def select_regulations(
    regulations: Sequence[Dict[str, Any]],
    target_region: str,
    hs_code: str,
    features: Sequence[str],
) -> List[Dict[str, Any]]:
    """Filter and rank regulation items by triggers."""
    normalized_features = {f.lower() for f in features}
    matched: List[Tuple[int, Dict[str, Any], str]] = []
    priority_weight = {"critical": 100, "high": 75, "medium": 50, "low": 25}

    for item in regulations:
        trigger = item.get("triggers", {})
        region_hit = target_region in trigger.get("regions", []) or "ALL" in trigger.get("regions", [])
        feature_hit_count = len(normalized_features & {f.lower() for f in trigger.get("features", [])})
        hs_hit = hs_prefix_matches(hs_code, trigger.get("hs_prefixes", []))
        if not (region_hit and (feature_hit_count > 0 or hs_hit)):
            continue
        score = priority_weight.get(item.get("priority", "medium"), 50)
        score += feature_hit_count * 12
        score += 18 if hs_hit else 0
        reason_parts = []
        if region_hit:
            reason_parts.append(f"대상 권역 {target_region} 일치")
        if feature_hit_count:
            reason_parts.append(f"제품 특성 {feature_hit_count}개 일치")
        if hs_hit:
            reason_parts.append("HS Code 접두어 일치")
        enriched = dict(item)
        enriched["matched_reason"] = ", ".join(reason_parts)
        matched.append((score, enriched, enriched["matched_reason"]))

    matched.sort(key=lambda row: row[0], reverse=True)
    return [item for _, item, _ in matched]


def score_recall_risks(
    recalls: Sequence[Dict[str, Any]],
    product_name: str,
    features: Sequence[str],
    top_n: int = 3,
) -> List[Dict[str, Any]]:
    """Rank recalls by product text and feature similarity."""
    feature_text = " ".join(features)
    query = f"{product_name} {feature_text}"
    results: List[Tuple[float, Dict[str, Any]]] = []
    for recall in recalls:
        recall_text = " ".join(
            [
                recall.get("product", ""),
                recall.get("hazard", ""),
                " ".join(recall.get("keywords", [])),
                recall.get("category", ""),
            ]
        )
        similarity = token_similarity(query, recall_text)
        feature_overlap = match_tags(features, [recall.get("category", "")])
        risk_value = min(1.0, 0.62 * similarity + 0.28 * feature_overlap + 0.10 * recall.get("severity", 0.0))
        risk_level = "HIGH" if risk_value >= 0.68 else "MEDIUM" if risk_value >= 0.38 else "LOW"
        enriched = dict(recall)
        enriched["similarity"] = round(similarity, 3)
        enriched["risk_score"] = round(risk_value, 3)
        enriched["risk_level"] = risk_level
        results.append((risk_value, enriched))
    results.sort(key=lambda row: row[0], reverse=True)
    return [item for _, item in results[:top_n]]


def score_patent_risks(
    patents: Sequence[Dict[str, Any]],
    product_name: str,
    features: Sequence[str],
    top_n: int = 3,
) -> List[Dict[str, Any]]:
    """Rank patent congestion by keyword similarity and inherent risk."""
    query = f"{product_name} {' '.join(features)}"
    results: List[Tuple[float, Dict[str, Any]]] = []
    for patent in patents:
        text = f"{patent.get('keyword', '')} {patent.get('korean_keyword', '')} {' '.join(patent.get('ipc', []))}"
        sim = token_similarity(query, text)
        growth = float(patent.get("recent_growth", 0.0))
        risk = float(patent.get("risk", 0.0))
        filings_norm = min(1.0, math.log10(max(1, int(patent.get("filings", 1)))) / 5.0)
        score = min(1.0, sim * 0.45 + risk * 0.32 + growth * 0.13 + filings_norm * 0.10)
        risk_level = "HIGH" if score >= 0.68 else "MEDIUM" if score >= 0.42 else "LOW"
        enriched = dict(patent)
        enriched["similarity"] = round(sim, 3)
        enriched["risk_score"] = round(score, 3)
        enriched["risk_level"] = risk_level
        results.append((score, enriched))
    results.sort(key=lambda row: row[0], reverse=True)
    return [item for _, item in results[:top_n]]


def score_support_programs(
    programs: Sequence[Dict[str, Any]],
    features: Sequence[str],
    regulations: Sequence[Dict[str, Any]],
    recall_risks: Sequence[Dict[str, Any]],
    patent_risks: Sequence[Dict[str, Any]],
    top_n: int = 5,
) -> List[Dict[str, Any]]:
    """Rank support programs based on the current gap profile."""
    need_tags = set(features) | {"export", "regional_sme"}
    if regulations:
        need_tags.add("certification")
    if any(r.get("risk_level") in {"MEDIUM", "HIGH"} for r in recall_risks):
        need_tags.update({"testing", "quality"})
    if any(p.get("risk_level") in {"MEDIUM", "HIGH"} for p in patent_risks):
        need_tags.update({"patent", "FTO", "technology"})

    results: List[Tuple[int, Dict[str, Any]]] = []
    for program in programs:
        tags = set(program.get("fit_tags", []))
        overlap = len({t.lower() for t in need_tags} & {t.lower() for t in tags})
        score = int(clamp(45 + overlap * 13 + (6 if program.get("roadmap_stage") == "30days" else 0), 30, 99))
        reason = f"현재 갭 태그({', '.join(sorted(need_tags))})와 지원사업 태그 {overlap}개가 일치합니다."
        enriched = dict(program)
        enriched["fit_score"] = score
        enriched["reason"] = reason
        results.append((score, enriched))
    results.sort(key=lambda row: row[0], reverse=True)
    return [item for _, item in results[:top_n]]


def score_readiness(
    market_scores: Sequence[int],
    regulations: Sequence[Dict[str, Any]],
    recall_risks: Sequence[Dict[str, Any]],
    patent_risks: Sequence[Dict[str, Any]],
    support_programs: Sequence[Dict[str, Any]],
    certifications: Sequence[str],
    has_bom: bool,
    has_test_report: bool,
    has_label_draft: bool,
) -> Tuple[int, Dict[str, int]]:
    """Weighted export-readiness score.

    Positive signals: market pull, documents already prepared, support fit.
    Negative signals: unresolved critical regulations, high recall and FTO risks.
    """
    market = int(clamp(sum(market_scores[:3]) / max(1, min(3, len(market_scores))), 0, 100))
    cert_coverage = min(100, len(certifications) * 15 + (20 if has_test_report else 0))
    doc = 20 + (25 if has_bom else 0) + (25 if has_test_report else 0) + (20 if has_label_draft else 0)
    doc = int(clamp(doc, 0, 100))
    support = int(clamp(sum(p.get("fit_score", 0) for p in support_programs[:3]) / max(1, min(3, len(support_programs))), 0, 100))

    critical_regs = sum(1 for r in regulations if r.get("priority") == "critical")
    high_regs = sum(1 for r in regulations if r.get("priority") == "high")
    reg_penalty = critical_regs * 11 + high_regs * 6 + max(0, len(regulations) - len(certifications)) * 2
    regulation = int(clamp(100 - reg_penalty + cert_coverage * 0.25, 0, 100))

    recall_penalty = sum(28 if r.get("risk_level") == "HIGH" else 13 if r.get("risk_level") == "MEDIUM" else 4 for r in recall_risks)
    recall = int(clamp(100 - recall_penalty, 0, 100))

    patent_penalty = sum(24 if p.get("risk_level") == "HIGH" else 11 if p.get("risk_level") == "MEDIUM" else 3 for p in patent_risks)
    patent = int(clamp(100 - patent_penalty, 0, 100))

    score = int(
        clamp(
            market * 0.24
            + regulation * 0.24
            + recall * 0.14
            + patent * 0.12
            + support * 0.12
            + doc * 0.14,
            0,
            100,
        )
    )
    breakdown = {
        "시장성": market,
        "규제·인증": regulation,
        "리콜·품질": recall,
        "특허·FTO": patent,
        "지원사업 적합도": support,
        "문서 준비도": doc,
    }
    return score, breakdown


def readiness_level(score: int) -> str:
    if score >= 78:
        return "HIGH"
    if score >= 55:
        return "MEDIUM"
    return "LOW"


def build_ml_feature_vector(
    market_scores: Sequence[int],
    regulations: Sequence[Dict[str, Any]],
    recall_risks: Sequence[Dict[str, Any]],
    patent_risks: Sequence[Dict[str, Any]],
    support_programs: Sequence[Dict[str, Any]],
    features: Sequence[str],
    target_region: str,
    has_bom: bool,
    has_test_report: bool,
    has_label_draft: bool,
) -> Dict[str, float]:
    """Convert the current analysis output into a readiness feature row."""
    market_score = sum(market_scores[:3]) / max(1, min(3, len(market_scores)))
    critical = sum(1 for r in regulations if r.get("priority") == "critical")
    high = sum(1 for r in regulations if r.get("priority") == "high")
    regulation_pressure = clamp((critical * 28 + high * 16 + len(regulations) * 4), 0, 100)
    recall_risk = max([risk_to_score(r.get("risk_level")) for r in recall_risks] or [12])
    patent_risk = max([risk_to_score(p.get("risk_level")) for p in patent_risks] or [10])
    support_fit = sum(p.get("fit_score", 0) for p in support_programs[:3]) / max(1, min(3, len(support_programs)))
    document_score = 20 + (25 if has_bom else 0) + (25 if has_test_report else 0) + (20 if has_label_draft else 0)
    feature_set = {f.lower() for f in features}
    return {
        "market_score": float(clamp(market_score)),
        "regulation_pressure": float(clamp(regulation_pressure)),
        "critical_reg_count": float(critical),
        "recall_risk": float(clamp(recall_risk)),
        "patent_risk": float(clamp(patent_risk)),
        "support_fit": float(clamp(support_fit)),
        "document_score": float(clamp(document_score)),
        "battery_flag": 1.0 if "battery" in feature_set else 0.0,
        "digital_flag": 1.0 if feature_set & {"digital", "iot", "software"} else 0.0,
        "target_eu_flag": 1.0 if target_region == "EU" else 0.0,
    }


def risk_to_score(level: str | None) -> float:
    return {"HIGH": 88.0, "MEDIUM": 56.0, "LOW": 22.0}.get(str(level or "").upper(), 24.0)


def run_xgboost_predictions(feature_row: Dict[str, float]) -> List[Dict[str, Any]]:
    """Estimate export execution readiness with compact demo models.

    The models are trained at startup from deterministic synthetic cases shaped
    like the public-data signals. In production, the same feature contract can be
    trained from accumulated diagnosis outcomes and certification lead times.
    """
    models = get_xgboost_models()
    x = np.array([[feature_row[name] for name in ML_FEATURES]], dtype=float)
    success_probability = float(models["success_classifier"].predict_proba(x)[0][1])
    lead_time = float(models["lead_time_regressor"].predict(x)[0])
    support_probability = float(models["support_classifier"].predict_proba(x)[0][1])
    risk_reserve = float(models["risk_reserve_regressor"].predict(x)[0])
    return [
        prediction_payload(
            models["success_classifier"],
            f"{models['engine']} Export Success Classifier",
            models["classifier_type"],
            "90일 파일럿 수출 실행 가능성",
            round(clamp(success_probability * 100), 1),
            "%",
            confidence_from_probability(success_probability),
            "시장성, 규제압력, 문서 준비도를 종합해 90일 내 파일럿 수출 패키지 완성 가능성을 추정합니다.",
        ),
        prediction_payload(
            models["lead_time_regressor"],
            f"{models['engine']} Certification Lead-time Regressor",
            models["regressor_type"],
            "인증·시험 리드타임 추정",
            round(clamp(lead_time, 21, 140), 0),
            "days",
            0.78,
            "긴급 규제 수, 배터리 여부, 문서 준비도를 기준으로 시험기관 상담부터 보완 완료까지의 기간을 추정합니다.",
        ),
        prediction_payload(
            models["support_classifier"],
            f"{models['engine']} Support Fit Classifier",
            models["classifier_type"],
            "지원사업 신청 우선순위 점수",
            round(clamp(support_probability * 100), 1),
            "%",
            confidence_from_probability(support_probability),
            "인증 갭, 지역 제조기업 태그, 지원사업 매칭 점수로 신청 우선순위를 정렬합니다.",
        ),
        prediction_payload(
            models["risk_reserve_regressor"],
            f"{models['engine']} Risk Reserve Regressor",
            models["regressor_type"],
            "시험·보완 예비비율 추정",
            round(clamp(risk_reserve, 4, 32), 1),
            "%",
            0.74,
            "리콜·FTO·규제 위험을 반영해 시험 실패, 라벨 보완, 회피설계 상담에 필요한 예비비율을 추정합니다.",
        ),
    ]


@lru_cache(maxsize=1)
def get_xgboost_models() -> Dict[str, Any]:
    x, y_success, y_lead, y_support, y_reserve = make_training_frame()
    try:
        from xgboost import XGBClassifier, XGBRegressor
        classifier_params = {
            "n_estimators": 80,
            "max_depth": 3,
            "learning_rate": 0.08,
            "subsample": 0.92,
            "colsample_bytree": 0.92,
            "eval_metric": "logloss",
            "random_state": 42,
        }
        regressor_params = {
            "n_estimators": 90,
            "max_depth": 3,
            "learning_rate": 0.07,
            "subsample": 0.92,
            "colsample_bytree": 0.92,
            "objective": "reg:squarederror",
            "random_state": 42,
        }
        return {
            "engine": "Optional boosted-tree demo",
            "classifier_type": "XGBClassifier",
            "regressor_type": "XGBRegressor",
            "success_classifier": XGBClassifier(**classifier_params).fit(x, y_success),
            "lead_time_regressor": XGBRegressor(**regressor_params).fit(x, y_lead),
            "support_classifier": XGBClassifier(**classifier_params).fit(x, y_support),
            "risk_reserve_regressor": XGBRegressor(**regressor_params).fit(x, y_reserve),
        }
    except Exception:
        from sklearn.ensemble import GradientBoostingClassifier, GradientBoostingRegressor

        classifier = {"n_estimators": 100, "max_depth": 3, "learning_rate": 0.06, "random_state": 42}
        regressor = {"n_estimators": 110, "max_depth": 3, "learning_rate": 0.055, "random_state": 42}
        return {
            "engine": "Gradient Boosting demo",
            "classifier_type": "GradientBoostingClassifier",
            "regressor_type": "GradientBoostingRegressor",
            "success_classifier": GradientBoostingClassifier(**classifier).fit(x, y_success),
            "lead_time_regressor": GradientBoostingRegressor(**regressor).fit(x, y_lead),
            "support_classifier": GradientBoostingClassifier(**classifier).fit(x, y_support),
            "risk_reserve_regressor": GradientBoostingRegressor(**regressor).fit(x, y_reserve),
        }


def make_training_frame() -> Tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    rng = np.random.default_rng(20260519)
    rows: List[List[float]] = []
    y_success: List[int] = []
    y_lead: List[float] = []
    y_support: List[int] = []
    y_reserve: List[float] = []
    for _ in range(260):
        market = rng.uniform(42, 96)
        regulation = rng.uniform(18, 94)
        critical = rng.integers(0, 4)
        recall = rng.uniform(12, 92)
        patent = rng.uniform(10, 88)
        support = rng.uniform(38, 98)
        doc = rng.uniform(20, 96)
        battery = rng.integers(0, 2)
        digital = rng.integers(0, 2)
        eu = rng.integers(0, 2)
        row = [market, regulation, critical, recall, patent, support, doc, battery, digital, eu]
        readiness_latent = (
            market * 0.28
            - regulation * 0.18
            - critical * 8.5
            - recall * 0.10
            - patent * 0.07
            + support * 0.16
            + doc * 0.23
            - battery * 4.0
            - digital * 2.0
            - eu * 3.0
            + rng.normal(0, 5)
        )
        lead = 28 + regulation * 0.46 + critical * 13 + battery * 9 + digital * 5 + eu * 8 - doc * 0.23 + rng.normal(0, 4)
        support_latent = support * 0.44 + market * 0.14 + regulation * 0.12 + battery * 5 + doc * 0.08 + rng.normal(0, 6)
        reserve = 5 + regulation * 0.08 + recall * 0.07 + patent * 0.06 + critical * 2.1 + battery * 1.8 - doc * 0.035 + rng.normal(0, 1.4)
        rows.append(row)
        y_success.append(1 if readiness_latent >= 28 else 0)
        y_lead.append(float(clamp(lead, 21, 140)))
        y_support.append(1 if support_latent >= 58 else 0)
        y_reserve.append(float(clamp(reserve, 4, 32)))
    return (
        np.array(rows, dtype=float),
        np.array(y_success, dtype=int),
        np.array(y_lead, dtype=float),
        np.array(y_support, dtype=int),
        np.array(y_reserve, dtype=float),
    )


def prediction_payload(
    model: Any,
    model_name: str,
    model_type: str,
    target: str,
    value: float,
    unit: str,
    confidence: float,
    interpretation: str,
) -> Dict[str, Any]:
    importances = getattr(model, "feature_importances_", np.zeros(len(ML_FEATURES)))
    ranked = sorted(zip(ML_FEATURES, importances), key=lambda row: row[1], reverse=True)[:5]
    total = sum(float(v) for _, v in ranked) or 1.0
    return {
        "model_name": model_name,
        "model_type": model_type,
        "target": target,
        "value": value,
        "unit": unit,
        "confidence": round(float(confidence), 2),
        "interpretation": interpretation,
        "feature_importance": [
            {
                "feature": ML_FEATURE_LABELS.get(name, name),
                "importance": round(float(value) / total, 3),
                "direction": feature_direction(name),
            }
            for name, value in ranked
        ],
    }


def confidence_from_probability(value: float) -> float:
    return round(0.62 + abs(value - 0.5) * 0.62, 2)


def feature_direction(name: str) -> str:
    if name in {"market_score", "support_fit", "document_score"}:
        return "positive"
    if name in {"regulation_pressure", "critical_reg_count", "recall_risk", "patent_risk"}:
        return "negative"
    return "mixed"
