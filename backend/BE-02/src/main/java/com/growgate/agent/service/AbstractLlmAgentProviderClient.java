package com.growgate.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

abstract class AbstractLlmAgentProviderClient implements AgentProviderClient {

    protected final ObjectMapper objectMapper;

    protected AbstractLlmAgentProviderClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    protected AgentInferenceResult parseTextResult(String provider, String model, String text, long latencyMs) {
        Map<String, Object> content = Map.of("summary", text == null ? "" : text);
        List<Map<String, Object>> suggestions = List.of();
        if (text != null && !text.isBlank()) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(stripMarkdownFence(text),
                        new TypeReference<Map<String, Object>>() {});
                content = parsed;
                Object rawSuggestions = parsed.get("knowledgeSuggestions");
                if (rawSuggestions instanceof List<?>) {
                    suggestions = ((List<?>) rawSuggestions).stream()
                            .filter(Map.class::isInstance)
                            .map(item -> (Map<String, Object>) item)
                            .limit(5)
                            .toList();
                }
            } catch (Exception ignored) {
                content = Map.of("summary", text);
            }
        }
        return AgentInferenceResult.success(provider, model, content, suggestions, latencyMs);
    }

    protected String buildPrompt(AgentInferenceRequest request) {
        try {
            return """
                    You are GrowGate Local EX's bounded export-readiness explanation agent.
                    You do not own finalGate, diagnosisScore, legal judgment, certification outcome, customs outcome, patent clearance, or support-program selection.
                    The final Gate is deterministic rule scoring plus risk and fallback conditions; keep llmInfluencedGate=false.

                    Return compact JSON only with keys:
                    summary, reasoning, recommendedActions, knowledgeSuggestions.

                    knowledgeSuggestions must be an array of up to 5 reviewable knowledge-candidate objects:
                    {
                      "type": "REGULATION_CHECKLIST|RECALL_CHECKLIST|FTO_SEARCH_MEMO|SUPPORT_PROGRAM_CANDIDATE|EXECUTION_TASK_TEMPLATE|DATA_STATUS_NOTE|MARKET_SIGNAL_NOTE|BUYER_REPLY_DRAFT|REVIEW_REQUIRED_NOTE",
                      "title": "short reviewable candidate title",
                      "evidence": "source-bound evidence summary with sourceId/provider/mode when available",
                      "rationale": "why this matters to a human export manager",
                      "impact": "how approved knowledge can be reused in a later diagnosis/report/exec board"
                    }

                    Forbidden claims:
                    - Do not say all data is live.
                    - Do not say customs, CE, safety, patent, support selection, export execution, or legal/certification outcomes are confirmed.
                    - Do not say no recall means safe.
                    - Do not say AI makes the autonomous final decision.
                    Use cautious wording: review needed, source-bound evidence, candidate, draft, checklist, 담당자 검토 필요.

                    Diagnosis:
                    - diagnosisId: %s
                    - productName: %s
                    - hsCode: %s
                    - targetCountry: %s

                    Evidence context:
                    %s

                    Scoring context:
                    %s
                    """.formatted(
                    safe(request.getDiagnosisId()),
                    safe(request.getProductName()),
                    safe(request.getHsCode()),
                    safe(request.getTargetCountry()),
                    objectMapper.writeValueAsString(request.getEvidenceContext() == null ? Map.of() : request.getEvidenceContext()),
                    objectMapper.writeValueAsString(request.getScoringContext() == null ? Map.of() : request.getScoringContext())
            );
        } catch (Exception e) {
            return "Return compact JSON reasoning for GrowGate diagnosis " + safe(request.getDiagnosisId());
        }
    }

    private String stripMarkdownFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
