# Lightsail 8GB Upsize Runbook - 2026-07-06

## Summary

GROW-GATE Local EX production was moved from a 2GB AWS Lightsail instance to an 8GB Lightsail instance on 2026-07-06. The production URL did not change because the existing Static IP was reattached to the new instance.

Production URL:

```text
https://growgate.54.180.56.24.sslip.io/
```

## Final State

| Item | Value |
| --- | --- |
| Static IP | `growgate-static-ip` / `54.180.56.24` |
| Current production instance | `growgate-8gb-20260706` |
| Previous rollback instance | `growgate-fresh-20260703-2134` |
| Current bundle | `large_3_0` |
| Current hardware | 8GB RAM, 2 vCPU, 160GB SSD |
| Previous bundle | `small_3_0` |
| Previous hardware | 2GB RAM, 2 vCPU, 60GB SSD |
| Snapshot used | `growgate-pre-8gb-20260706-1611` |
| Snapshot state | `available` |
| Deployed commit | `40492eb30e04113801fc353d2b74f571201d1663` |

## Safety Rules Applied

- The original 2GB instance was not deleted.
- A fresh manual snapshot was created before the 8GB instance was launched.
- The 8GB instance was tested before the Static IP cutover.
- Static IP cutover was performed only after explicit approval.
- No new load balancer, managed database, block storage, CDN, or Static IP was created.
- Temporary SSH allowlist entries were removed after verification.
- Temporary local Lightsail PEM files were deleted after use.
- Secrets and environment values were not printed.

## Cost Notes

| Resource | Estimate |
| --- | --- |
| 8GB `large_3_0` instance | about USD 44/month |
| 8GB 72-hour validation window | about USD 4.34 |
| Previous 2GB plus new 8GB for 72 hours | about USD 5.52 total |
| Snapshot storage | may incur separate storage cost |
| Unattached Static IP | none expected because existing Static IP is attached |

AWS Budgets should be treated as alerting only, not a hard spending cap. The old 2GB instance and snapshots should be reviewed after 24-48 hours.

## Verification Performed

Before cutover, the new 8GB instance was checked through its temporary public IP and forced DNS resolution.

After cutover, the following production URL checks passed:

| Check | Result |
| --- | --- |
| `/` | `200` |
| `/health` | `200` |
| `/public-data/daily-refresh` | `200` |
| `/diagnosis/57ddd3bd-c8d6-40fd-b25c-978b6f67c8f6` | `200` |
| `BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/smoke-prod.sh` | PASS |

Smoke summary:

```text
stageCards.s04-s07=present
publicData.successCount=31
fallbackCount=0
liveRatio=0.8611
diagnosisId=54f6fe9d-89ba-4210-90bd-18333d178281
collectionRunId=c8a70531-2a06-4544-83c8-4235be9d4122
liveCount=0
cacheCount=31
fallbackCount=0
operationEndpointLiveCount=9
apiKeyConfiguredLiveCount=0
```

Server-side health after cutover:

| Check | Result |
| --- | --- |
| RAM | about 7.6Gi total, about 6.3Gi available during verification |
| `nginx` | active |
| `growgate-backend` | active |
| `mysql` | active |
| backend local `/health` | `200` |
| backend/nginx severe recent logs | no OOM, killed process, 502, or 504 found during verification |

## Firewall State

| Port | State |
| --- | --- |
| 80/tcp | open to `0.0.0.0/0`, `::/0` |
| 443/tcp | open to `0.0.0.0/0`, `::/0` |
| 22/tcp | restricted to existing 4 CIDRs plus `lightsail-connect` |

Temporary SSH CIDR `1.234.247.187/32` was added only for verification and removed after the checks.

## Rollback Plan

If production becomes unstable, reattach `growgate-static-ip` to the previous instance:

```bash
aws lightsail attach-static-ip \
  --profile jiyongrock \
  --region ap-northeast-2 \
  --static-ip-name growgate-static-ip \
  --instance-name growgate-fresh-20260703-2134
```

Then verify:

```bash
curl -s -o /dev/null -w '%{http_code} %{time_total}\n' \
  https://growgate.54.180.56.24.sslip.io/health
```

Do not delete `growgate-fresh-20260703-2134` until the 8GB instance has been stable for at least 24-48 hours and deletion is explicitly approved.

## Follow-Up Checklist

- [ ] Keep the previous 2GB instance for 24-48 hours as rollback capacity.
- [ ] Review whether to delete or retain `growgate-pre-8gb-20260706-1611`.
- [ ] Add 2-4GB swap only if operational monitoring shows sustained memory pressure.
- [ ] Move Gradle/Vite builds out of the server path where possible; deploy jar/dist artifacts.
- [ ] Add Lightsail/CloudWatch CPU, memory, disk, and status alarms if the project remains publicly accessible.
- [ ] Re-run `BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/smoke-prod.sh` before judging/demo sessions.
