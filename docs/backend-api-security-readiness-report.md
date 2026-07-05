# Backend/API/Security Readiness Report

## Overall Conclusion

The backend is submission-usable when MySQL is prepared first and the app is run through the Gradle wrapper. The main clean-machine risk is not hidden security weakness; it is startup reproducibility: missing JDK 17, first-time Gradle download, missing MySQL, missing DB password, or prod JWT secret policy.

The latest changes make those failure points explicit through `scripts/verify-local.sh`, `/api/health`, env docs, and admin proof authorization.

## Changed Files

- [.env.example](../.env.example)
- [scripts/verify-local.sh](../scripts/verify-local.sh)
- [backend/BE-02/src/main/resources/application.yml](../backend/BE-02/src/main/resources/application.yml)
- [backend/BE-02/src/main/java/com/growgate/security/JwtTokenProvider.java](../backend/BE-02/src/main/java/com/growgate/security/JwtTokenProvider.java)
- [backend/BE-02/src/main/java/com/growgate/health/controller/HealthController.java](../backend/BE-02/src/main/java/com/growgate/health/controller/HealthController.java)
- [backend/BE-02/src/main/java/com/growgate/config/SecurityConfig.java](../backend/BE-02/src/main/java/com/growgate/config/SecurityConfig.java)
- [backend/BE-02/src/test/java/com/growgate/security/JwtTokenProviderTest.java](../backend/BE-02/src/test/java/com/growgate/security/JwtTokenProviderTest.java)
- [backend/BE-02/src/test/java/com/growgate/publicdata/controller/PublicDataProofControllerSecurityTest.java](../backend/BE-02/src/test/java/com/growgate/publicdata/controller/PublicDataProofControllerSecurityTest.java)
- [docs/env-and-secret-runbook.md](env-and-secret-runbook.md)
- [docs/database-migration-readiness.md](database-migration-readiness.md)
- [docs/security-auth-policy.md](security-auth-policy.md)

## Clean Machine Run Path

Fast verification:

```bash
./scripts/verify-local.sh
```

Manual backend path:

```bash
cd database
cp .env.example .env
docker compose up -d
```

```bash
cd backend/BE-02
export DB_URL="jdbc:mysql://127.0.0.1:3306/growgate?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"
export DB_USERNAME="growgate_app"
export DB_PASSWORD="growgate_app"
export JWT_SECRET="unsafe-local-demo-secret-change-before-prod-32bytes"
./gradlew bootRun
```

Health checks:

```bash
curl -s http://localhost:8080/api/health
curl -s http://localhost:8080/api/public-data/health
```

## Env/JWT_SECRET Policy

Local/demo can use `unsafe-local-demo-secret-change-before-prod-32bytes` only to avoid blocking a judge laptop smoke run. It is visibly unsafe and rejected by `prod`/`production`.

Production must set `JWT_SECRET` to at least 32 bytes. The app fails fast if the secret is blank, too short, or the local demo value.

## DB/MySQL/Migration State

MySQL is required for the Spring backend. Docker Compose provides MySQL 8.4 with healthcheck and first-volume init SQL.

There is no Flyway/Liquibase runtime. JPA defaults to `ddl-auto=validate`, so an unprepared schema blocks startup. Fresh Docker volumes get schema from `database/mysql/init`; existing DB upgrades remain a manual migration risk.

## CSRF/JWT/Admin Proof

JWT is stateless. Spring CSRF is disabled, and custom double-submit CSRF protection applies to cookie-authenticated unsafe requests.

`POST /api/admin/public-data/proof-runs` now requires `ROLE_OWNER` or `ROLE_ADMIN`. Public proof endpoints remain read-only and must return masked proof metadata.

## Agent/FastAPI Flags

Defaults:

```text
APP_AGENT_ENABLED=false
APP_FASTAPI_ENABLED=false
```

When false, health reports `mode=rule-assisted`. Do not present this as a live autonomous AI Agent.

## Verification Commands

Required:

```bash
./scripts/verify-local.sh
cd backend/BE-02 && ./gradlew test --no-daemon
cd database && docker compose config
```

Optional after starting the backend:

```bash
curl -s http://localhost:8080/api/health
curl -s http://localhost:8080/api/public-data/health
```

Current local verification on 2026-07-04:

| Command | Result | Notes |
| :--- | :--- | :--- |
| `bash -n scripts/verify-local.sh` | PASS | Script syntax is valid. |
| `cd database && docker compose config` | PASS | MySQL service, healthcheck, and init SQL mount render correctly. |
| `./scripts/check-risk-gates.sh` | PASS | All repository risk gates passed. |
| `cd backend/BE-02 && GRADLE_USER_HOME=.gradle-home ./gradlew test --tests com.growgate.publicdata.controller.PublicDataProofControllerSecurityTest --tests com.growgate.security.JwtTokenProviderTest --no-daemon` | PASS | Admin proof authorization and JWT secret policy tests passed. |
| `cd backend/BE-02 && GRADLE_USER_HOME=.gradle-home ./gradlew test --no-daemon` | PASS | Backend tests passed. |
| `./scripts/verify-local.sh` | PASS | Java, backend tests, frontend mapper tests/build, and public-data smoke passed. Health check skipped because the backend was not running; set `VERIFY_REQUIRE_HEALTH=true` after starting the app. |

## Remaining Risks

- First Gradle wrapper run needs access to `services.gradle.org` unless Gradle 8.13 is already cached.
- Spring backend has no H2 fallback; MySQL must be available.
- Existing DB migration automation is not present yet.
- Public data live proof still depends on real provider keys and approved operation endpoints.
