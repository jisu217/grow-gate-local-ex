---
title: Data Catalog Source Dossier
category: frontend
problem_type: knowledge
component: team-dashboard-data-catalog
date: 2026-05-27
tags:
  - dashboard
  - data-catalog
  - ux
  - public-data
  - evidence
---

# Data Catalog Source Dossier

## Context

The team operations dashboard needs to explain not only which public data is already used, but also which external, institutional, and commercial-adjacent sources can strengthen the export-readiness story later.

## Guidance

Use one shared `dataSources` model for both the team dashboard and the catalog page:

- Team dashboard: show a paginated operational list first, then open source detail only after a user selects an item.
- Catalog page: show the full source inventory, original site, API or spec URL, endpoint, auth, format, collection method, usage, and data shape.
- Statistics: summarize category, status, and reliability distribution as lightweight bar charts so non-developers can scan coverage gaps.
- Pagination: keep the team dashboard to six sources per page; the catalog page can list all sources because its job is source review.
- Links: external API and site URLs should open in a new tab and be escaped before rendering.
- Data shape: render field examples as a calm key-value grid, not a dark code block. A dark block overstates raw schema details and steals attention from the source decision.

## Why This Matters

Judges and business reviewers need to see that the data strategy is not a static list. A source dossier makes the plan auditable: each source has an origin, a collection route, a data shape, and a product use case. This also gives developers a clear connector backlog without crowding the main operations room.

## When to Apply

Apply this pattern when a dashboard has more than ten data sources or when data trust is part of the product pitch. Keep the dense API documentation outside the main dashboard unless the selected source detail explicitly needs it.
