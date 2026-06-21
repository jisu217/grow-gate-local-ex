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
| `owner@growgate.local` | `owner` | owns sample company and product |
| `viewer@growgate.local` | `viewer` | read-only sample member |

The seeded password hash is a bcrypt test hash for `password`. Use it only for local development.

## Resetting MySQL

To delete all local MySQL data and re-run the init SQL:

```bash
cd database
docker compose down -v
docker compose up -d
```
