# Submission Scope

Judged source scope:

- `frontend/FE-02`
- `backend/BE-02`
- `database`
- `docs`
- `scripts`
- `grow_gate_local_ex_demo/backend` only as optional Python data/AI worker

Excluded from submission zip:

- `.env` files and local secrets
- `node_modules`, Gradle caches, virtual environments
- build output
- legacy backups and presentation runtime artifacts
- zip archives

Synthetic data:

- `docs/judge-solution-pack` preserves the received judge solution pack.
- `docs/synthetic/02_docs` mirrors the synthetic wiki/product/policy documents referenced by seed SQL.
- `database/mysql/init/05_seed_synthetic_companies.sql` inserts three repeatable demo companies.
- Every synthetic demo artifact must retain `synthetic_demo_not_real_customer` semantics.

Run order:

1. `database/mysql/init/01_schema.sql`
2. `database/mysql/init/02_seed.sql`
3. `database/mysql/init/03_agent_ml_regional_schema.sql`
4. `database/mysql/init/04_seed_regional_and_registry.sql`
5. `database/mysql/init/05_seed_synthetic_companies.sql`

One-command checks:

```bash
./scripts/check-risk-gates.sh
./scripts/check-submission-zip.sh path/to/submission.zip
```
