# Engineering Failure Log

## 2026-07-03: AI claim drift

### Failure Pattern

The project accumulated useful AI-adjacent features, but some documents and UI wording could be read as stronger than the implementation. That creates judge-defense risk because reviewers ask for labeled data, model artifacts, vector indexes, and unconstrained autonomy evidence.

### Fix

- Added API fields for trust transparency: `sourceTrace`, `llmFallbackReasons`, and `claimVerifications`.
- Extended `gateDecision` with `ruleScore`, `riskLevel`, `fallbackCount`, `finalGate`, `reasons`, and `llmInfluencedGate=false`.
- Added `ClaimVerificationService` so source-less or unsafe LLM claims are blocked or surfaced as fallback reasons.
- Added `docs/contest-defense.md`, `docs/ai-claim-truth-table.md`, and root `AGENTS.md`.

### Prevention

- Add tests before changing AI-related behavior.
- Keep documentation wording tied to runtime paths.
- Treat LLM output as report/explanation assistance only.
- Keep the final Gate rule-owned and visible in the detail API.
