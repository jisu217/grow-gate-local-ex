---
title: Safe public web crawl ingestion for source-traced public data
date: 2026-07-02
category: docs/solutions/architecture-patterns
module: backend/BE-02 publicdata
problem_type: architecture_pattern
component: service_object
severity: medium
applies_when:
  - "A public-data workflow needs supplemental public web evidence without claiming it is an official API record"
  - "Crawled content must be stored in the same source trace model as API/cache/fallback records"
tags: [spring-boot, public-data, crawler, source-trace, security]
---

# Safe public web crawl ingestion for source-traced public data

## Context

Some public evidence is available only as public web pages or policy notices, not stable operation APIs. Adding crawling directly to scoring would blur source quality and create security/operational risk. The safer pattern is to treat crawl output as an opt-in connector that produces explicitly typed source records.

## Guidance

Use the existing `PublicDataConnector` boundary instead of creating a separate persistence path:

- The crawler is disabled by default with `@ConditionalOnProperty(prefix = "public-data.crawl", name = "enabled", havingValue = "true")`.
- It accepts only configured HTTPS URLs from an allowlist.
- It limits page count, raw HTML length, and normalized text snippet length.
- It strips scripts/styles, removes tags, normalizes whitespace, and masks secrets, emails, and phone numbers.
- It persists as `provider=MOTIE_WEB_CRAWL` and `sourceType=crawl`, not `live`.

The diagnosis/source UI must display `crawl` separately from `live`, `cache`, `fallback`, and `heuristic` so reviewers do not confuse public web evidence with official API integration.

## Why This Matters

The project needs to demonstrate practical data collection breadth without overclaiming. A typed crawl source lets the system use public policy pages as supporting evidence while preserving honesty about data provenance.

This also contains the biggest operational risks of crawling:

- SSRF risk is reduced by HTTPS and host allowlist checks.
- Sensitive strings are masked before persistence.
- Large payloads are bounded before writing to `TEXT` columns.
- CI risk gates assert that the crawler remains opt-in, allowlisted, masked, and source-typed.

## When to Apply

- API keys or operation endpoints are not yet available.
- Public policy notices or support program pages are useful as secondary evidence.
- The primary user flow must keep working if crawling fails.
- Judges or auditors need to see source provenance and confidence separately.

## Examples

Configuration:

```yaml
public-data:
  crawl:
    enabled: ${PUBLIC_DATA_CRAWL_ENABLED:false}
    urls: ${PUBLIC_DATA_CRAWL_URLS:}
    allowed-hosts: ${PUBLIC_DATA_CRAWL_ALLOWED_HOSTS:www.motie.go.kr,www.kotra.or.kr,www.safetykorea.kr}
```

Source display:

```js
if (sourceType === 'crawl') return `안전 크롤링 공개 웹${suffix}`
```

Risk gate:

```bash
search_quiet 'ConditionalOnProperty' MotieWebCrawlConnector.java \
  || fail "crawler must be disabled unless explicitly enabled"
```
