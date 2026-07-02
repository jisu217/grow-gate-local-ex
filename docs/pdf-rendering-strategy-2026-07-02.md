# PDF Rendering Strategy - 2026-07-02

## Current Policy

GrowGate supports two report surfaces:

- `/api/diagnosis/{id}/report`: stable Markdown payload
- `/api/diagnosis/{id}/report.pdf`: binary PDF endpoint verified by smoke test

The demo UI should continue to expose Markdown download as the safest default. PDF can be shown in presentation only after the production smoke confirms:

- HTTP 200
- `Content-Type` is PDF-compatible
- response begins with `%PDF-`
- Korean text is legible in the downloaded file

## Fallback Policy

If Korean font rendering is not verified on the deployed server, do not claim PDF quality in the presentation. Use Markdown report download and show `/report.pdf` only as an implementation endpoint that is still font-QA gated.

## Evidence

`scripts/smoke-prod.sh` already checks `/report.pdf` and fails if the response is not a PDF binary. Add a screenshot or downloaded sample to the presentation evidence pack only after visual inspection.

