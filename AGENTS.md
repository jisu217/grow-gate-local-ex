# GrowGate AI Truth Rules

This repository is for the Ministry of Trade / 산업통상부 2026 contest project. Agents working here must avoid AI and ML overclaims.

## Hard Rules

- The final Gate is a deterministic rule score plus risk and fallback conditions.
- LLM output is advisory only. It may help with report text, source-bound explanation, and trace summaries.
- Do not describe the app as a labeled training model, vector database retrieval system, or unbounded autonomous system unless those runtime paths, tests, artifacts, and metrics are actually present.
- Any AI-related behavior change must add or update tests first.
- Documentation must match runtime behavior, not planned future work.

## Allowed Wording

- Rule-based Gate
- LLM assisted report
- Source-bound evidence retrieval
- Source trace
- Evidence guardrails
- Claim verification
- bounded workflow agent loop
- Fallback-aware diagnosis

## Required API Transparency

Diagnosis detail responses should expose these fields when internal data exists:

- `evidenceRetrieval`
- `evidenceGuardrails`
- `sourceTrace`
- `llmFallbackReasons`
- `gateDecision`
- `claimVerifications`

## Decision Ownership

The final Gate is determined by rule scoring and risk/fallback conditions. LLM output must not override `finalGate`, and the response must keep `llmInfluencedGate=false`.
