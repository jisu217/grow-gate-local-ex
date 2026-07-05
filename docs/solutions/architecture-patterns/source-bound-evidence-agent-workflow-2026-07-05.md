---
title: Source-bound evidence agent workflow
date: 2026-07-05
category: docs/solutions/architecture-patterns
module: diagnosis-publicdata-agent
problem_type: architecture_pattern
component: service_object
severity: high
applies_when:
  - "Diagnosis agents need to use public data without letting LLM output control the final Gate"
  - "Public data can be live, cached, empty, fallback, seed, or heuristic and each state must be visible"
  - "S10 evidence screens need to show the proof run connected to the current diagnosis"
tags: [source-bound, evidence, public-data, agent-trace, proof-run, gate]
---

# Source-bound evidence agent workflow

## Context

GrowGate must present an AI-assisted workflow for the contest, but the final Gate remains deterministic. The safe architecture is a source-bound evidence workflow: agents can collect, normalize, explain, and verify sources, while rule scoring owns `finalGate` and every claim needs a traceable `sourceId`.

## Guidance

Treat evidence quality as a first-class contract before scoring or report generation.

Use this status set:

- `LIVE_VERIFIED`: operation endpoint, API key, raw response hash, schema validation, and normalized records exist.
- `CACHE_VERIFIED`: cached or CSV evidence is schema-valid and traceable, but not a new live API call.
- `EMPTY_TRUE_ZERO`: the source was configured and queried, and zero records is a valid result.
- `EMPTY_NOT_CONFIGURED`: endpoint or API key is missing, so zero records must not be read as market safety.
- `FALLBACK_ERROR`: connector failed or returned fallback data.
- `SEED_SYNTHETIC`: demo or seed data only.
- `HEURISTIC_ONLY`: rule or local heuristic, not public API evidence.

The EvidenceContextBuilder should expose enough fields for agents and UI to explain why a source did or did not affect the Gate:

- `zeroReason`
- `rawCount`
- `normalizedCount`
- `operationEndpoint`
- `apiKeyConfigured`
- `schemaValidation`
- `scoreContributionLedger`

Only `LIVE_VERIFIED`, `CACHE_VERIFIED`, and deliberate `EMPTY_TRUE_ZERO` may contribute positive score. `EMPTY_NOT_CONFIGURED`, `FALLBACK_ERROR`, `SEED_SYNTHETIC`, and `HEURISTIC_ONLY` can explain risk or next action, but they must not add positive contribution.

Persist each role step as an `AgentTraceEvent` with role, schema, input hash, evidence count, fallback reasons, and output JSON. This makes the multi-agent story inspectable without claiming unbounded autonomy.

Role outputs should remain bounded and schema-shaped:

- Market
- Regulation
- Recall
- FTO
- Support
- Roadmap
- Guardrail

The Guardrail role is responsible for removing claims without `sourceId`, preserving `llmInfluencedGate=false`, and confirming that LLM text cannot override `finalGate`.

## Why This Matters

This pattern lets the product say "source-bound evidence agent workflow" instead of overstating autonomous AI. It also gives judges a concrete proof chain:

- collection run stores raw/cache/fallback context
- proof run validates hashes and schema
- evidence context carries quality and contribution ledger
- agent trace stores role-specific reasoning artifacts
- S10 shows the proof and collection IDs for the current diagnosis

## When to Apply

- Adding any new public-data connector or CSV-first provider.
- Adding an agent role that consumes source evidence.
- Changing Gate score contribution rules.
- Updating S10, report, or diagnosis detail APIs that display source trust.

## Examples

Bad contribution:

```json
{
  "sourceQualityStatus": "EMPTY_NOT_CONFIGURED",
  "scoreContribution": 3,
  "zeroReason": "API key missing"
}
```

Good contribution:

```json
{
  "sourceQualityStatus": "EMPTY_NOT_CONFIGURED",
  "scoreContribution": 0,
  "scoreContributionLedger": {
    "positiveContributionAllowed": false,
    "blockedReason": "source quality does not allow positive contribution"
  }
}
```

Good report behavior:

- Keep: sourced claim with `sourceId`.
- Drop: unsourced LLM assertion.
- Keep: `finalGate` from deterministic rule score.
- Keep: `llmInfluencedGate=false`.

## Related

- `backend/BE-02/src/main/java/com/growgate/publicdata/enums/SourceQualityStatus.java`
- `backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataQualityClassifier.java`
- `backend/BE-02/src/main/java/com/growgate/agent/service/EvidenceContextBuilder.java`
- `backend/BE-02/src/main/java/com/growgate/agent/service/AgentTraceRecorder.java`
- `frontend/FE-02/src/components/diagnosis/S10Sources.jsx`
- `docs/solutions/architecture-patterns/csv-first-public-data-agent-harness-2026-07-05.md`
