# Security/SRE/Performance Persona Review - 2026-07-02

> Security disclaimer: this review reduces obvious risk in the current submission build, but it is not a formal penetration test, legal review, or compliance certification.

## Review Prompts Used

### CSO

```
You are the CSO reviewing GrowGate before a public demo.
Check user permissions, cross-user data access, public-data raw payload handling,
personal data storage, API key exposure, cookie/JWT/CSRF behavior, and LLM cost-abuse paths.
Only report high-confidence risks that can be fixed in code or CI gates today.
```

### SRE

```
You are the SRE responsible for demo reliability.
Check health/smoke coverage, external API failure behavior, public-data fallback visibility,
LLM provider fallback behavior, deploy-time environment defaults, and whether an operator
can detect degraded data collection before a presentation.
```

### Performance Engineer

```
You are the performance engineer reviewing the diagnosis path.
Check high-cardinality loops, external provider fan-out, prompt/context size, frontend bundle
mock leakage, daily collection caps, crawler safety limits, and avoidable repeated calls.
```

## Findings And Fixes Applied

| Area | Risk | Code action |
| --- | --- | --- |
| LLM cost control | Repeated diagnosis could call Gemini/OpenRouter without a server-side quota. | Added per-user daily quota, global daily quota, prompt-size cap, and provider output-token cap. |
| LLM prompt size | Large evidence/scoring context could create slow or expensive calls. | `AgentInferenceService` now stops before provider invocation when estimated context exceeds `APP_AGENT_MAX_PROMPT_CHARS`. |
| API key leakage | Raw OpenRouter/Gemini tokens must never enter frontend or docs. | `scripts/check-risk-gates.sh` now fails on raw token-shaped values and frontend `VITE_*KEY/SECRET/TOKEN` usage. |
| Provider payload cost | Gemini/OpenRouter requests did not explicitly cap generated response size. | Gemini uses `generationConfig.maxOutputTokens`; OpenRouter uses `max_tokens`. |
| Deployment drift | Presentation env used non-code env names for LLM keys. | `.env.example.presentation` now uses `APP_AGENT_*` names that Spring actually reads. |
| Data access | Public-data history must not leak across users. | Existing user/company scoped run and record queries remain protected by risk gate. |
| Personal data storage | Auth token should not be readable by XSS. | Existing HttpOnly cookie + CSRF path remains protected by risk gate; localStorage access token writes are blocked. |
| Refund policy | Current service has no paid plan or payment API. | README now states billing/refund is not implemented and must be a separate release gate before monetization. |

## Runtime Controls

Set these only in the backend environment or secret manager:

```bash
APP_AGENT_ENABLED=false
APP_AGENT_MAX_DAILY_REQUESTS_PER_USER=30
APP_AGENT_MAX_DAILY_REQUESTS_GLOBAL=300
APP_AGENT_MAX_PROMPT_CHARS=12000
APP_AGENT_MAX_OUTPUT_TOKENS=1024
APP_AGENT_GEMINI_API_KEY=...
APP_AGENT_OPENROUTER_API_KEY=...
```

Recommended demo stance:

- Keep `APP_AGENT_ENABLED=false` unless keys and quotas are confirmed.
- If enabled, start with lower limits for a public demo account.
- Keep provider keys out of frontend `.env` files. Only `VITE_API_BASE_URL`/`VITE_API_URL` API origin aliases belong in frontend build settings.

## Remaining Release Gates

These are not blockers for the current non-paid submission demo, but should be handled before commercial launch:

- Persist LLM quota counters in Redis or DB for multi-instance deployment.
- Add billing/refund policy pages before any payment integration.
- Add SLO dashboard for public-data success/fallback ratio and LLM provider failure rate.
- Add data-retention controls for raw public-data payloads and accepted evidence-loop knowledge.

## Final Verification Snapshot

Run after the CSO/SRE/performance review updates:

```bash
./scripts/check-risk-gates.sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew test --no-daemon
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew bootJar -x test --no-daemon
/Users/jiyongkim/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node src/services/diagnosisService.mapper.test.js
/Users/jiyongkim/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node node_modules/vite/bin/vite.js build
cd grow_gate_local_ex_demo && .venv312/bin/python -m pytest tests/test_agent.py -q
```

Observed result:

- Risk gates passed.
- Secret-pattern scan returned no committed OpenRouter/Gemini/AWS/private-key shaped values.
- Backend tests passed under Java 17.
- Backend deployable jar built.
- Frontend mapper test and production build passed.
- Python worker/agent tests passed.
