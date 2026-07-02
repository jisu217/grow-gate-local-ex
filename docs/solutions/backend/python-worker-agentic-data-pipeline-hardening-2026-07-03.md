---
title: "Python worker agentic data pipeline hardening"
date: "2026-07-03"
category: "backend"
problem_type: "architecture"
component: "diagnosis-python-worker"
tags: ["python-worker", "agent-loop", "public-data", "security", "sre", "performance"]
---

# Python Worker Agentic Data Pipeline Hardening

## Context

GrowGate uses Spring Boot for auth, ownership, diagnosis lifecycle, DB persistence, and frontend DTOs. Python is a better fit for public-data preprocessing, evidence ranking, source-bound claim checks, and future data/ML expansion, but the worker must not become an unbounded agent that bypasses Spring security.

## Guidance

Keep the Python worker behind the Spring diagnosis flow and treat it as a bounded data/AI engine:

- Spring calls `POST /worker/diagnosis-v1` through `FastApiAgentClient.workerDiagnosis`.
- Spring sends normalized public-data records, source ids, and counts. It does not send raw public-data payloads.
- Python returns `normalizedMetrics`, `evidenceRetrieval`, `exportKnowledgeTimeline`, `compiledWiki`, and guardrail artifacts.
- Python also returns `agenticLoop.mode=python-planning-tool-verification-retry`, which documents plan, tool-use trace, verification, retry plan, and completion.
- Python reports `budgetGuard` with `externalLlmCalls=0`, `estimatedCostUsd=0`, `latencyMs`, processed/skipped record counts, and privacy posture.
- Spring persists the worker status under `pythonWorker` and detailed loop under `pythonAgenticLoop`.

## Why This Matters

This lets the team truthfully say that data collection, preprocessing, evidence compilation, and verification are handled by a Python data/AI worker without overstating autonomous AI. It also gives reviewers a concrete answer to CSO/SRE/performance questions:

- CSO: raw payloads and personal data are not accepted by the worker contract.
- SRE: fallback-heavy buckets produce retry/backoff guidance.
- Performance: record count is bounded by `GROW_GATE_WORKER_MAX_RECORDS`, and latency is visible.
- Cost control: the worker endpoint does not call external LLMs.

## Verification

Run these checks after modifying the worker contract:

```bash
cd grow_gate_local_ex_demo
python -m pytest tests/test_agent.py -q

cd ../backend/BE-02
JAVA_HOME=/private/tmp/temurin17/Contents/Home GRADLE_USER_HOME=.gradle-home ./gradlew test --no-daemon

cd ..
bash scripts/check-risk-gates.sh
```

## When To Apply

Apply this pattern whenever adding Python-side data collection, preprocessing, crawling, claim verification, or ML steps. If the feature needs user/company ownership, token handling, or persistent status, keep that responsibility in Spring and pass only scoped, normalized inputs to Python.
