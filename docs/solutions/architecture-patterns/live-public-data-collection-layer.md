---
title: Live public data collection with cache and fallback traces
date: 2026-06-30
category: docs/solutions/architecture-patterns
module: backend/BE-02 publicdata
problem_type: architecture_pattern
component: service_object
severity: medium
applies_when:
  - "A Spring service must call external public APIs but still run without API keys in local or judging environments"
  - "Diagnosis results need to show whether evidence came from live API, cache, or fallback"
tags: [spring-boot, public-data, cache, fallback, source-trace]
---

# Live public data collection with cache and fallback traces

## Context

The evaluation called out that public data was only represented as local signals. The fix needed to make the Spring main app capable of real live public API collection without making local development or judging fail when API keys are unavailable.

## Guidance

Use a three-layer shape:

- `PublicDataConnector`: provider-specific API call and normalization boundary.
- `PublicDataCollectionService`: orchestration, TTL cache lookup, run/record persistence.
- Diagnosis integration: convert records into source traces and small score contributions.

Persist two tables:

- `public_data_collection_runs`: one row per collection attempt.
- `public_data_records`: one row per provider result, including `rawPayload`, `normalizedPayload`, `fallbackUsed`, `cacheHit`, `scoreContribution`, `errorMessage`, `fetchedAt`, and `expiresAt`.

Connector failures should not fail diagnosis. Instead, create fallback records:

```java
PublicDataConnectorResult.builder()
    .provider(connector.provider())
    .requestKey(connector.requestKey(request))
    .usedFor(connector.usedFor())
    .fallbackUsed(true)
    .errorMessage("PUBLIC_DATA_ENABLED=false")
    .scoreContribution(-1)
    .build();
```

Diagnosis can then expose a defensible trace:

```java
source.put("fallbackUsed", record.isFallbackUsed());
source.put("cacheHit", record.isCacheHit());
source.put("collectionRunId", record.getRun().getId());
source.put("errorMessage", record.getErrorMessage());
source.put("scoreContribution", record.getScoreContribution());
```

## Why This Matters

External public APIs are not reliable in local environments: keys may be missing, APIs may throttle, and schemas may change. A persisted run/record model makes the product honest under failure while still letting the service use live data when configuration is available.

This also creates auditability for judging: the team can show exactly which provider was called, which request key was used, whether cache was reused, and why fallback happened.

## When to Apply

- A feature depends on public or third-party APIs.
- The product must work in local/offline demo mode.
- Scoring or recommendations need citation/source trace evidence.
- Failures should be visible but should not break the primary user flow.

## Examples

Use environment variables to keep provider configuration deploy-specific:

```yaml
public-data:
  enabled: ${PUBLIC_DATA_ENABLED:false}
  cache-ttl-hours: ${PUBLIC_DATA_CACHE_TTL_HOURS:24}
  kcs-trade:
    url: ${KCS_TRADE_API_URL:http://apis.data.go.kr/1220000/Newtrade/getNewtradeList}
    key: ${KCS_TRADE_API_KEY:${DATAGO_SERVICE_KEY:}}
```

Then explain the status precisely:

- Good: "API key가 있으면 live 호출하고, 실패하면 fallbackUsed와 errorMessage를 source trace에 남긴다."
- Good: "공식 카탈로그 URL만 설정된 provider는 operation endpoint가 필요하다는 fallback record를 남긴다."
- Avoid: "항상 실시간 공공 API만으로 점수가 계산된다."

## Related

- [Live Public Data Collection Design](../../superpowers/specs/2026-06-30-live-public-data-collection-design.md)
- [Live Public Data Collection Plan](../../superpowers/plans/2026-06-30-live-public-data-collection.md)
