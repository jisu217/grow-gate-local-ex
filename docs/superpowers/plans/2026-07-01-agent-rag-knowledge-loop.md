# Agent RAG Knowledge Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Gemini/OpenRouter fallback inference, DB-backed RAG context injection, and mypage knowledge-loop approval.

**Architecture:** Spring owns the diagnosis orchestration and calls a provider-agnostic `AgentInferenceService`. The service builds bounded RAG context from existing KB/public-data records, calls Gemini first and OpenRouter second, and stores only summarized trace and knowledge-loop suggestions.

**Tech Stack:** Spring Boot 3.4, RestTemplate, JPA, MySQL schema SQL, React/Vite.

---

### Task 1: Backend Agent Provider

**Files:**
- Create: `backend/BE-02/src/main/java/com/growgate/agent/service/AgentInferenceService.java`
- Create: `backend/BE-02/src/main/java/com/growgate/agent/service/AgentProviderClient.java`
- Create: `backend/BE-02/src/main/java/com/growgate/agent/service/GeminiAgentProviderClient.java`
- Create: `backend/BE-02/src/main/java/com/growgate/agent/service/OpenRouterAgentProviderClient.java`
- Create: `backend/BE-02/src/main/java/com/growgate/agent/service/AgentInferenceRequest.java`
- Create: `backend/BE-02/src/main/java/com/growgate/agent/service/AgentInferenceResult.java`
- Test: `backend/BE-02/src/test/java/com/growgate/agent/service/AgentInferenceServiceTest.java`

- [ ] Write failing tests for Gemini-first, OpenRouter fallback, and all-provider fallback.
- [ ] Implement minimal provider result abstraction.
- [ ] Add environment flags and timeout-safe RestTemplate calls.
- [ ] Run backend tests.

### Task 2: RAG Context Builder

**Files:**
- Create: `backend/BE-02/src/main/java/com/growgate/agent/service/AgentRagContextBuilder.java`
- Test: `backend/BE-02/src/test/java/com/growgate/agent/service/AgentRagContextBuilderTest.java`

- [ ] Write failing test proving product KB, sources, public-data records, and accepted loop items appear in context.
- [ ] Implement bounded context maps without raw secret/prompt persistence.
- [ ] Run focused tests.

### Task 3: Knowledge Loop Persistence

**Files:**
- Create: `backend/BE-02/src/main/java/com/growgate/knowledge/entity/KnowledgeLoopItem.java`
- Create: `backend/BE-02/src/main/java/com/growgate/knowledge/enums/KnowledgeLoopStatus.java`
- Create: `backend/BE-02/src/main/java/com/growgate/knowledge/repository/KnowledgeLoopItemRepository.java`
- Create: `backend/BE-02/src/main/java/com/growgate/knowledge/dto/KnowledgeLoopItemsResponse.java`
- Create: `backend/BE-02/src/main/java/com/growgate/knowledge/dto/KnowledgeLoopActionResponse.java`
- Modify: `database/mysql/init/01_schema.sql`
- Modify: `backend/BE-02/src/main/java/com/growgate/knowledge/service/KnowledgeService.java`
- Modify: `backend/BE-02/src/main/java/com/growgate/knowledge/controller/KnowledgeController.java`

- [ ] Write failing service test for accepting a pending loop item.
- [ ] Add table/entity/repository.
- [ ] Add list, accept, dismiss APIs.
- [ ] On accept, create a `KnowledgeSource` row and audit log.

### Task 4: Diagnosis Integration

**Files:**
- Modify: `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java`
- Modify: `backend/BE-02/src/main/java/com/growgate/diagnosis/dto/DiagnosisDetailResponse.java`

- [ ] Write failing test that diagnosis result contains agent provider trace and knowledge-loop suggestions.
- [ ] Call `AgentInferenceService` after Spring scoring.
- [ ] Merge bounded agent output into `agentTrace`, `stageCards`, `reportMarkdown`.
- [ ] Persist pending `KnowledgeLoopItem` candidates.

### Task 5: Mypage UI

**Files:**
- Modify: `frontend/FE-02/src/services/productService.js`
- Modify: `frontend/FE-02/src/pages/mypage/KnowledgeDetail.jsx`
- Modify: `frontend/FE-02/src/pages/mypage/KnowledgeDetail.module.css`

- [ ] Add API service functions for loop items.
- [ ] Add `AI 지식화` tab.
- [ ] Show recommended items and accept/dismiss actions.
- [ ] Run frontend build.
