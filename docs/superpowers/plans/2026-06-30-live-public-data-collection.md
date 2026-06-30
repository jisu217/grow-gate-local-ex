# Live Public Data Collection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add production-shaped live public API collection to the Spring main app and feed collection records into diagnosis source traces and scoring.

**Architecture:** Introduce a Spring `publicdata` module with connector interfaces, provider implementations, persisted collection runs/records, cache-aware orchestration, and diagnosis integration. Provider calls are enabled by environment variables and degrade to explicit fallback records when keys or networks are unavailable.

**Tech Stack:** Java 17, Spring Boot 3.4, Spring Data JPA, RestTemplate, Jackson, MySQL/SQLite schema files, React docs only.

---

### Task 1: Public Data Domain Model

**Files:**
- Create: `backend/BE-02/src/main/java/com/growgate/publicdata/enums/PublicDataProvider.java`
- Create: `backend/BE-02/src/main/java/com/growgate/publicdata/enums/PublicDataCollectionStatus.java`
- Create: `backend/BE-02/src/main/java/com/growgate/publicdata/entity/PublicDataCollectionRun.java`
- Create: `backend/BE-02/src/main/java/com/growgate/publicdata/entity/PublicDataRecord.java`
- Create: `backend/BE-02/src/main/java/com/growgate/publicdata/repository/PublicDataCollectionRunRepository.java`
- Create: `backend/BE-02/src/main/java/com/growgate/publicdata/repository/PublicDataRecordRepository.java`
- Modify: `database/mysql/init/01_schema.sql`
- Modify: `database/sqlite/schema.sql`

- [ ] Add enums for providers and run status.
- [ ] Add run entity with diagnosisId, hsCode, targetCountry, requestedBy, status, counts, errorSummary, timestamps.
- [ ] Add record entity with provider, endpoint, requestKey, rawPayload, normalizedPayload, fallbackUsed, scoreContribution, fetchedAt, expiresAt, errorMessage.
- [ ] Add repositories for latest cache lookup and run history.
- [ ] Add MySQL/SQLite tables and indexes.

### Task 2: Connector Layer

**Files:**
- Create: `backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataRequest.java`
- Create: `backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataConnectorResult.java`
- Create: `backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataConnector.java`
- Create: `backend/BE-02/src/main/java/com/growgate/publicdata/service/AbstractPublicDataConnector.java`
- Create: provider connector classes for KCS, KOTRA, Safety, KIPRIS, Bizinfo.
- Modify: `backend/BE-02/src/main/resources/application.yml`
- Modify: `backend/BE-02/src/main/resources/application.yml.example`

- [ ] Define connector request and result value objects.
- [ ] Implement shared RestTemplate GET helper with URI query construction, timeout inherited from `HttpClientConfig`, and error capture.
- [ ] Implement provider connectors with env URL/key, provider-specific request params, normalized fields, and explicit key-missing fallback.
- [ ] Add environment properties for public data enablement, TTL, URLs, and keys.

### Task 3: Collection Service and API

**Files:**
- Create: `backend/BE-02/src/main/java/com/growgate/publicdata/service/PublicDataCollectionService.java`
- Create: `backend/BE-02/src/main/java/com/growgate/publicdata/controller/PublicDataController.java`
- Create: `backend/BE-02/src/main/java/com/growgate/publicdata/dto/*.java`

- [ ] Implement cache lookup by provider/requestKey/expiresAt.
- [ ] Implement `collectForDiagnosis(diagnosisId, userId, hsCode, country, productName)`.
- [ ] Persist runs and records for success, cache, skipped, and failed states.
- [ ] Add `POST /api/public-data/collect`, `GET /api/public-data/runs`, `GET /api/public-data/records`.

### Task 4: Diagnosis Integration

**Files:**
- Modify: `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java`
- Modify: `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisService.java`
- Modify: `backend/BE-02/src/main/java/com/growgate/diagnosis/dto/DiagnosisSourcesResponse.java`

- [ ] Call public data collection before local scoring.
- [ ] Convert records to source trace entries.
- [ ] Add live record contribution to market/regulation/recall/fto/support scores.
- [ ] Expose run id and fallback information in source responses.

### Task 5: Tests, Docs, Status

**Files:**
- Create: `backend/BE-02/src/test/java/com/growgate/publicdata/service/PublicDataCollectionServiceTest.java`
- Create: connector tests as needed.
- Modify: `README.md`
- Modify: `backend/BE-02/README.md`
- Modify: `docs/evaluation-resolution-status.md`
- Modify: `docs/submission-update-report.md`

- [ ] Add service tests for key-missing fallback and cache reuse.
- [ ] Update docs with env vars and honest status.
- [ ] Run Spring tests, frontend build, and diff check.
