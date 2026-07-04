# Synthetic Data Policy

GROW-GATE includes a judge-facing synthetic scenario pack for repeatable demos.

Required label:

```text
synthetic_demo_not_real_customer
```

Rules:

- Synthetic companies, products, buyers, reports, interviews, ML rows, and diagnosis results are not real customer data.
- Synthetic records must keep `sourceType=seed` or another non-live type. They must never be rewritten as `live` or `cache`.
- Synthetic records must not set `operationEndpoint=true` or `apiKeyConfigured=true`.
- Synthetic data can demonstrate workflow shape, source-bound evidence UI, deterministic Gate logic, and ML contract shape.
- Synthetic data cannot be used as proof of live public API collection, public-data freshness, real customer validation, model accuracy, or production performance.

Presentation wording:

- Allowed: "합성 seed 기반 3개 시연 시나리오"
- Allowed: "Rule-based Gate with source-traced evidence and bounded workflow agent loop"
- Forbidden: "실제 고객 데이터"
- Forbidden: "합성 seed로 live 공공데이터 연동 증명"
- Forbidden: "합성 CSV만으로 검증된 production ML"

Judge defense:

Seed scenarios remain useful because they make the business workflow auditable without exposing customer data. Live public-data proof must come separately from S10 and `/api/public-data/health`, where official records require `sourceType=live/cache`, `operationEndpoint=true`, `apiKeyConfigured=true`, `collectionRunId`, `fetchedAt`, and metric fields.
