# Agent Public Data Harness Playbook

Checked at: 2026-07-05

This playbook is the contest-facing contract for how Grow-GATE lets agents use public data without overstating AI capability.

## Runtime Boundary

- Final Gate owner: deterministic rule score plus risk/fallback conditions.
- Agent role: collect evidence, classify documents, retrieve source-bound chunks, verify claims, draft report text, and suggest retry actions.
- Required detail fields: `evidenceRetrieval`, `evidenceGuardrails`, `sourceTrace`, `llmFallbackReasons`, `gateDecision`, `claimVerifications`.
- Required Gate policy: `llmInfluencedGate=false`.

## Multi-Agent Roles

| Agent | Inputs | Outputs | Hard stop |
| --- | --- | --- | --- |
| PublicDataCollectorAgent | Spring public-data connectors, env config, local CSV/PDF/HWPX/DOCX | `sourceType`, `collectionEvidence`, metrics, masked params | Cannot call catalog pages as live proof |
| EvidenceCuratorAgent | `public_data_sources.json`, public-data records, knowledge files | source-bound chunks, source ids, timeline items | Cannot use source-less chunks in report claims |
| ClaimVerifierAgent | report claim candidates, source ids, source trace | accepted/rejected claim list | Rejects fallback/seed as official live proof |
| LoopEngineerAgent | provider health, zero reasons, stale flags | retry plan, owner task, API key/config gap | Cannot auto-promote unapproved loop wiki items |
| ReportAgent | verified claims, rule-owned Gate, action roadmap | Markdown report, buyer reply draft, internal memo | Cannot change `finalGate` |

## Provider Handling

KICOX operation rate:
- Runtime provider: `KICOX_OPERATION_RATE`.
- Fast path: `backend/BE-02/src/main/resources/publicdata/kicox-national-industrial-complex-operation-rate-20260331.csv`.
- API refresh: only after update due and `KICOX_OPERATION_RATE_API_KEY` or `DATAGO_SERVICE_KEY`.
- Use: industrial-complex aggregate context only.

KICOX factory registration:
- Runtime provider: `KICOX_FACTORY_REGISTRATION_BY_REGION_INDUSTRY`.
- Fast path: `backend/BE-02/src/main/resources/publicdata/kicox-factory-registration-by-region-industry-20260331.csv`.
- API refresh: only after update due and `KICOX_FACTORY_REGISTRATION_API_KEY` or `DATAGO_SERVICE_KEY`.
- Use: regional/industry factory-density context only.

MOTIE trade remedy occurrence:
- Runtime provider: `MOTIE_TRADE_REMEDY_OCCURRENCE`.
- Fast path: `backend/BE-02/src/main/resources/publicdata/motie-trade-remedy-occurrences-20251231.csv`.
- API refresh: only after update due and `MOTIE_TRADE_REMEDY_OCCURRENCE_API_KEY` or `DATAGO_SERVICE_KEY`.
- Use: annual trade-remedy aggregate context only; never product petition/legal/final Gate proof.

MOTIE trade remedy FTA list:
- Runtime provider: `MOTIE_TRADE_REMEDY_FTA_LIST`.
- Fast path: `backend/BE-02/src/main/resources/publicdata/motie-trade-remedy-fta-list-20250825.csv`.
- API refresh: only after update due and `MOTIE_TRADE_REMEDY_FTA_LIST_API_KEY` or `DATAGO_SERVICE_KEY`.
- Use: target-country FTA list context only; never tariff-rate, origin, legal, or final Gate proof.

KSURE country/industry overdue rate:
- Runtime provider: `KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE`.
- Fast path: `backend/BE-02/src/main/resources/publicdata/ksure-country-industry-overdue-rates-20211231.csv`.
- API refresh: update due has passed, so call odcloud only when `KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE_API_KEY` or `DATAGO_SERVICE_KEY` is configured.
- Use: country/industry historical overdue-rate context only; never buyer creditworthiness, insurance approval, transaction feasibility, or final Gate proof.

Knowledge-only sources:
- FTA PDF: target-country FTA context and sourceTrace support only.
- KEA HWPX: industry energy/GHG context and ESG explanation only.
- These do not add Gate score by themselves.

## Harness Loop

1. Collect: run Spring connector or file parser and record `collectionEvidence`.
2. Normalize: compute metrics, source period, warning, and bounded score contribution.
3. Verify: reject source-less claims and fallback-positive contribution.
4. Retry classify: convert failures into `API_KEY_NOT_CONFIGURED`, `CSV_CACHE_STALE_API_KEY_NOT_CONFIGURED`, `EMPTY_LIVE_RESPONSE`, `SCHEMA_MISMATCH`, or `HTTP_ERROR`.
5. Report: show live/cache/fallback/seed separation and the rule-owned Gate.
6. Compound: document solved connector or evidence-contract changes under `docs/solutions/`.

## Verification Commands

```bash
python3 scripts/check_public_data_providers.py --json
scripts/check-risk-gates.sh
cd backend/BE-02 && ./gradlew test --tests 'com.growgate.publicdata.service.*ConnectorTest'
```
