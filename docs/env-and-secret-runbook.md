# Env And Secret Runbook

## Principle

GrowGate keeps the final Gate deterministic. LLM and worker outputs are advisory report/evidence helpers only, and `llmInfluencedGate=false` must remain true.

Secrets must make startup behavior obvious:

- `prod` or `production` profile: `JWT_SECRET` is required and must be at least 32 bytes after base64 decode or as plain UTF-8 bytes.
- `prod` or `production` profile: the demo secret `unsafe-local-demo-secret-change-before-prod-32bytes` is rejected at startup.
- default/local/demo profile: the same unsafe demo secret can start the app for judge laptop smoke runs, but it is not an operating secret.
- Secret values are never logged or returned by health/proof APIs.

## Required Variables

| Variable | Required For | Notes |
| :--- | :--- | :--- |
| `DB_HOST` | docs/scripts | Default local host is `127.0.0.1`. Spring uses `DB_URL` directly. |
| `DB_PORT` | docs/scripts | Default MySQL port is `3306`. |
| `DB_NAME` | docs/scripts | Default DB is `growgate`. |
| `DB_USERNAME` | Spring DB login | Use `growgate_app` with the provided Docker DB. |
| `DB_PASSWORD` | Spring DB login | Required when running the Spring backend against MySQL. |
| `JWT_SECRET` | auth token signing | Generate with `openssl rand -base64 32` for non-demo use. |
| `JWT_ACCESS_TOKEN_TTL` | transparency template | Current Spring key is `JWT_EXPIRATION`; keep this aligned for future split access tokens. |
| `JWT_REFRESH_TOKEN_TTL` | transparency template | Refresh token TTL is not a separate runtime path yet. |
| `APP_AGENT_ENABLED` | optional LLM assisted report path | Default `false`. |
| `APP_FASTAPI_ENABLED` | optional Python worker bridge | Default `false`. |

Use [.env.example](../.env.example) as the non-secret template. Do not paste real provider API keys or JWT secrets into committed files.

## Local Demo Startup

For a clean laptop demo with Docker MySQL:

```bash
cd database
cp .env.example .env
docker compose up -d
```

```bash
cd backend/BE-02
export DB_URL="jdbc:mysql://127.0.0.1:3306/growgate?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"
export DB_USERNAME="growgate_app"
export DB_PASSWORD="growgate_app"
export JWT_SECRET="unsafe-local-demo-secret-change-before-prod-32bytes"
./gradlew bootRun
```

The unsafe demo secret is acceptable only for a local/demo run where the machine is not serving real users.

## Production Startup

For `prod`:

```bash
export SPRING_PROFILES_ACTIVE=prod
export JWT_SECRET="$(openssl rand -base64 32)"
```

The app fails fast if the secret is absent, too short, or equal to the local demo value. This is intentional.

## Agent And FastAPI Flags

Default:

```bash
export APP_AGENT_ENABLED=false
export APP_FASTAPI_ENABLED=false
```

With both false, the backend reports:

```json
{
  "agent": {
    "enabled": false,
    "fastapiEnabled": false,
    "mode": "rule-assisted",
    "reason": "APP_AGENT_ENABLED and APP_FASTAPI_ENABLED are false"
  }
}
```

This must be described as rule-assisted fallback mode, not as a live autonomous AI Agent.

To enable the optional worker bridge:

```bash
cd grow_gate_local_ex_demo
.venv312/bin/python run_backend.py
```

```bash
export APP_FASTAPI_ENABLED=true
export APP_FASTAPI_BASE_URL=http://localhost:8000
```
