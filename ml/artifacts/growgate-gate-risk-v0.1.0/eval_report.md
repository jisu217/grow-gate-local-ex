# Evaluation Report

```json
{
  "gate_macro_f1": 0.4363,
  "stop_recall": 0.75,
  "perClass": {
    "CONDITIONAL_GO": {
      "precision": 0,
      "recall": 0.0,
      "f1": 0
    },
    "HOLD": {
      "precision": 0.2917,
      "recall": 1.0,
      "f1": 0.4516
    },
    "STOP": {
      "precision": 1.0,
      "recall": 0.75,
      "f1": 0.8571
    }
  },
  "confusionMatrix": {
    "CONDITIONAL_GO": {
      "CONDITIONAL_GO": 0,
      "HOLD": 13,
      "STOP": 0
    },
    "HOLD": {
      "CONDITIONAL_GO": 0,
      "HOLD": 7,
      "STOP": 0
    },
    "STOP": {
      "CONDITIONAL_GO": 0,
      "HOLD": 4,
      "STOP": 12
    }
  },
  "delay_mae_days": 0.0,
  "delay_rmse_days": 0.0,
  "quote_review_mae_hours": 0.0,
  "support_match_macro_f1": 0.55,
  "modelVersion": "growgate-gate-risk-v0.1.0",
  "dataClassification": "synthetic_rule_assisted_not_real_customer_outcome",
  "explanationMethod": "permutation_fallback_not_shap",
  "thresholdWarning": true
}
```
