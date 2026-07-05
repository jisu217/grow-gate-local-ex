# GrowGate E2E Demo Reel - 2026-07-05

## Artifact

- GIF: `growgate-e2e-source-bound-demo.gif`
- Contact sheet: `contact-sheet.jpg`
- Frames: `01-login.png` through `06-action-board.png`
- Production URL tested: `https://growgate.54.180.56.24.sslip.io/`

## Scenario

Company user checks whether a lithium-ion battery pack can move forward for an EU buyer inquiry.

The product is intentionally set up as a realistic pre-quote risk case:

- Product: lithium-ion battery pack
- HS Code: 850760
- Target market: EU
- Buyer asks for CE, UN38.3, RoHS, FTO status, and 4-week sample shipment
- Current documents are incomplete, so the correct result is not GO but HOLD with evidence gaps and owner tasks

## Reel Beats

1. Login starts from a clean company-user entry point.
2. My Page shows the registered product workspace and product readiness status.
3. Gate input captures the buyer inquiry, deadline, shipment date, requested certifications, current documents, export experience, and review budget.
4. Diagnosis overview shows a rule-owned HOLD decision, source trust summary, and next actions.
5. Source proof view shows live/cache/fallback separation and public-data proof status.
6. Execution board turns evidence gaps into owner/action/output tasks.

## Presentation Script

Use this short script while showing the GIF:

> GrowGate does not claim that AI decides export approval. The final Gate is rule-owned. The agent workflow collects and explains source-bound evidence, separates live/cache/fallback status, and blocks positive scoring when evidence is missing or not configured. In this demo, the battery pack inquiry is held before quotation because FTO and certification evidence are incomplete. The result is not just a warning. The system turns the evidence gaps into concrete actions for the representative, quality owner, and external specialist.

## Safe Claims

- Final Gate is deterministic and rule-owned.
- LLM output is advisory only.
- Public data is separated into live, cache, fallback, empty, seed, and heuristic evidence.
- Missing or fallback evidence leads to HOLD or human review, not automatic GO.
- Source trace and proof surfaces explain why the decision was made.
