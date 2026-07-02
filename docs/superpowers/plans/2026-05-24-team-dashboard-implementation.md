# Team Dashboard Implementation Plan

> Archive note, 2026-07-03: this plan describes the removed static frontend prototype. The judged app is now `frontend/FE-02`; the old `grow_gate_local_ex_demo/frontend` path is preserved only in `artifacts/legacy-backups/legacy-demo-and-mock-backup-2026-07-03.zip` and is excluded from submission packages.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add team-facing strategy and code-gate dashboards to the existing GROW-GATE Local EX static frontend.

**Architecture:** Create two new static pages, `frontend/team.html` and `frontend/code-gate.html`, backed by one focused script, `frontend/team.js`. Reuse the existing global navigation, CSP, visual tokens, panel language, and file-based demo behavior.

**Tech Stack:** HTML, CSS, vanilla JavaScript, existing local static frontend, optional Python `http.server` for verification.

---

## File Structure

- Modify `grow_gate_local_ex_demo/frontend/index.html`: add navigation links for `팀 운영실` and `코드 게이트`.
- Modify `grow_gate_local_ex_demo/frontend/workspace.html`: add the same navigation links.
- Modify `grow_gate_local_ex_demo/frontend/data.html`: add the same navigation links.
- Modify `grow_gate_local_ex_demo/frontend/roadmap.html`: add the same navigation links.
- Modify `grow_gate_local_ex_demo/frontend/styles.css`: add dashboard-specific layout, score, action, checklist, and responsive styles.
- Create `grow_gate_local_ex_demo/frontend/team.html`: team operations dashboard.
- Create `grow_gate_local_ex_demo/frontend/code-gate.html`: code workflow dashboard.
- Create `grow_gate_local_ex_demo/frontend/team.js`: static data, rendering helpers, action simulator behavior, and code-gate checklist/button behavior.

## Task 1: Add Navigation Surface

**Files:**
- Modify: `grow_gate_local_ex_demo/frontend/index.html`
- Modify: `grow_gate_local_ex_demo/frontend/workspace.html`
- Modify: `grow_gate_local_ex_demo/frontend/data.html`
- Modify: `grow_gate_local_ex_demo/frontend/roadmap.html`

- [ ] **Step 1: Update each `.global-nav` link group**

Insert these two links after `실행 로드맵` on existing pages:

```html
<a href="team.html">팀 운영실</a>
<a href="code-gate.html">코드 게이트</a>
```

On `team.html`, mark `팀 운영실` active:

```html
<a class="active" href="team.html">팀 운영실</a>
```

On `code-gate.html`, mark `코드 게이트` active:

```html
<a class="active" href="code-gate.html">코드 게이트</a>
```

- [ ] **Step 2: Verify navigation strings**

Run:

```bash
rg -n "팀 운영실|코드 게이트" grow_gate_local_ex_demo/frontend/*.html
```

Expected: every HTML page includes both labels, with one active label on each new page.

## Task 2: Build Team Operations Dashboard

**Files:**
- Create: `grow_gate_local_ex_demo/frontend/team.html`
- Modify: `grow_gate_local_ex_demo/frontend/team.js`
- Modify: `grow_gate_local_ex_demo/frontend/styles.css`

- [ ] **Step 1: Create semantic page shell**

Create `team.html` with the same CSP and stylesheet reference as existing pages, plus `defer` loading for `team.js`:

```html
<script src="team.js" defer></script>
```

The body should include:

```html
<body class="home-page team-page" data-dashboard="team">
```

Required element IDs for JS rendering:

```html
<div id="teamDataSources"></div>
<div id="missingDataList"></div>
<div id="stakeholderScores"></div>
<div id="awardFactors"></div>
<div id="actionSimulator"></div>
<div id="selectedActionInsight"></div>
```

- [ ] **Step 2: Add static data and render functions in `team.js`**

Define these top-level constants:

```js
const dataSources = [];
const missingData = [];
const stakeholderScores = [];
const awardFactors = [];
const actionSimulations = [];
```

Implement helpers:

```js
function qs(selector) {
  return document.querySelector(selector);
}

function renderTeamDashboard() {
  renderDataSources();
  renderMissingData();
  renderStakeholderScores();
  renderAwardFactors();
  renderActionSimulator();
}
```

- [ ] **Step 3: Render action simulator behavior**

Each action card should be a `<button>` and should call:

```js
selectAction(actionSimulations[0].id);
```

`selectAction(id)` updates `#selectedActionInsight` with effect, score delta, reason, effort, and owner. Positive actions use class `positive`, neutral actions use `neutral`, negative actions use `negative`.

- [ ] **Step 4: Add CSS for dashboard layout**

Add styles for:

```css
.team-page .global-nav > div { flex-wrap: wrap; }
.team-hero {}
.team-metrics {}
.team-grid {}
.strategy-panel {}
.source-list {}
.role-score-grid {}
.action-simulator {}
.action-card {}
.action-card.positive {}
.action-card.neutral {}
.action-card.negative {}
.insight-box {}
```

