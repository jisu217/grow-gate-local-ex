# Security And Auth Policy

## JWT

GrowGate uses stateless JWT authentication. The token is accepted from the `Authorization: Bearer` header or the `gg_access_token` HttpOnly cookie.

`JWT_SECRET` behavior:

- Missing or blank secret fails startup.
- Secret material shorter than 32 bytes fails startup.
- `prod`/`production` rejects `unsafe-local-demo-secret-change-before-prod-32bytes`.
- Health and proof responses expose only `jwtSecretConfigured=true|false`, never the secret value.

## CSRF

Spring Security's built-in CSRF filter is disabled because the API is stateless JWT based. Cookie-authenticated unsafe requests are still protected by `CsrfCookieFilter`, a custom double-submit cookie filter:

- safe requests receive a readable `gg_csrf_token` cookie;
- unsafe requests using `gg_access_token` cookie must send matching `X-GG-CSRF`;
- bearer-token API calls do not depend on browser cookies.

## CORS

Allowed origins come from `APP_CORS_ORIGINS`. The default is local development origins only:

```text
http://localhost:3000,http://localhost:5173,http://localhost:5174,http://localhost:8765,http://localhost:8000
```

Production must set exact deployed frontend origins. Do not use wildcard origins with credentials.

## Public And Admin API Split

Public read-only proof endpoints:

- `GET /api/public-data/health`
- `GET /api/public-data/proof-runs/latest`
- public dashboard/run/record endpoints return empty scoped data for anonymous users where applicable.

Admin proof creation:

- `POST /api/admin/public-data/proof-runs`
- requires `ROLE_OWNER` or `ROLE_ADMIN`;
- `ROLE_EDITOR` and `ROLE_VIEWER` must receive 403.

Admin proof responses must not expose API keys, DB passwords, JWT secrets, raw credentials, or unredacted request headers/query strings. Provider proof should stay at fields such as provider/source type, `apiKeyConfigured`, redacted operation endpoint, raw/normalized counts, raw response hash, and schema validation status.

## Auth Error Contract

Authentication failure returns 401 through `JwtAuthenticationEntryPoint`.

Authorization failure returns 403 through `JwtAccessDeniedHandler`.

Both use the shared error response contract and do not leak token or secret contents.

## Agent/LLM Wording

When `APP_AGENT_ENABLED=false` and `APP_FASTAPI_ENABLED=false`, describe the runtime as rule-assisted fallback mode. Do not claim a live autonomous AI Agent is running.
