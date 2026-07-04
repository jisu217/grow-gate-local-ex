# Codex Prompt 04 — Agent Trace DB/API/UI 구현

목표:
첨부 보강 기획서의 P0 요구처럼 `agentTrace`를 JSON blob 내부가 아니라 `agent_trace_events` 테이블과 UI Timeline으로 노출한다.

작업:
1. `agent_trace_events` migration을 추가한다.
2. Entity/Repository: `com.growgate.agent.entity.AgentTraceEvent`, `AgentTraceEventRepository`.
3. Service: `AgentTraceWriter`를 구현한다. 필드: agentRunId, diagnosisId, stepNo, agentName, toolName, inputHash, outputSchema, outputSummary, sourceType, provider, endpoint, latencyMs, status, retryCount, fallbackReason, evidenceCount.
4. `DiagnosisAsyncProcessor.AgentLoop.runTool` 호출부를 agentName/toolName/outputSchema/evidenceCount를 받을 수 있게 확장한다.
5. `DiagnosisController`에 `GET /diagnosis/{id}/agent-trace`를 추가한다.
6. FE: `S10Sources.jsx`에 Agent Trace 탭 또는 `S11AgentTrace.jsx`를 추가한다.

수용 조건:
- seed 진단 3개에도 최소 4개 trace row가 보인다.
- live 진단 실행 시 PlannerAgent→PublicDataAgent→RegulationAgent→FTOAgent→ReportAgent 순서가 DB에 저장된다.
- fallbackReason은 secret/API key를 포함하지 않는다.
