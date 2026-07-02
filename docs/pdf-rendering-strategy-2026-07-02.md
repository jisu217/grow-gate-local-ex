# PDF Rendering Strategy - 2026-07-02

## Current Policy

GrowGate exposes one presentation-ready report surface:

- `/api/diagnosis/{id}/report`: stable Markdown payload encoded as UTF-8

The previous minimal PDF endpoint used a Latin font path and could not prove Korean rendering quality. It has been removed from the presentation API surface. The demo UI exposes Markdown download as the customer-facing artifact.

## Fallback Policy

Do not claim PDF quality in the presentation until Korean font embedding is implemented and visually verified. If PDF is reintroduced, it must use an embedded Korean-capable font, include a downloaded sample in the evidence pack, and be covered by smoke and visual QA.

## Evidence

`scripts/smoke-prod.sh` checks the Markdown report payload, file name, content type, and Korean-safe UTF-8 response path. PDF evidence should not be included unless a future font-embedded renderer passes visual inspection.
