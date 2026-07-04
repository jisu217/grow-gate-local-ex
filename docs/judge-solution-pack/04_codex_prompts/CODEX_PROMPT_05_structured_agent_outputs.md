# Codex Prompt 05 — Agent별 Structured Output 계약

목표:
Agent 결과를 자연어가 아니라 DTO/JSON Schema로 검증한다.

작업:
1. Java records 추가: PlannerAgentOutput, PublicDataAgentOutput, DocumentClassificationOutput, SentimentOutput, SearchOutput, RegulationAgentOutput, RecallAgentOutput, FtoAgentOutput, SupportAgentOutput, RoadmapAgentOutput, ReportAgentOutput, VerificationOutput.
2. enum: RiskLevel(low/medium/high/blocker), AgentStatus(success/retry/fallback/failed/planned).
3. Validator: required field 누락, sourceId 없는 high-risk claim, fallback positive contribution을 reject한다.
4. Python worker `schemas.py`에도 같은 Pydantic 모델을 추가한다.
5. `worker_engine.py`는 `toolUseTrace`를 agentName/toolName 구조로 반환한다.

수용 조건:
- validation 실패 시 retry 1회 후 controlled fallback으로 전환한다.
- ReportAgentOutput.claims는 evidenceSourceIds 또는 limitation 중 하나를 반드시 가진다.
- 테스트에서 sourceId 없는 고위험 claim은 block된다.
