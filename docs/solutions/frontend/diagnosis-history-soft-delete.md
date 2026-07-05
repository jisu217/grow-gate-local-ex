# Diagnosis History Soft Delete

## Context

The diagnosis history page needs an operator-facing way to remove stale or failed Gate runs before judge demos. Deleting a card must not break evidence auditability, shared company scoping, or the synthetic demo seed scenarios used for presentation.

## Decision

Use soft delete for diagnosis history:

- `DiagnosisRun.deletedAt` records when the history item was hidden.
- `DiagnosisRun.deletedBy` records the user that requested deletion.
- List, detail, report, sources, and agent trace lookups only resolve rows where `deletedAt` is null.
- Watermarked `synthetic_demo_not_real_customer` seed diagnoses cannot be deleted through the user-facing API.

This keeps the UI clean without treating demo seed data as disposable real-customer records.

## Backend Contract

`DELETE /api/diagnosis/{diagnosisId}` returns `204 No Content` for an owned diagnosis and then hides it from normal reads. Company users delete within their company scope; personal users delete within their own user scope.

The delete path intentionally does not use the synthetic cross-account read fallback. That fallback exists only so judge/demo accounts can view watermarked synthetic scenarios.

## Frontend Pattern

The history card remains clickable for detail navigation. The delete button:

- stops card click propagation,
- opens a confirmation dialog,
- calls the delete endpoint,
- removes the item from the current list in memory,
- adjusts pagination when the last item on a page is removed.

## Verification

Commands run:

```bash
cd backend/BE-02
./gradlew test

cd frontend/FE-02
npm run test:mappers
npm run build
```

## Prevention

Do not hard-delete `diagnosis_runs` from the app UI. Evidence rows, source trace, reports, and future audit views may still need the deleted diagnosis ID for operational debugging.
