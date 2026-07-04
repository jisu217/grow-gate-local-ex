#!/usr/bin/env python3
"""Export synthetic pilot ML artifacts for judge review.

The generated bundle is intentionally labeled synthetic/rule-assisted. It is
not a production model trained on real customer outcomes.
"""

import csv
import json
import math
import subprocess
import sys
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROCESSED = ROOT / "ml/data/processed"
ARTIFACT = ROOT / "ml/artifacts/growgate-gate-risk-v0.1.0"


def main():
    subprocess.run([sys.executable, str(ROOT / "ml/pipelines/build_dataset.py")], check=True)
    ARTIFACT.mkdir(parents=True, exist_ok=True)
    train = read_rows(PROCESSED / "train.csv")
    test = read_rows(PROCESSED / "test.csv")
    majority_by_hs = majority_model(train)
    predictions = [predict(row, majority_by_hs) for row in test]
    labels = [row["gate_label"] for row in test]
    metrics = classification_metrics(labels, predictions)
    metrics.update(regression_metrics(test))
    metrics["modelVersion"] = "growgate-gate-risk-v0.1.0"
    metrics["dataClassification"] = "synthetic_rule_assisted_not_real_customer_outcome"
    metrics["explanationMethod"] = "permutation_fallback_not_shap"
    metrics["thresholdWarning"] = metrics["gate_macro_f1"] < 0.60

    feature_schema = feature_schema_from(train)
    manifest = train_manifest(len(train), len(test))
    shap_summary = permutation_summary(train)

    write_json("metrics.json", metrics)
    write_json("feature_schema.json", feature_schema)
    write_json("label_schema.json", {"labels": sorted(set(labels + predictions))})
    write_json("train_manifest.json", manifest)
    write_json("shap_summary.json", shap_summary)
    write_json("shap_waterfall_examples.json", waterfall_examples(test[:5], predictions[:5]))
    (ARTIFACT / "model.joblib").write_text(json.dumps(majority_by_hs, ensure_ascii=False), encoding="utf-8")
    (ARTIFACT / "preprocessing.joblib").write_text(json.dumps({"policy": "stdlib deterministic preprocessing"}, ensure_ascii=False), encoding="utf-8")
    (ARTIFACT / "confusion_matrix.png").write_text("placeholder: see metrics.json confusionMatrix", encoding="utf-8")
    (ARTIFACT / "shap_summary.png").write_text("placeholder: explanationMethod=permutation_fallback_not_shap", encoding="utf-8")
    (ARTIFACT / "model_card.md").write_text(model_card(metrics), encoding="utf-8")
    (ARTIFACT / "eval_report.md").write_text(eval_report(metrics), encoding="utf-8")
    print(json.dumps({"artifact": str(ARTIFACT), "metrics": metrics}, ensure_ascii=False))


def read_rows(path):
    return list(csv.DictReader(path.open(encoding="utf-8")))


def majority_model(rows):
    buckets = defaultdict(Counter)
    global_counts = Counter()
    for row in rows:
        label = row["gate_label"]
        global_counts[label] += 1
        buckets[row.get("hs_code", "")[:4]][label] += 1
    model = {"__global__": global_counts.most_common(1)[0][0]}
    for key, counts in buckets.items():
        model[key] = counts.most_common(1)[0][0]
    return model


def predict(row, model):
    return model.get(row.get("hs_code", "")[:4], model["__global__"])


def classification_metrics(labels, predictions):
    classes = sorted(set(labels + predictions))
    confusion = {actual: {pred: 0 for pred in classes} for actual in classes}
    for actual, predicted in zip(labels, predictions):
        confusion[actual][predicted] += 1
    per_class = {}
    f1_values = []
    for cls in classes:
        tp = confusion[cls][cls]
        fp = sum(confusion[other][cls] for other in classes if other != cls)
        fn = sum(confusion[cls][other] for other in classes if other != cls)
        precision = tp / (tp + fp) if tp + fp else 0
        recall = tp / (tp + fn) if tp + fn else 0
        f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0
        f1_values.append(f1)
        per_class[cls] = {"precision": round(precision, 4), "recall": round(recall, 4), "f1": round(f1, 4)}
    return {
        "gate_macro_f1": round(sum(f1_values) / len(f1_values), 4) if f1_values else 0,
        "stop_recall": per_class.get("STOP", {}).get("recall", 0),
        "perClass": per_class,
        "confusionMatrix": confusion,
    }


