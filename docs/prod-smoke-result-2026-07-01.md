# Production Smoke Result Template

Date: 2026-07-01

Command:

```bash
BASE_URL=http://3.36.60.121 ./scripts/smoke-prod.sh
```

Expected checks:

| Step | Expected |
| --- | --- |
| `/health` | 200 |
| `/api/diagnosis` without token | 401 |
| signup/login | token issued |
| `/api/users/me` | current user returned |
| product create | product id returned |
| diagnosis create | diagnosis id returned |
| diagnosis polling | `done` or `failed` visible |
| sources | sourceType visible |
| report Markdown | UTF-8 Markdown payload downloaded |
| public data runs | success/fallback counts printed |

Latest local verification before deploy:

- Backend `./gradlew test`: pending for this smoke run
- Frontend `npm run build`: pending for this smoke run
