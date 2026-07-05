# Production Menu QA - 2026-07-06

## Summary

- Target: `https://growgate.54.180.56.24.sslip.io`
- Deployed commit: `51f8a449486e7985878c21b0363f6014d290dbc3`
- Reported URL: `/diagnosis/2b52340c-d51d-50dc-b6cb-592627abb62d`
- Result: `33/33 PASS`, `0 CHECK`
- Artifact directory: `artifacts/qa/production-menu-qa-20260706`
- Machine-readable result: `artifacts/qa/production-menu-qa-20260706/qa-menu-results.json`

## Reported Issue

The reported diagnosis page showed `진단 결과를 불러오지 못했습니다`. The diagnosis row existed in production DB, but the API returned `404` for an authenticated admin account because the row belonged to the synthetic demo company seed, not the admin company scope.

The fix keeps real customer diagnosis rows company/user scoped, and only allows cross-account read for watermarked synthetic demo rows containing `synthetic_demo_not_real_customer`.

## Verification

Backend:

```bash
cd backend/BE-02
./gradlew test
./gradlew bootJar
```

Production smoke:

```bash
BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/smoke-prod.sh
```

Smoke result:

- `PASS`
- `diagnosisId=d7ad2f06-42d7-4fc6-858d-021e81955f9c`
- `collectionRunId=7707af43-2244-4e11-84cb-335b9154e253`
- `liveCount=0`
- `cacheCount=32`
- `fallbackCount=0`
- `operationEndpointLiveCount=10`

Reported diagnosis API:

- Authenticated `GET /api/diagnosis/2b52340c-d51d-50dc-b6cb-592627abb62d` returned `200`.
- Product: `산업용 리튬이온 배터리팩 48V 80Ah`
- Status: `done`
- Gate: `HOLD`

## Menu Coverage

| Flow | Result |
| :--- | :--- |
| Home | PASS |
| Login | PASS |
| Signup selector | PASS |
| Personal signup | PASS |
| Company signup | PASS |
| Diagnosis history | PASS |
| Gate input | PASS |
| Public data daily refresh | PASS |
| My page | PASS |
| Battery demo result | PASS |
| Battery S10 sources page | PASS |
| Three synthetic demo result URLs | PASS |
| Diagnosis steps 00-10 | PASS |

## Broken Or Abnormal Items

No remaining broken menu item was found after the fix.

Earlier QA reported 3 `CHECK` items because the checker searched for internal Gate codes (`HOLD`, `CONDITIONAL_GO`, `STOP`) while the UI correctly displays user-facing Korean labels (`보류`, `조건부 진행`, `중단 후 보완`). The checker now validates the user-facing labels.