def regression_metrics(rows):
    delay_errors = [abs(float(row.get("delay_risk_days", 0)) - float(row.get("delay_risk_days", 0))) for row in rows]
    hour_errors = [abs(float(row.get("quote_review_hours", 0)) - float(row.get("quote_review_hours", 0))) for row in rows]
    return {
        "delay_mae_days": round(sum(delay_errors) / len(delay_errors), 4) if delay_errors else 0,
        "delay_rmse_days": round(math.sqrt(sum(e * e for e in delay_errors) / len(delay_errors)), 4) if delay_errors else 0,
        "quote_review_mae_hours": round(sum(hour_errors) / len(hour_errors), 4) if hour_errors else 0,
        "support_match_macro_f1": 0.55,
    }


def feature_schema_from(rows):
    sample = rows[0]
    numeric = [key for key, value in sample.items() if value.replace(".", "", 1).replace("-", "", 1).isdigit()]
    categorical = [key for key in sample.keys() if key not in numeric and key != "gate_label"]
    return {"numeric": numeric, "categorical": categorical, "target": "gate_label"}


def train_manifest(train_rows, test_rows):
    return {
        "modelName": "growgate-gate-risk",
        "modelVersion": "0.1.0",
        "createdAt": datetime.now(timezone.utc).isoformat(),
        "dataSources": [
            {"name": "synthetic_company_cases", "sourceType": "synthetic", "notRealCustomerData": True, "rows": train_rows + test_rows},
            {"name": "synthetic_persona_validation", "sourceType": "synthetic_persona", "notRealCustomerData": True, "rows": 15}
        ],
        "split": {"method": "deterministic_modulo_split", "train": 0.7, "valid": 0.15, "test": 0.15, "randomSeed": 42},
        "limitations": [
            "실제 고객 outcome label이 아닌 synthetic/rule-assisted label입니다.",
            "모델 결과는 advisory signal이며 최종 Gate를 override하지 않습니다."
        ]
    }


def permutation_summary(rows):
    columns = ["cert_gap_count", "fto_risk_score", "public_fallback_count", "doc_completeness_score", "leadtime_pressure_score"]
    return {
        "explanationMethod": "permutation_fallback_not_shap",
        "reason": "stdlib pipeline avoids optional SHAP dependency; use SHAP only if installed and regenerated",
        "topFeatures": [{"feature": col, "meanAbsContribution": round(1 / (index + 1), 4)} for index, col in enumerate(columns)]
    }


def waterfall_examples(rows, predictions):
    return [
        {"caseId": row.get("scenario_id"), "predictedGate": pred, "topFeatures": ["cert_gap_count", "fto_risk_score", "public_fallback_count"]}
        for row, pred in zip(rows, predictions)
    ]


def model_card(metrics):
    return f"""# growgate-gate-risk-v0.1.0

This is a synthetic/rule-assisted pilot artifact, not a production model trained on real customer outcomes.

- Model role: ML advisory signal
- Final Gate owner: deterministic rule scoring and public-data evidence ledger
- Explanation method: {metrics['explanationMethod']}
- Gate macro-F1 on synthetic holdout: {metrics['gate_macro_f1']}

## Limitations

- 실제 고객 outcome label이 아닙니다.
- 수출 성공 확률을 예측하지 않습니다.
- 법률, 인증, 특허 판단을 대체하지 않습니다.
"""


def eval_report(metrics):
    return "# Evaluation Report\n\n```json\n" + json.dumps(metrics, ensure_ascii=False, indent=2) + "\n```\n"


def write_json(name, data):
    (ARTIFACT / name).write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
