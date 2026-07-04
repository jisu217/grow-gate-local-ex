# Codex Prompt 02 — 3개 합성 기업/제품/진단 seed 적용

입력 파일:
- `01_synthetic_data/2026-07-03_seed_3_synthetic_companies.sql`
- `01_synthetic_data/synthetic_company_profiles.json`
- `01_synthetic_data/knowledge_documents_index.jsonl`
- `02_docs/wiki/*`
- `02_docs/product_docs/*`
- `02_docs/policy_docs/*`

목표:
기존 MySQL 스키마에 맞게 3개 합성 기업, 사용자, 멤버십, 제품, 제품지식, 인증, FTO, 바이어 요구, knowledge_sources, diagnosis_runs를 seed한다.

작업:
1. SQL 파일을 `database/mysql/init/03_seed_synthetic_companies.sql`로 복사한다.
2. 기존 `02_seed.sql`와 충돌하지 않도록 `ON DUPLICATE KEY UPDATE` 유지.
3. `database/README.md`에 적용 순서를 추가한다: 01_schema → 02_seed → 03_seed_synthetic.
4. `docs/demo-scenarios.md`에 세 진단 ID와 로그인 계정(owner+...@growgate.local)을 적는다.
5. `backend/BE-02/src/test`에 seed JSON parse smoke test를 추가한다.

수용 조건:
- Docker MySQL 초기화 후 `/api/diagnosis` 목록에 seed diagnosis 3개가 보인다.
- 각 diagnosis result의 `result.syntheticData=true`가 유지된다.
- `sourceTrustSummary.liveCount=0`, `seedCount>=3`이다.
