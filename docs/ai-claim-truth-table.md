# AI Claim Truth Table

| Claim area | Current truth | Allowed wording | Not allowed as a claim |
| --- | --- | --- | --- |
| Gate decision | Deterministic rule scoring with risk/fallback conditions | Rule-based Gate, explainable Gate, fallback-aware diagnosis | LLM-owned or AI-owned Gate decision |
| LLM usage | Optional provider chain for report/explanation assistance | LLM assisted report, source-bound explanation | LLM owns scoring or final decision |
| Retrieval | Known source records and approved knowledge items are searched and traced | Source-bound evidence retrieval, source trace | Vector DB retrieval as a completed runtime path |
| Agent workflow | Fixed Spring workflow loop with tool, retry, fallback, and verification trace | bounded workflow agent loop | Open-ended tool-discovering autonomy |
| Claims | Claims must be connected to known source IDs | Claim verification, evidence guardrails | Source-less factual claims |
| ML model | No labeled training model in production diagnosis | Rule-based scoring; supervised model is future work | Production-grade trained model claim |

## API Fields That Defend The Claims

- `gateDecision.finalGate`
- `gateDecision.llmInfluencedGate=false`
- `evidenceRetrieval`
- `evidenceGuardrails`
- `sourceTrace`
- `llmFallbackReasons`
- `claimVerifications`
- `sourceTrustSummary`
- `scoreContributionLedger`
