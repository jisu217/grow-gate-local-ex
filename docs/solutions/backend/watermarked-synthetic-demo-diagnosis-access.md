---
title: Watermarked synthetic demo diagnosis access
date: 2026-07-06
category: docs/solutions/backend
module: diagnosis
problem_type: logic_error
component: service_object
symptoms:
  - "Production diagnosis URL returned the generic result load error even though the diagnosis row existed"
  - "Authenticated admin API call to a seeded demo diagnosis returned 404"
root_cause: scope_issue
resolution_type: code_fix
severity: high
tags: [diagnosis, synthetic-demo, access-scope, production-qa]
---

# Watermarked synthetic demo diagnosis access

## Problem

Seeded synthetic demo cards point to fixed diagnosis IDs. Those rows are owned by synthetic scenario companies, so a different authenticated account could see the card but could not open the completed result.

The user-visible impact was a production result page showing `진단 결과를 불러오지 못했습니다` for `/diagnosis/2b52340c-d51d-50dc-b6cb-592627abb62d`.

## Symptoms

- The browser route redirected unauthenticated users to login as expected.
- After login, `GET /api/diagnosis/2b52340c-d51d-50dc-b6cb-592627abb62d` returned `404`.
- Direct DB inspection showed the row existed, was `done`, and contained `demoWatermark: synthetic_demo_not_real_customer`.

## What Didn't Work

- Treating the issue as missing seed data was wrong. The row and product existed.
- Frontend retry could not fix it because the backend deliberately hid out-of-scope rows.
- Broadening all diagnosis reads would have exposed real customer data across companies, which is not acceptable.

## Solution

Add a narrow synthetic-demo access predicate:

```java
public final class SyntheticDemoAccess {
    private static final String DEMO_WATERMARK = "synthetic_demo_not_real_customer";

    public static boolean isSyntheticDemoRun(DiagnosisRun run) {
        return run != null
                && (containsWatermark(run.getResult())
                || containsWatermark(run.getInputData())
                || containsWatermark(run.getKnowledgeSnapshot()));
    }
}
```

Then keep the original scoped lookup first:

```java
Optional<DiagnosisRun> scopedRun = company != null
        ? diagnosisRepository.findByIdAndCompanyId(diagnosisId, company.getId())
        : diagnosisRepository.findByIdAndUserId(diagnosisId, user.getId());

if (scopedRun.isPresent()) {
    return scopedRun.get();
}

return diagnosisRepository.findById(diagnosisId)
        .filter(SyntheticDemoAccess::isSyntheticDemoRun)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "진단 결과를 찾을 수 없습니다."));
```

Apply the same predicate to `AgentTraceQueryService` so the S10/trace path does not break after the main detail page loads.

## Why This Works

The normal company/user authorization path remains unchanged for real data. Only rows explicitly carrying the synthetic demo watermark become readable across authenticated accounts.

This matches the demo contract: these rows are not customer data, are labeled as synthetic, and must be usable by judges from whichever demo login is active.

## Prevention

- Add regression tests for both allowed synthetic demo access and denied real cross-company access.
- Check both `DiagnosisService` and `AgentTraceQueryService` when a route has multiple backend endpoints.
- In production QA, verify fixed demo IDs with the account judges are likely to use, not only with the scenario owner account.

## Related Issues

- QA report: `docs/qa/production-menu-qa-2026-07-06.md`
- Runtime code: `backend/BE-02/src/main/java/com/growgate/diagnosis/service/SyntheticDemoAccess.java`
- Regression tests: `backend/BE-02/src/test/java/com/growgate/diagnosis/service/DiagnosisServiceTest.java`, `backend/BE-02/src/test/java/com/growgate/agent/service/AgentTraceQueryServiceTest.java`
