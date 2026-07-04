# Database Migration Readiness

## Current DB

The Spring submission backend uses MySQL through `spring-boot-starter-data-jpa` and `mysql-connector-j`.

Local Docker DB lives in [database/docker-compose.yml](../database/docker-compose.yml). It starts MySQL 8.4 and mounts [database/mysql/init](../database/mysql/init) into `/docker-entrypoint-initdb.d`.

## Profile Behavior

| Profile | DB Setup | Startup Behavior |
| :--- | :--- | :--- |
| default/local/demo | MySQL expected at `DB_URL`; Docker Compose is the supported path | Starts after init SQL has created the schema. |
| prod | Managed MySQL expected | Must use an already migrated schema. |

There is no H2 fallback for the Spring submission backend. If MySQL is not running, the app should fail visibly instead of pretending to run with missing persistence.

## Migration Status

| Item | Status |
| :--- | :--- |
| Flyway | Not present |
| Liquibase | Not present |
| JPA `ddl-auto` | `${JPA_DDL_AUTO:validate}` |
| Fresh Docker schema | Created once by MySQL init SQL on a new volume |
| Existing DB upgrade automation | Manual SQL/init-script discipline, not automatic migration |

Answer to “DB migration은 자동인가?”:

> Fresh Docker MySQL volumes apply the SQL files under `database/mysql/init` automatically on first creation. Existing databases do not have Flyway/Liquibase migration automation yet. Spring uses `ddl-auto=validate` by default, so schema drift fails startup instead of silently changing production tables.

## Clean Machine DB Preparation

```bash
cd database
cp .env.example .env
docker compose up -d
docker compose ps
```

Then run the backend with:

```bash
cd backend/BE-02
export DB_URL="jdbc:mysql://127.0.0.1:3306/growgate?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"
export DB_USERNAME="growgate_app"
export DB_PASSWORD="growgate_app"
export JWT_SECRET="unsafe-local-demo-secret-change-before-prod-32bytes"
./gradlew bootRun
```

Verify:

```bash
curl -s http://localhost:8080/api/health
curl -s http://localhost:8080/api/public-data/health
```

## Remaining Risk

- Existing DB schema changes require manual SQL management until Flyway or Liquibase is introduced.
- `docker compose up -d` requires Docker and an available MySQL port.
- `./gradlew` may need network access the first time it downloads Gradle 8.13.
