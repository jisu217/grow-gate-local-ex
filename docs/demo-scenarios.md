# Demo Scenarios

All three scenarios are `synthetic_demo_not_real_customer` seed data. They are for repeatable judging demos and do not count as live public API proof.

Password for all owner accounts:

```text
growgateadmin
```

| Scenario | Login | Product ID | Diagnosis ID | Expected Gate |
| --- | --- | --- | --- | --- |
| 창원 배터리팩 | `owner+battery@growgate.local` | `a2e2360b-c6ac-57c3-8e3b-61b303faf36c` | `2b52340c-d51d-50dc-b6cb-592627abb62d` | `HOLD` |
| 부산 마린 액추에이터 | `owner+marine@growgate.local` | `9f7f291b-210a-5575-891a-bf6115bc7cce` | `6bc9800e-755c-55f9-b9ce-87a69dc2c450` | `CONDITIONAL_GO` |
| 대구 메디센서 | `owner+medical@growgate.local` | `db88726d-1c62-56fa-94d5-4d147710c7ce` | `cf7d23b4-51d8-53f7-be26-a92bf6682a6d` | `STOP` |

Recommended 3-minute flow:

1. Log in with one of the owner accounts.
2. Open `수출 Gate 판단`.
3. Use the synthetic scenario card to load the case or open the completed result.
4. Show Source Trust Bar and S10.
5. Point out `seedCount`, `liveCount=0`, and the warning that seed is not live proof.
6. Run public-data recollection only when real operation endpoints and keys are configured.

