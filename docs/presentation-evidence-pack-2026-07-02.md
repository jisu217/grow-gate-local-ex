# Presentation Evidence Pack - 2026-07-02

Prepare this pack after running production smoke against the Lightsail domain.

## Required Artifacts

| Artifact | Source |
| --- | --- |
| Live public-data smoke JSON | `/tmp/growgate-smoke/live-public-data-smoke-evidence.json` |
| Source Trust Bar screenshot | Diagnosis result first screen |
| Score Contribution Ledger screenshot | S10 Evidence Workflow |
| Public data dashboard screenshot | `/api/public-data/dashboard` backed UI/API output |
| Five-minute scenario capture | PMGrow or battery-pack demo scenario |
| Markdown report | `/api/diagnosis/{id}/report` |
| Markdown report proof | `/api/diagnosis/{id}/report` payload and downloaded `.md` file |

## Five-Minute Scenario

1. Login with test account.
2. Create or select battery-pack product.
3. Run diagnosis for `DE` with HS `850760`.
4. Show Source Trust Bar: run id, live/cache/fallback count, MOTIE-related contribution.
5. Open S10 and show source ids plus score contribution ledger.
6. Download Markdown report. Show PDF only when Korean rendering has been visually confirmed.
