# Team Dashboard Design

Date: 2026-05-24
Project: GROW-GATE Local EX

## Goal

Build two team-facing dashboard pages for the current static frontend prototype:

1. `team.html`: a team operations dashboard for public-data coverage, planning maturity, stakeholder scoring, award probability, and score-improvement actions.
2. `code-gate.html`: a development collaboration dashboard for review readiness, review completion, pre-commit checks, final commit guidance, rollback guidance, and rollback-version notes.

The pages should help non-developer teammates, planners, and developers understand project readiness without reading source code or raw data files.

## Existing Context

The current project is a local demo app under `grow_gate_local_ex_demo`:

- Static frontend: `frontend/index.html`, `workspace.html`, `data.html`, `roadmap.html`, `styles.css`, `app.js`
- Python backend: FastAPI analysis APIs, public-data pipeline, agents, schema validation
- Data: `data/public_data_sources.json`, `data/sample_datasets.json`, normalized build outputs
- Visual language: dark technical dashboard, cyan/green accent, glass panels, dense operational layout

There is no Git repository detected in the current workspace root, so the design document cannot be committed until the folder is initialized or moved under a repository.

## Product Structure

### Navigation

Add two new entries to the existing global navigation:

- `팀 운영실` -> `team.html`
- `코드 게이트` -> `code-gate.html`

All existing pages should include these links so teammates can move between the product demo, data page, roadmap, team strategy dashboard, and code workflow dashboard.

### Team Operations Dashboard

The `team.html` page is a one-page operations room. It should prioritize fast scanning over deep editing.

Required sections:

- Public-data coverage: show already used data sources, normalized tables, update cadence, source reliability, and missing data.
- Data insight: explain which current sources are high-value, which are weak, and what evidence each product screen depends on.
- Planning maturity: score the project from many stakeholder viewpoints with evidence and weakness notes.
- Stakeholder scoring: include at least public-contest judge, doctoral practitioner, actual export-support officer, SME CEO, large-enterprise CEO, minister-level policy viewer, undergraduate student, graduate student, exporter, importer, certification lab, investor, and regional government officer.
- Award probability: show current estimated award probability, confidence, main reasons, and risk factors.
- Action simulator: show proposed actions, whether each action helps or hurts, why, expected score movement, effort, and recommended owner.
- Code status summary: show review waiting count, pre-commit readiness, rollback availability, and link to `code-gate.html`. Detailed code operations stay out of this page.

The scoring language must be evidence-based, not overconfident. Award probability is a planning heuristic, not a guaranteed prediction.

### Code Gate Dashboard

The `code-gate.html` page is a separate development workflow surface.

Required sections:

- Review request: who should review, review focus, changed areas, and urgency.
- Review core: checklist for security, data evidence, UI quality, backend/API integrity, tests, and presentation risk.
- Review done: visible criteria for marking review complete.
- Pre-commit checklist: static HTML review, responsive check, backend test, data source check, screenshot/visual QA, and documentation update.
- Final commit: recommended commit message, files to inspect, and terminal command guidance.
- Commit rollback: explain safe rollback options, including `git revert`, local backup before rollback, and avoiding destructive reset unless explicitly agreed.
- Rollback version notes: list candidate versions or checkpoints as manually maintained cards in the prototype.

Because browser JavaScript cannot safely execute local Git commands from a static file, the page should use buttons to update UI state, reveal checklists, and show command guidance. Actual Git execution remains a terminal action or future backend integration.

## Data Model

Keep the first implementation static and local. Use hardcoded data objects in a new frontend script or inline script only if consistent with the current CSP. The preferred implementation is a new `team.js` file loaded by both pages.

Suggested objects:

- `dataSources`: source name, provider, target table, used screens, status, reliability, gap note
- `missingData`: missing item, impact, owner, priority, next action
- `stakeholderScores`: role, score, confidence, evidence, concern, improvement
- `awardSignals`: current probability, confidence, positive evidence, risks, assumptions
- `actionSimulations`: action, effect type, point delta, reason, effort, owner
- `codeGate`: review items, pre-commit checks, commit guidance, rollback versions

This static data should be derived from existing project materials where possible:

- `data/public_data_sources.json`
- `docs/architecture.md`
- README public-data and expansion sections
- Existing frontend screens and analysis outputs

## Interaction Design

`team.html` interactions:

- Click an action simulation card to show whether the action increases or decreases score and why.
- Use compact filters or segmented controls for stakeholder role groups if space requires it.
- Show score deltas visually with positive, neutral, and negative states.

`code-gate.html` interactions:

- Buttons toggle workflow state: review requested, review completed, pre-commit checked, final commit ready, rollback selected.
- Checklist items can be checked locally in the page state.
- Command guidance appears in a read-only code block after the relevant button is selected.
- Rollback version cards show risk and intended use case.

Use local in-page state only. No persistence is required for this prototype.

## Visual Design

Extend the existing GROW-GATE design system:

- Keep the dark operational background, global navigation, cyan/green accents, and existing panel language.
- Use a denser dashboard structure than a marketing page.
- Avoid large decorative hero sections; the pages are team tools.
- Use charts made from CSS bars/rings and compact tables instead of introducing a charting dependency.
- Keep cards at or below 8px radius where new dashboard-specific components allow it, while respecting existing global style where inherited.

Visual thesis: a compact export-strategy command room with clear evidence, sober scoring, and obvious next actions.

## Error Handling And Safety

- Do not present award probability as a factual prediction. Label it as a heuristic estimate based on current prototype evidence.
- Do not imply Git actions execute when they only change UI state. Buttons must use labels such as `체크 완료 표시`, `명령 보기`, or `철회 가이드 열기` if direct execution is not implemented.
- Warn that destructive rollback commands are not automated.
- Keep all content local and compatible with file-based demo mode.

## Testing And Verification

Implementation should be verified by:

- Opening `frontend/team.html` and `frontend/code-gate.html` directly from the filesystem.
- Checking desktop and mobile widths for text overflow and panel overlap.
- Clicking every action simulator and code-gate button.
- Running existing backend tests if backend code is touched.
- Running a simple static server if browser file mode blocks script loading.

## Out Of Scope

- Real Git execution from the browser
- User authentication and role permissions
- Persistent team state storage
- Live GitHub API integration
- Real probabilistic award model trained on contest results

These can be future backend or GitHub integration features.
