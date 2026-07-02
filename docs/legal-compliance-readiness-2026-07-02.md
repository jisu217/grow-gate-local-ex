# Legal/Compliance Readiness - 2026-07-02

> This is an engineering readiness checklist, not a legal opinion, statutory certification, or official compliance audit.

## Scope

- Signup/login personal-data handling.
- Consent evidence stored at account creation.
- Optional Google Sign-In decision.
- Public demo server posture before contest review.

## Applied Controls

| Area | Control | Implementation |
| --- | --- | --- |
| Required consent | Signup cannot proceed without Terms and Privacy consent. | `SignupRequest.termsAccepted`, `SignupRequest.privacyAccepted` use server-side validation. |
| Optional consent | Marketing consent is separate and optional. | `SignupRequest.marketingAccepted`; frontend copy states signup is allowed without it. |
| Consent evidence | Store policy version, selected flags, source, acceptedAt. | `user_consent_records` table and `UserConsentRecord` entity. |
| Data minimization | Google SSO is not enabled by default. | `GOOGLE_SSO_ENABLED=false`; no Google profile data is collected unless explicitly released. |
| Token storage | Access token stays in HttpOnly cookie path. | Existing auth risk gate blocks localStorage access token persistence. |
| CSRF | Cookie auth uses double-submit CSRF header. | `gg_csrf_token` + `X-GG-CSRF`. |
| Payment/refund | No paid plan or refund feature is shipped. | README states billing/refund must be a separate release gate before monetization. |

## Signup Consent Records

Stored fields:

- `user_id`
- `policy_version`
- `terms_accepted`
- `privacy_accepted`
- `marketing_accepted`
- `accepted_at`
- `withdrawn_at`
- `source`

Default policy version:

```text
2026-07-privacy-v1
```

Deployment order:

1. Apply `database/mysql/migrations/2026-07-02_user_consent_records.sql`.
2. Deploy the Spring Boot jar.
3. Deploy the frontend build with the signup consent checklist.
4. Run `scripts/smoke-prod.sh`; signup payload now includes consent evidence fields.

## Personal Data Inventory

| Data | Purpose | Source | Notes |
| --- | --- | --- | --- |
| Email | Login/account identity | Signup | Unique account key. |
| Password hash | Authentication | Signup | Raw password is not stored. |
| Name | Account display/contact | Signup | Required in current flow. |
| Company name/business number/industry | Company account and ownership scope | Company signup | Company signup only. |
| Product/export knowledge | Diagnosis and source-traced readiness review | User input | Company/user scoped APIs. |
| Public-data raw payload | Evidence trace and parser debug | Public APIs/crawl | Needs retention/masking policy before commercial launch. |

## Google Sign-In Decision

Current decision: do not enable Google Sign-In for the contest demo by default.

Reasoning:

- The current password + HttpOnly cookie flow already works for the demo.
- Google Sign-In would introduce additional OAuth consent screen, authorized origin, privacy policy URL, ID-token verification, and account-linking requirements.
- Google Identity documentation recommends separating authentication from later data authorization and asking for extra data access only when needed. For GrowGate, this means Google Sign-In can be optional SSO, while Google Drive/Docs access should be a later, contextual authorization flow.

Release gate before enabling:

1. Register OAuth client with the final production domain.
2. Publish privacy policy and terms URLs.
3. Verify Google ID token on the server; do not trust frontend profile data.
4. Store only provider, subject id, email, and name required for account linking.
5. Add account unlink and data deletion request handling.
6. Update consent text to disclose Google account profile data use.

References:

- Google Identity Services overview: https://developers.google.com/identity/gsi/web/guides/overview
- Google OAuth web-server guidance: https://developers.google.com/identity/protocols/oauth2/web-server
- 개인정보보호위원회 guidance portal: https://www.pipc.go.kr/

## Remaining Formal Review Items

- Have a Korean privacy/legal professional review the final privacy policy and terms.
- Define public-data raw payload retention period and deletion process.
- Define data-subject request workflow: access, correction, deletion, processing suspension.
- Add admin UI or documented runbook for marketing-consent withdrawal.
- Add payment/refund policy only when a paid product is introduced.
