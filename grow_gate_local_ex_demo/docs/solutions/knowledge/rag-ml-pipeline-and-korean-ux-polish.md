---
title: "RAG/ML pipeline fallback and Korean UX text wrapping"
date: 2026-06-02
category: knowledge
problem_type: implementation_pattern
component: grow_gate_local_ex_demo
tags:
  - rag
  - ml-pipeline
  - pgvector
  - korean-ux
---

## Context

GROW-GATE Local EX needs to demonstrate a production-shaped RAG and ML data pipeline without requiring paid infrastructure during a public demo. The UI also includes dense Korean copy, so generic `overflow-wrap: anywhere` can break Korean words into awkward one-character lines.

## Guidance

Use JSON fallback artifacts for the demo, but keep the API and table contracts aligned with the intended hosted database:

- `GET /rag/schema` exposes the Supabase Postgres + pgvector table contract.
- `POST /ml/train` runs an Airflow-ready task sequence and records execution history.
- `GET /pipeline/runs` and `GET /pipeline/latest-status` make the execution state visible to operators.
- `RAGDatabasePreview` writes table-shaped `rag_documents`, `rag_chunks`, and `rag_embeddings` preview data.

For Korean UI copy, prefer:

```css
word-break: keep-all;
overflow-wrap: break-word;
line-break: strict;
```

Reserve `overflow-wrap: anywhere` for technical strings such as URLs, JSON, code, and file paths.

## Why This Matters

The demo remains stable without Supabase credentials, but the service can still claim an honest migration path to Supabase/pgvector. The Korean wrapping rule avoids visible breaks such as `기 / 준`, which makes a polished product look unfinished to real business users.

## When to Apply

Apply this pattern when a public demo must show operational maturity without relying on external paid services, or when Korean business copy appears inside cards, dashboards, forms, and narrow panels.

## Examples

Airflow-ready task order:

```text
collect_public_sources
validate_raw_data
normalize_documents
build_rag_chunks
build_ml_features
train_xgboost
evaluate_model
publish_model_artifacts
```

Demo storage path:

```text
grow_gate_local_ex_demo/data/build/rag_database_preview.json
grow_gate_local_ex_demo/data/build/pipeline_runs.json
```
