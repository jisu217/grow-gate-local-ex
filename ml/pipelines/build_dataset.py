#!/usr/bin/env python3
"""Build a deterministic synthetic ML dataset.

This pipeline intentionally uses synthetic/rule-assisted labels only. It does
not prove production model accuracy or real customer outcomes.
"""

import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RAW = ROOT / "docs/judge-solution-pack/01_synthetic_data/ml_training_synthetic_cases.csv"
PROCESSED = ROOT / "ml/data/processed"


def main():
    PROCESSED.mkdir(parents=True, exist_ok=True)
    rows = list(csv.DictReader(RAW.open(encoding="utf-8-sig")))
    if not rows:
        raise SystemExit("no synthetic rows found")

    rows = sorted(rows, key=lambda row: row.get("case_id", ""))
    train, valid, test = split(rows)
    write_csv(PROCESSED / "train.csv", train)
    write_csv(PROCESSED / "valid.csv", valid)
    write_csv(PROCESSED / "test.csv", test)
    dictionary = {
        "classification": "synthetic_rule_assisted_not_real_customer_outcome",
        "labelColumn": "gate_label",
        "featurePolicy": "Only deterministic synthetic features are used.",
        "limitations": [
            "실제 고객 outcome label이 아닙니다.",
            "모델 결과는 advisory signal이며 최종 Gate를 override하지 않습니다."
        ],
        "columns": list(rows[0].keys()),
    }
    (PROCESSED / "feature_dictionary.json").write_text(json.dumps(dictionary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"rows": len(rows), "train": len(train), "valid": len(valid), "test": len(test)}, ensure_ascii=False))


def split(rows):
    train, valid, test = [], [], []
    for index, row in enumerate(rows):
        bucket = index % 20
        if bucket < 14:
            train.append(row)
        elif bucket < 17:
            valid.append(row)
        else:
            test.append(row)
    return train, valid, test


def write_csv(path, rows):
    with path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


if __name__ == "__main__":
    main()
