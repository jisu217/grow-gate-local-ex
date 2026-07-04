# GROW-GATE Local EX Database Setup

This directory prepares the latest `BE-02` Spring Boot data model for DBeaver.

## Tables

The schema is based on the current `BE-02` JPA entities:

- `companies`
- `users`
- `memberships`
- `products`
- `product_knowledge`
- `certifications`
- `fto`
- `buyer_requirements`
- `knowledge_sources`
- `audit_logs`
- `diagnosis_runs`
- judge-facing extension tables: `synthetic_data_registry`, `regional_industry_profiles`, `agent_trace_events`, `ml_prediction_runs`, `company_validation_interviews`

## Option A. Open Immediately In DBeaver With SQLite

This works on the current machine without Docker or MySQL.

```bash
python3 database/sqlite/build_sqlite_test_db.py
```

Then open DBeaver:

1. `Database` -> `New Database Connection`
2. Select `SQLite`
3. Set database file to:

```text
database/sqlite/growgate_be02_test.db
```

4. Test connection.
5. Run `database/sqlite/verify.sql` in the SQL editor.

This SQLite database is for schema browsing, sample data checks, and join-query testing. The production backend should use the MySQL setup below.

## Option B. Run MySQL For Spring Boot And DBeaver

Prerequisites:

- Docker Desktop or another Docker runtime
- DBeaver

Start MySQL:

```bash
cd database
cp .env.example .env
docker compose up -d
```

Fresh clone에서 Docker DB와 Spring `ddl-auto=validate`가 같이 뜨는지 한 번에 확인하려면 root에서 아래 스크립트를 실행합니다. 이 스크립트는 별도 Compose project와 기본 포트 `33306`을 사용하고 종료 시 임시 volume을 삭제하므로 기존 로컬 DB를 건드리지 않습니다.

```bash
./scripts/verify-fresh-db.sh
```

DBeaver connection:

| Field | Value |
| :--- | :--- |
| Driver | MySQL |
| Host | `127.0.0.1` |
| Port | `3306` |
| Database | `growgate` |
| Username | `growgate_app` |
| Password | `growgate_app` |

Run the verification query:

```sql
SOURCE database/mysql/verify.sql;
```

If DBeaver does not support `SOURCE` in your SQL editor, open `database/mysql/verify.sql` and run the file contents directly.

## Spring Boot Environment

Use these values when running `backend/BE-02`:

```bash
export DB_URL="jdbc:mysql://localhost:3306/growgate?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"
export DB_USERNAME="growgate_app"
export DB_PASSWORD="growgate_app"
export JWT_SECRET="replace-with-base64-32-byte-secret"
```

Example JWT secret generation:

```bash
openssl rand -base64 32
```

Seed users are included for DB-level testing:

| Email | Role | Note |
| :--- | :--- | :--- |
| `admin@growgate.com` | `owner` | presentation/demo admin for the sample company |
| `owner@growgate.local` | `owner` | owns sample company and product |
| `viewer@growgate.local` | `viewer` | read-only sample member |
| `owner+battery@growgate.local` | `owner` | synthetic_demo_not_real_customer 창원 배터리팩 |
| `owner+marine@growgate.local` | `owner` | synthetic_demo_not_real_customer 부산 마린 액추에이터 |
| `owner+medical@growgate.local` | `owner` | synthetic_demo_not_real_customer 대구 메디센서 |

The presentation admin password is `growgateadmin`. The backend also upserts this account on startup while `APP_DEMO_ADMIN_ENABLED=true`, so existing demo DB volumes can recover the login without a full reset. Set `APP_DEMO_ADMIN_ENABLED=false` outside local/demo environments. The legacy local seed users still use the bcrypt test hash for `password`. Use these accounts only for local development and demo reset environments.

MySQL init order:

```text
01_schema.sql
02_seed.sql
03_agent_ml_regional_schema.sql
04_seed_regional_and_registry.sql
05_seed_synthetic_companies.sql
06_public_api_proof_ml_validation.sql
07_seed_synthetic_persona_validation.sql
```

The three `owner+...@growgate.local` accounts are synthetic judge scenarios. They use the same demo password, `growgateadmin`, and must be presented with the `synthetic_demo_not_real_customer` limitation. Details are in [../docs/demo-scenarios.md](../docs/demo-scenarios.md).

`06_public_api_proof_ml_validation.sql` adds proof-run, raw-response hash, normalized proof, score waterfall, ML metadata, and synthetic persona validation tables. `07_seed_synthetic_persona_validation.sql` inserts persona-based validation rows and keeps `synthetic_disclaimer=TRUE`.

## Resetting MySQL

To delete all local MySQL data and re-run the init SQL:

```bash
cd database
docker compose down -v
docker compose up -d
```
