---
id: judge-synthetic-scenario-pack-2026-07-04
title: Judge Synthetic Scenario Pack Integration
kind: solution
status: active
created: 2026-07-04
---

# Judge Synthetic Scenario Pack Integration

## Problem

The project needed judge-safe demo data that shows the full Gate workflow without confusing synthetic records with live public-data proof or real customer validation.

## Solution

The received `grow_gate_judge_solution_pack.zip` is preserved under `docs/judge-solution-pack`, while executable seed assets are wired into MySQL init files and the FE demo flow:

- `03_agent_ml_regional_schema.sql` adds regional, agent trace, ML, synthetic registry, and validation tables.
- `04_seed_regional_and_registry.sql` inserts synthetic regional profiles.
- `05_seed_synthetic_companies.sql` inserts three owner accounts, products, knowledge, completed diagnosis results, and seed source traces.
- `docs/SYNTHETIC_DATA_POLICY.md` states that seed records are not live proof.
- `DiagnosisNew.jsx` exposes three synthetic scenario cards for fast judging demos.

## Guardrails

Synthetic records must remain `sourceType=seed` or another non-live type, must not set `operationEndpoint=true`, and must not increment official live/cache proof counts.

## Verification

The backend tests parse the synthetic profile and expected trajectory fixtures. The risk gate checks that synthetic policy and seed files exist and that source trust UI calls out seed limitations.
