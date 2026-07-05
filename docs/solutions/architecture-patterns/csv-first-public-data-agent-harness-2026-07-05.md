---
title: CSV-First Public Data Agent Harness
date: 2026-07-05
category: docs/solutions/architecture-patterns
module: public-data
problem_type: architecture_pattern
component: service_object
severity: medium
applies_when:
  - "A public-data file has a matching odcloud Swagger/API endpoint but local CSV should remain the fast path"
  - "Agents need to use the data without claiming that AI owns the final Gate"
tags: [public-data, csv-first, odcloud, agent-harness, evidence]
---

# CSV-First Public Data Agent Harness

## Context

Several public-data datasets arrive as user-provided CSV/PDF/HWPX/DOCX files and also expose odcloud or data.go.kr API metadata. The app needs to read local files quickly, switch to API refresh only when update due and key/endpoint are configured, and let agent/report flows use the data without overstating live API proof or AI decision ownership.

## Guidance

For file-backed public data that has an API refresh path:

1. Add a `PublicDataProvider` enum with explicit agency, license, official URL, usage warning, refresh hours, and bounded `usedFor`.
2. Implement a connector that prefers a classpath CSV resource and exposes `collectionEvidence`:
   - `collectionMode`
   - `datasetDate`
   - `nextUpdateDate`
   - `updateDue`
   - `apiRefreshEligible`
   - `apiRefreshSkippedReason`
   - `sameAsSwaggerLatestDataset`
   - `sameAsDataGoKrMetadata`
3. Call the API only when `updateDue && endpoint configured && api key configured`.
4. Keep `sourceType=cache` for CSV, `sourceType=live` only for successful operation endpoint calls, and `sourceType=fallback/empty` for failures or missing key.
5. Give the source at most a bounded domain contribution and write a `decisionImpact` that forbids final Gate, legal, capacity, demand, or safety overclaims.
6. Register the provider in:
   - Spring config and `.env.example`
   - `PublicDataOperationalStatusService`
   - `DiagnosisAsyncProcessor.publicDataDatasetName`
   - connector tests for CSV and API paths
   - `scripts/check_public_data_providers.py`
   - `data/public_data_sources.json`
   - provider/license matrix docs
7. Update the agent harness prompt/playbook so `PublicDataCollectorAgent`, `EvidenceCuratorAgent`, `ClaimVerifierAgent`, `LoopEngineerAgent`, and `ReportAgent` share the same source-type contract.

## Why This Matters

This pattern gives a strong contest story without breaking truth rules:

- Local CSVs make demos fast and deterministic.
- API refresh is real but does not pretend to be live when the key is absent.
- Agents can explain, verify, and retry evidence, but `llmInfluencedGate=false` remains the Gate policy.
- Risk gates can prove the connector, source catalog, docs, prompt, and tests stay aligned.

## When to Apply

- File data and Swagger/latest API metadata are the same dataset.
- The data is useful as aggregate context but cannot prove product-specific readiness.
- Update cadence matters and stale cache should trigger a clear API-key/config action.

## Examples

KICOX operation rate uses `KICOX_OPERATION_RATE`, reads `kicox-national-industrial-complex-operation-rate-20260331.csv`, and refreshes through odcloud only after `2026-06-30` when a key exists.

KICOX factory registration uses `KICOX_FACTORY_REGISTRATION_BY_REGION_INDUSTRY`, reads `kicox-factory-registration-by-region-industry-20260331.csv`, records that Swagger namespace `15085508/v1` matches the `20260331/26-row` file, and refreshes only after `2026-04-30` when a key exists.

## Related

- `docs/agent-public-data-harness-playbook-2026-07-05.md`
- `docs/public-data-provider-matrix.md`
- `docs/public-data-license-and-usage-matrix.md`
- `docs/judge-solution-pack/04_codex_prompts/CODEX_PROMPT_13_multi_agent_public_data_harness.md`