Use responsive grid breakpoints so the page works at desktop and mobile widths.

- [ ] **Step 5: Browser smoke test**

Open:

```text
grow_gate_local_ex_demo/frontend/team.html
```

Expected:

- No blank sections.
- Stakeholder roles show scores and evidence.
- Clicking action cards updates the insight panel.
- The code status summary links to `code-gate.html`.

## Task 3: Build Code Gate Dashboard

**Files:**
- Create: `grow_gate_local_ex_demo/frontend/code-gate.html`
- Modify: `grow_gate_local_ex_demo/frontend/team.js`
- Modify: `grow_gate_local_ex_demo/frontend/styles.css`

- [ ] **Step 1: Create semantic code-gate page shell**

Create `code-gate.html` with:

```html
<body class="home-page team-page code-gate-page" data-dashboard="code-gate">
<script src="team.js" defer></script>
```

Required element IDs:

```html
<div id="reviewFocus"></div>
<div id="reviewChecklist"></div>
<div id="preCommitChecklist"></div>
<div id="commitGuidance"></div>
<div id="rollbackVersions"></div>
<div id="codeGateStatus"></div>
```

- [ ] **Step 2: Add code-gate data and render functions**

Define:

```js
const codeGate = {
  reviewFocus: [],
  reviewChecklist: [],
  preCommitChecklist: [],
  commitCommands: [],
  rollbackVersions: []
};
```

Implement:

```js
function renderCodeGate() {
  renderReviewFocus();
  renderReviewChecklist();
  renderPreCommitChecklist();
  renderCommitGuidance("review");
  renderRollbackVersions();
  updateCodeGateStatus();
}
```

- [ ] **Step 3: Add local workflow buttons**

Buttons should update UI state only:

```js
const gateState = {
  reviewRequested: false,
  reviewCompleted: false,
  preCommitChecked: false,
  commitReady: false,
  rollbackOpen: false
};
```

Implement:

```js
function setGateState(key, value) {
  gateState[key] = value;
  updateCodeGateStatus();
}
```

Labels must avoid implying direct Git execution:

```html
리뷰 요청 표시
리뷰 완료 표시
커밋 명령 보기
철회 가이드 열기
```

- [ ] **Step 4: Add safe command guidance**

The command block should include review and commit commands as guidance, not executable browser actions:

```bash
git status --short
git diff -- grow_gate_local_ex_demo/frontend
git add grow_gate_local_ex_demo/frontend/team.html grow_gate_local_ex_demo/frontend/code-gate.html grow_gate_local_ex_demo/frontend/team.js grow_gate_local_ex_demo/frontend/styles.css
git commit -m "feat: add team operations dashboards"
```

Rollback guidance should prefer:

```bash
git revert <commit-sha>
```

The page should explicitly say destructive reset is not automated.

- [ ] **Step 5: Browser smoke test**

Open:

```text
grow_gate_local_ex_demo/frontend/code-gate.html
```

Expected:

- Review focus, checklist, command guidance, rollback versions render.
- Buttons change status text and do not claim to execute Git.
- Checklist items can be toggled without page errors.

## Task 4: Visual And Static Verification

**Files:**
- Read: all modified frontend files

- [ ] **Step 1: Search for broken labels and placeholders**

Run:

```bash
rg -n "미완성|임시문구|브라우저에서 Git 실행" grow_gate_local_ex_demo/frontend
```

Expected: no production-facing placeholder text or misleading Git execution wording.

- [ ] **Step 2: Run a local static server**

Run:

```bash
cd grow_gate_local_ex_demo
python -m http.server 8765
```

Open:

```text
http://127.0.0.1:8765/frontend/team.html
http://127.0.0.1:8765/frontend/code-gate.html
```

Expected: both pages render with local JS active.

- [ ] **Step 3: Check responsive behavior**

At desktop and mobile widths:

- Navigation wraps without overlap.
- Score cards and role cards stack cleanly.
- Long Korean text wraps inside panels.
- Buttons remain readable.

- [ ] **Step 4: Run backend tests if backend changed**

Backend is not expected to change. If it does, run:

```bash
cd grow_gate_local_ex_demo
pytest
```

Expected: existing tests pass.

## Self-Review

Spec coverage:

- Public-data coverage: Task 2 data-source and missing-data sections.
- Stakeholder scoring: Task 2 stakeholder score grid.
- Award probability: Task 2 award factor section.
- Action simulator: Task 2 action simulator and selected insight panel.
- Git/code workflow: Task 3 code gate page.
- Navigation: Task 1.
- Safety limits for Git execution: Task 3 command guidance and labels.
- Visual verification: Task 4.

Placeholder scan:

- No unfinished markers or undefined future work appears in the implementation steps.

Type consistency:

- Required IDs in HTML match render function names and dashboard routing in `team.js`.
