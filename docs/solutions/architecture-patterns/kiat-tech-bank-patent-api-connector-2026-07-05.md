---
title: KIAT Tech Bank Patent API Connector Pattern
date: 2026-07-05
category: docs/solutions/architecture-patterns
module: public-data
problem_type: architecture_pattern
component: service_object
severity: low
applies_when:
  - Adding a Data.go.kr operation API connector that requires a service key
  - Using patent or FTO-adjacent public data as advisory evidence only
tags: [public-data, kiat, patent, fto, data-go-kr, api-key]
---

# KIAT Tech Bank Patent API Connector Pattern

## Context

The KIAT 기술은행 등록기술 특허 조회 서비스 is an operation API, not a bundled CSV source. In the local development environment no valid Data.go.kr service key was configured, and unauthenticated smoke calls returned `Unexpected errors`, so the implementation must not claim live success without a configured key.

## Guidance

- Register the connector as an `fto` public data provider with the official endpoint and conservative `decisionImpact`.
- Read keys from provider-specific env first, then shared KIAT/Data.go.kr fallbacks.
- Return `API_KEY_NOT_CONFIGURED` without making an HTTP call when no key exists.
- Mask `serviceKey` in normalized request parameters and collection evidence.
- Parse explicit row arrays such as `item`, `data`, `patents`, and `rows` before falling back to generic record estimation.
- Treat returned patent records as advisory FTO red flags only. They must not become a legal FTO conclusion or override the deterministic Gate.

## Why This Matters

Data.go.kr operation APIs often fail noisily without keys, and gateway response shapes vary by namespace. A connector should prove request shape, key handling, masking, fallback behavior, and parser metrics in tests even when local live API success cannot be verified.

## When to Apply

- A public data service has only an operation endpoint and no local CSV fixture.
- The source contributes evidence to `sourceTrace` or `evidenceRetrieval`.
- The signal is risk-adjacent, especially patent, recall, regulation, safety, or trade restriction data.

## Examples

The KIAT patent connector uses `https://apis.data.go.kr/B552536/tech_3` with `pageNo`, `numOfRows`, `type=json`, and `serviceKey`. With no configured key, it records a zero-result diagnostic instead of attempting a live call. With a configured key, tests assert the gateway URL, masked request params, row metrics, and conservative negative FTO contribution.

## Related

- `backend/BE-02/src/main/java/com/growgate/publicdata/service/KiatTechBankPatentConnector.java`
- `backend/BE-02/src/test/java/com/growgate/publicdata/service/KiatTechBankPatentConnectorTest.java`
- `docs/solutions/architecture-patterns/live-public-data-collection-layer.md`
