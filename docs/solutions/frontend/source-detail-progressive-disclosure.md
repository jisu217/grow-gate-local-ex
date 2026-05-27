---
title: Source Detail Progressive Disclosure
category: frontend
problem_type: knowledge
component: team-dashboard-public-data
date: 2026-05-24
tags:
  - dashboard
  - ux
  - progressive-disclosure
  - public-data
---

# Source Detail Progressive Disclosure

## Context

The public data coverage section originally rendered a source detail panel before the user selected a source. That made the section feel visually heavy and implied that one data source was already more important than the others.

## Guidance

Use progressive disclosure for source details:

- Default state shows only the source list, with enough metadata to compare sources.
- Selecting a source opens the detail workspace.
- The list compresses into a navigation rail only after detail is open.
- A clear "목록만 보기" action returns to the comparison state.
- Long table names and code references must wrap inside their cards.
- Korean headings and UI copy should use `word-break: keep-all` with a safe fallback such as `overflow-wrap: break-word`; otherwise words like "분리합니다" can split into single syllables and look unfinished.

## Why This Matters

Dashboard users first scan coverage, trust, and provider signals. Showing a detail panel too early creates split attention and weakens the planning story. Click-to-open detail keeps the initial state calm, then gives enough room for code, schema, usage, and statistics once the user has made an explicit choice.

## When to Apply

Apply this pattern when a dashboard has a comparison list and a dense evidence panel. Do not auto-select the first item unless the page's primary task is reviewing one known record.
