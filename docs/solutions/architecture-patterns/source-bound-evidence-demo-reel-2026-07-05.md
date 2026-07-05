---
title: Source-bound evidence demo reel scenario
date: 2026-07-05
category: docs/solutions/architecture-patterns
module: demo-reel
problem_type: architecture_pattern
component: documentation
severity: medium
applies_when:
  - "A contest or stakeholder demo needs to show AI-assisted diagnosis without overstating autonomous AI"
  - "The demo should connect login, product setup, Gate diagnosis, source proof, and execution tasks"
tags: [demo-reel, source-bound, gate, contest, evidence]
---

# Source-bound evidence demo reel scenario

## Context

The strongest GrowGate demo is not a perfect GO. A HOLD case shows the product's actual philosophy: source-bound evidence, deterministic Gate ownership, and practical follow-up tasks before quotation.

## Guidance

Use a realistic pre-quote product with enough risk to make the Gate useful:

- Lithium-ion battery pack
- HS Code 850760
- EU buyer inquiry
- CE, UN38.3, RoHS, FTO, and 4-week sample shipment pressure
- Partial documents only

The reel should move through six beats:

1. Login as a company user.
2. Show the product workspace in My Page.
3. Enter the buyer inquiry and requested documents.
4. Show the rule-owned HOLD decision.
5. Show source proof with live/cache/fallback separation.
6. Show the execution board turning evidence gaps into owner tasks.

## Why This Matters

This avoids two weak demos:

- A synthetic GO that hides risk and makes the tool look like a shallow checklist.
- An overclaimed AI demo that suggests the LLM owns export approval.

The HOLD scenario makes the value proposition concrete: the user avoids premature quotation, sees exactly what evidence is missing, and gets a work plan.

## When to Apply

- Public-data proof or source quality is a key judging criterion.
- The audience may challenge whether the AI is overclaiming.
- The product needs to show operational usefulness, not just a report page.

## Examples

Current artifact:

- `artifacts/demo-reel/growgate-e2e-20260705/growgate-e2e-source-bound-demo.gif`
- `artifacts/demo-reel/growgate-e2e-20260705/README.md`

Safe narration:

> The final Gate is rule-owned. The agent workflow collects and explains source-bound evidence, separates live/cache/fallback status, and turns evidence gaps into owner tasks.

## Related

- `docs/solutions/architecture-patterns/source-bound-evidence-agent-workflow-2026-07-05.md`
- `artifacts/demo-reel/growgate-e2e-20260705/README.md`
