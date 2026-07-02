# Security Threat Model: CSRF and Login Rate Limit

## Scope

This document explains the current submission-server security model for cookie authentication, CSRF, and login throttling.

## CSRF Design

GrowGate uses stateless JWT authentication with an HttpOnly `gg_access_token` cookie. Spring Security's built-in CSRF protection is optimized for server-side sessions and `CsrfTokenRepository` flows. The app instead uses a stateless double-submit pattern:

- Server sets `gg_access_token` as `HttpOnly`, `SameSite=Lax`.
- Server sets `gg_csrf_token` as readable by JavaScript, `SameSite=Lax`.
- Frontend reads `gg_csrf_token` and sends it as `X-GG-CSRF` on unsafe methods.
- `CsrfCookieFilter` rejects unsafe cookie-authenticated requests when cookie and header do not match.
- Bearer token compatibility is retained for API tooling, but the browser login path uses cookies.

## Tests

`CsrfCookieFilterTest` verifies:

- unsafe cookie-authenticated requests without `X-GG-CSRF` are rejected with 403
- unsafe requests with matching cookie/header pass to the filter chain
- safe authenticated requests receive a readable CSRF cookie

## Login Rate Limit

`LoginAttemptRateLimiter` is intentionally in-memory for the single-instance Lightsail submission server. It blocks repeated failed attempts per normalized email and clears the counter on successful login.

This is not a distributed rate limiter. If GrowGate is deployed on multiple backend instances, the release gate is:

- replace the in-memory store with Redis or another shared TTL store
- key by normalized email plus client network signal
- expose metrics for blocked login attempts
- add alerting when throttling spikes

## Presentation Answer

If asked why this is not Spring Security CSRF:

> The app is stateless and uses HttpOnly JWT cookies, so we use a double-submit CSRF filter matched to the cookie model. It is tested at filter level. For multi-instance commercial deployment, login throttling moves from the current single-instance in-memory store to Redis.
