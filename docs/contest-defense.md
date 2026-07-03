# Contest Defense

## 1. Is XGBoost used in production?

No. The submitted service uses Rule-based Gate scoring. A supervised model would require labeled training data, a train/eval pipeline, a model artifact, reproducible metrics, and a tested inference path. Those are not part of the current production diagnosis path.

## 2. Is this RAG?

No. The submitted service does not use a vector database retrieval path. It uses Source-bound evidence retrieval over known product knowledge, public-data records, and approved loop items. The API exposes `evidenceRetrieval`, `sourceTrace`, and `evidenceGuardrails` so judges can see which sources were used.

## 3. Is this an autonomous Agent?

No. It is a bounded workflow agent loop with fixed tools: collect public data, score readiness, plan risk actions, optionally call configured workers/providers, render a report, and verify sources. The loop records plan, tool execution, retry, fallback, and completion trace, but it does not discover arbitrary tools or own the final decision.

## 4. What prevents hallucination?

Claim verification. LLM-generated claims must reference known source IDs. Source-less claims, unknown source IDs, arbitrary numeric claims without metric evidence, and unsafe definitive claims are blocked or surfaced as `llmFallbackReasons` and `claimVerifications`.

## 5. What decides the final Gate?

The final Gate is decided by deterministic rule scoring, risk level, and fallback count. `gateDecision` exposes `ruleScore`, `riskLevel`, `fallbackCount`, `finalGate`, `reasons`, and `llmInfluencedGate=false`. LLM content can assist a report, but it cannot override the Gate.
