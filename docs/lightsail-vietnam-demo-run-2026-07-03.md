# Lightsail Vietnam Demo Run - 2026-07-03

## Target

- Lightsail profile/account: `jiyongrock` / `980921741639`
- Instance: `Ubuntu-1`
- Static IP: `54.180.56.24`
- Public URL: `https://growgate.54.180.56.24.sslip.io`
- Scenario country: Vietnam (`VN`)
- Scenario product: industrial water treatment filter cartridge
- HS code: `842121`

## Deployment Status

The GitHub `main` branch is ahead of the currently deployed server.

Latest GitHub commit confirmed locally:

- `53c0d16 Harden AI evidence truth contract`

Deployment to the Lightsail host could not be completed in this run because SSH is blocked before authentication:

- Lightsail firewall now allows SSH from current operator IP `211.36.147.108/32`.
- Existing allowed SSH CIDR also remains `221.139.247.7/32`.
- `ssh ubuntu@54.180.56.24` still times out.
- SSM is not available for this Lightsail instance.

Likely cause: instance-level UFW or local outbound SSH restrictions. The HTTPS app is reachable, but server shell access is required to pull `main`, update environment variables, and restart services.

## Smoke Result

Production smoke completed against the public URL:

```text
Smoke target: https://growgate.54.180.56.24.sslip.io
diagnosis status: done
stageCards.s04-s07=present
publicData.successCount=0 fallbackCount=5
publicData.liveRatio=0.0 fallbackRatio=1.0
PASS: smoke completed
```

Evidence file:

- `/private/tmp/growgate-smoke/live-public-data-smoke-evidence.json`

Important result:

- `diagnosisId=ace75015-b0ad-4395-bc9b-1b49b8fe4cbe`
- `liveCount=0`
- `cacheCount=0`
- `fallbackCount=5`
- `operationEndpointLiveCount=0`
- `apiKeyConfiguredLiveCount=0`

Interpretation: public-data collection endpoint works, but the deployed server does not have live operation endpoints/API keys configured.

## Demo Account And Data

Created through the deployed HTTPS API:

- Account: `vietnam-admin-20260703-201633@growgate.local`
- Password: standard demo password requested by the operator. Do not commit active deployment passwords to public docs.
- User type: `company`
- Role: `owner`
- Company: `청주퓨어테크 주식회사`
- Product ID: `902f77be-842c-4f0a-8ba4-38152640ff04`
- Product: `베트남 수출용 산업용 정수 필터 카트리지 20260703-201633`
- Diagnosis ID: `d1c401eb-65e0-498f-8bf6-9068a9b0d572`
- Diagnosis URL: `https://growgate.54.180.56.24.sslip.io/diagnosis/d1c401eb-65e0-498f-8bf6-9068a9b0d572`
- Accepted knowledge-loop item: `15fcfacf-50c3-40c6-ab3c-e4e5b531ef13`

Scenario data inserted:

- Manager-verified product knowledge
- 3 certifications
- 1 Vietnam FTO review item
- 2 buyer requirements
- 3 knowledge source documents
- Explicit public-data collection request
- Diagnosis run and report retrieval
- Knowledge-loop accept action

Diagnosis summary:

```json
{
  "status": "done",
  "score": {
    "overall": 71,
    "market": 61,
    "regulation": 76,
    "recall": 78,
    "fto": 59,
    "support": 88
  },
  "gateDecision": {
    "status": "CONDITIONAL_GO",
    "label": "조건부 진행"
  },
  "sourceTypes": {
    "cache": 5,
    "heuristic": 5,
    "agent": 1
  },
  "publicDataDashboard": {
    "successCount": 0,
    "fallbackCount": 10,
    "liveRatio": 0.0,
    "fallbackRatio": 1.0
  },
  "agentTraceMode": "agent-loop-planning-tool-verification-retry",
  "agentInference": null,
  "hasEvidenceRetrieval": true,
  "hasEvidenceGuardrails": true,
  "hasKnowledgeLoopStage": true
}
```

## Known Deployed Server Gaps

These are deployment/runtime gaps, not local `main` code gaps:

- `/api/company/me` returns HTTP 500 for the new company owner account.
- `sourceTrace`, `claimVerifications`, and `llmFallbackReasons` from the latest `main` are not visible on the deployed server yet.
- Public-data live count is still `0`; API keys/operation endpoints are not configured on the host.
- `agentInference` is `null`; LLM provider chain is not enabled/configured on the host.
- Python worker/FastAPI bridge is not confirmed running on the host.

## Required Next Server Actions

After SSH or Lightsail browser shell access is restored:

1. Pull latest `main`.
2. Rebuild backend and frontend.
3. Set production environment variables:
   - `PUBLIC_DATA_ENABLED=true`
   - provider operation endpoint/API key variables
   - optional `APP_AGENT_ENABLED=true` only with Gemini/OpenRouter keys and quota caps
   - optional `APP_FASTAPI_ENABLED=true` only if the Python worker is running and smoke-tested
4. Restart Spring, frontend/Nginx, and optional worker services.
5. Rerun:

```bash
BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/smoke-prod.sh
```

For live public-data proof after keys/endpoints are configured:

```bash
SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true \
BASE_URL=https://growgate.54.180.56.24.sslip.io \
./scripts/smoke-prod.sh
```
