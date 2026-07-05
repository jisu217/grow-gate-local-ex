# Diagnosis Overview KPI Clarity - 2026-07-05

## Context

The diagnosis overview screen is a judge-facing first impression. The top KPI cards previously showed `0/100`, `56/100`, `0/100` with missing labels for the first and third cards. Even when backend diagnosis data existed, the frontend made the page look unfinished.

## Root Cause

The backend provided meaningful diagnosis data through `stageCards`, `inputTrace`, `diagnosisScore`, and `sourceTrustSummary`, but the frontend built the top KPI row from a fixed `EMPTY_METRICS` array and only replaced the second card score with `overallScore`.

That meant:

- `01` looked like a score card even though it should orient the product.
- `03` looked like a score card even though it should summarize evidence status.
- `label` existed in some legacy data, but `KpiRow` rendered only `eyebrow`, so labels could disappear.

## Fix

`frontend/FE-02/src/services/diagnosisService.js` now builds KPI data from the actual diagnosis detail:

- `01 대상 제품`: product name, HS Code, target country.
- `02 Gate 준비도`: rule-based overall score, Gate status, weakest score area.
- `03 근거 상태`: live/cache/fallback/empty source counts.

`frontend/FE-02/src/components/layout/KpiRow.jsx` now falls back from `eyebrow` to `label` or `title`, and value cards omit empty subtitles instead of leaving awkward whitespace.

`frontend/FE-02/src/components/layout/KpiRow.module.css` now uses a stable three-column layout for the three overview cards and protects long product names with wrapping.

## Guardrails

Mapper tests assert the KPI contract directly:

- Product card contains product name and `HS Code · country`.
- Gate card contains the overall score.
- Evidence card contains source count text.
- Sparse diagnosis data still renders meaningful fallback text instead of blank labels or meaningless `0/100` placeholders.

## Verification

Commands run:

```bash
cd frontend/FE-02
npm run test:mappers
npm run build
```

Browser QA used a local mock API and Playwright/gstack screenshot check to confirm the first viewport shows the three cards with labels and meaningful values.

## Presentation Rule

Do not show unlabeled KPI cards in the first viewport. If source evidence is missing, the UI should say so explicitly through source quality counts and review-required messaging, not by rendering empty score placeholders.
