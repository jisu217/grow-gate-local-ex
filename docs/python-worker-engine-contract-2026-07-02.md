# Python Worker Engine Contract - 2026-07-02

## Decision

GrowGate keeps Spring Boot as the system of record for authentication, authorization, diagnosis run lifecycle, DB transactions, and frontend DTOs. Python/FastAPI is promoted from a demo-only helper into a contract-based data/AI worker for collection, preprocessing, metric extraction, evidence timeline compilation, and guardrail checks.

## Implemented Contract

Endpoint:

```text
POST /worker/diagnosis-v1
```

Spring sends:

- `diagnosisId`, `userId`, `companyId`
- `productSnapshot`: product id/name, HS code, target country, description presence
- `knowledgeSnapshot`: counts for certifications, FTO, buyer requirements, accepted loop knowledge
- `publicDataRecords`: normalized public-data metrics only, not raw payloads

Python returns:

- `engineVersion=python-worker-diagnosis-v1`
- `normalizedMetrics`
- `evidenceRetrieval`
- `exportKnowledgeTimeline`
- `evidenceGuardrails`
- `sourceBoundVerification`
- `claimVerification`
- `budgetGuard`
- `agenticLoop`
- `preprocessingTrace`

## Runtime Behavior

- If `APP_FASTAPI_ENABLED=false`, Spring uses its local evidence artifacts and scoring path.
- If `APP_FASTAPI_ENABLED=true` and the worker succeeds, Spring stores worker artifacts in the diagnosis result.
- If the worker fails, Spring records fallback in `agentTrace` and still completes the diagnosis.
- Python worker output is advisory; Spring remains responsible for access control and persistence.

## Why This Boundary

- Python is better for API parsing, HTML/text preprocessing, schema validation, evidence ranking, future embeddings, and ML expansion.
- Spring is better for user ownership, CSRF/cookie auth, DB transaction safety, diagnosis status polling, and stable frontend APIs.
- This avoids a risky rewrite while giving the project a credible production path for data/AI workloads.

## Guardrails

- Spring sends normalized metrics, not raw public-data payloads.
- Fallback records cannot create positive score contribution.
- Worker result must include `engineVersion`; otherwise Spring rejects it and uses local fallback.
- Worker never performs external LLM calls in this endpoint; `budgetGuard.externalLlmCalls=0` and `estimatedCostUsd=0` make cost posture explicit.
- Worker enforces `GROW_GATE_WORKER_MAX_RECORDS` and reports truncated records rather than processing unbounded input.
- Candidate claims must reference an existing `sourceId`; overconfident phrases such as "문제 없음" are rejected before exposure.
- CI risk gate checks that the endpoint, Spring client method, `pythonWorker`, `pythonAgenticLoop`, and `normalizedMetrics` result fields remain wired.

## Agentic Worker Shape

The worker now returns `agenticLoop.mode=python-planning-tool-verification-retry`.
This is intentionally bounded rather than open-ended autonomous browsing:

1. `ingest_public_records`: receives Spring-scoped normalized records only.
2. `preprocess_metrics`: aggregates numeric metrics by scoring bucket.
3. `rank_evidence`: selects top source-bound evidence.
4. `verify_claims`: rejects claims without source ids or with overconfident wording.
5. `compile_wiki`: creates wiki/log/error-book artifacts for manager review.
6. `decide_retry`: recommends retry/backoff when live/cache evidence is missing or fallback remains.

Spring remains the owner of auth, company/user scope, persistence, and final response DTOs.
