package com.growgate.agent.service;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentKnowledgeCaptureService {

    private final AgentInstructionRegistry registry;
    private final AgentExplanationValidator validator;

    public AgentKnowledgeCaptureService(AgentInstructionRegistry registry, AgentExplanationValidator validator) {
        this.registry = registry;
        this.validator = validator;
    }

    public CaptureResult capture(String diagnosisId,
                                 String productId,
                                 String companyId,
                                 String productName,
                                 String hsCode,
                                 String targetCountry,
                                 Map<String, Integer> scoreMap,
                                 List<Map<String, Object>> sources,
                                 List<Map<String, Object>> risks,
                                 Map<String, Object> actionRoadmap,
                                 AgentInferenceResult llmResult) {
        List<Map<String, Object>> explanations = new ArrayList<>();
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (AgentInstructionRegistry.AgentInstruction instruction : registry.all()) {
            Map<String, Object> explanation = buildExplanation(instruction, productName, hsCode, targetCountry,
                    scoreMap == null ? Map.of() : scoreMap,
                    sources == null ? List.of() : sources,
                    risks == null ? List.of() : risks,
                    actionRoadmap == null ? Map.of() : actionRoadmap,
                    llmResult);
            explanations.add(validator.sanitizeMap(explanation));
            candidates.add(validator.validateCandidate(buildCandidate(diagnosisId, productId, companyId, instruction, explanation, llmResult)));
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("candidateCount", candidates.size());
        summary.put("pendingReviewCount", candidates.stream().filter(item -> "PENDING_REVIEW".equals(item.get("status"))).count());
        summary.put("dataInsufficientCount", candidates.stream().filter(item -> "DATA_INSUFFICIENT".equals(item.get("status"))).count());
        summary.put("approvedKnowledgeOnlyReusable", true);
        summary.put("llmInfluencedGate", false);
        return new CaptureResult(validator.sanitizeMap(summary), explanations, candidates);
    }

    private Map<String, Object> buildExplanation(AgentInstructionRegistry.AgentInstruction instruction,
                                                 String productName,
                                                 String hsCode,
                                                 String targetCountry,
                                                 Map<String, Integer> scoreMap,
                                                 List<Map<String, Object>> sources,
                                                 List<Map<String, Object>> risks,
                                                 Map<String, Object> actionRoadmap,
                                                 AgentInferenceResult llmResult) {
        String agent = instruction.agent();
        String usedFor = usedFor(agent);
        List<String> sourceIds = sourceIds(sources, usedFor);
        Map<String, Object> dataStatus = dataStatus(sources, usedFor);
        String scoreKey = scoreKey(agent);
        Integer score = scoreMap.get(scoreKey);
        String conclusion = conclusion(agent, score, productName, targetCountry);
        String reason = reason(agent, score, risks, actionRoadmap, sourceIds.size());
        List<String> nextActions = nextActions(agent, risks, actionRoadmap);
        List<String> limitations = limitations(agent, dataStatus);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("agent", agent);
        map.put("knowledgeType", instruction.knowledgeType());
        map.put("conclusion", conclusion);
        map.put("reason", reason);
        map.put("evidence", evidenceSummary(hsCode, targetCountry, dataStatus, sourceIds));
        map.put("sourceIds", sourceIds);
        map.put("limitations", limitations);
        map.put("nextActions", nextActions);
        map.put("dataStatus", dataStatus);
        map.put("knowledgeStatus", sourceIds.isEmpty() ? "DATA_INSUFFICIENT" : "PENDING_REVIEW");
        map.put("reviewRequired", true);
        map.put("llm", llmMeta(llmResult));
        return map;
    }

    private Map<String, Object> buildCandidate(String diagnosisId,
                                               String productId,
                                               String companyId,
                                               AgentInstructionRegistry.AgentInstruction instruction,
                                               Map<String, Object> explanation,
                                               AgentInferenceResult llmResult) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        String agent = instruction.agent();
        candidate.put("id", "agent_knowledge_candidate_" + safe(diagnosisId) + "_" + agent.toLowerCase());
        candidate.put("diagnosisId", safe(diagnosisId));
        candidate.put("productId", safe(productId));
        candidate.put("companyId", safe(companyId));
        candidate.put("agent", agent);
        candidate.put("knowledgeType", instruction.knowledgeType());
        candidate.put("title", title(agent, instruction.knowledgeType()));
        candidate.put("plainSummary", explanation.get("conclusion"));
        candidate.put("whyItMatters", explanation.get("reason"));
        candidate.put("evidenceSummary", explanation.get("evidence"));
        candidate.put("sourceIds", explanation.get("sourceIds"));
        candidate.put("dataStatus", explanation.get("dataStatus"));
        candidate.put("limitations", explanation.get("limitations"));
        candidate.put("nextActions", explanation.get("nextActions"));
        candidate.put("status", explanation.get("knowledgeStatus"));
        candidate.put("reviewerNote", "");
        candidate.put("createdBy", "agent");
        candidate.put("reviewedBy", null);
        candidate.put("createdAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        candidate.put("reviewedAt", null);
        candidate.put("llm", llmMeta(llmResult));
        return candidate;
    }

    private String conclusion(String agent, Integer score, String productName, String targetCountry) {
        String subject = safe(productName).isBlank() ? "대상 제품" : productName;
        String country = safe(targetCountry).isBlank() ? "대상국" : targetCountry;
        return switch (agent) {
            case "DATA_COLLECTION" -> "현재 확인 가능한 데이터 상태를 기준으로 " + subject + "의 근거 연결 상태를 정리했습니다.";
            case "NORMALIZATION" -> subject + " 입력값은 다음 진단에서 재사용할 수 있도록 보정 후보로 남깁니다.";
            case "MARKET" -> country + " 시장 신호는 score " + value(score) + "/100 기준의 검토 후보입니다.";
            case "REGULATION_CERTIFICATION" -> "인증 적용 가능성과 필요 문서 확인이 필요합니다.";
            case "RECALL_SAFETY" -> "유사 리콜 신호가 제한적이어도 제품안전 점검은 별도로 필요합니다.";
            case "FTO_PATENT" -> "공개 특허 기준 위험 후보 탐색 결과이며 최종 FTO 검토가 필요합니다.";
            case "SUPPORT" -> "조건이 유사한 지원사업 후보이며 최종 선정은 기관 심사에 따릅니다.";
            case "EXEC_BOARD" -> "AI가 업무 초안을 정리했으며 담당자가 우선순위와 완료 여부를 확인해야 합니다.";
            case "REPORT" -> "담당자 검토를 돕기 위한 수출 준비도 검토 보고서 초안입니다.";
            case "VERIFICATION" -> "출처, 한계, fallback 상태를 기준으로 담당자 검토 큐를 생성했습니다.";
            default -> "담당자 검토를 위한 판단 근거 후보입니다.";
        };
    }

    private String reason(String agent, Integer score, List<Map<String, Object>> risks,
                          Map<String, Object> actionRoadmap, int sourceCount) {
        String topRisk = risks.isEmpty() ? "출처 최신성 점검" : stringValue(risks.get(0).get("label"));
        String action = firstAction(actionRoadmap);
        return switch (agent) {
            case "DATA_COLLECTION" -> "연결된 sourceId " + sourceCount + "건과 데이터 상태를 기준으로 검토 범위를 제한했습니다.";
            case "NORMALIZATION" -> "HS Code, 대상국, 제품 설명이 다음 진단의 입력 품질에 영향을 줍니다.";
            case "EXEC_BOARD" -> "우선 업무는 `" + action + "` 항목부터 담당자 확인이 필요합니다.";
            case "REPORT" -> "리포트 문장은 근거와 한계를 보존한 담당자 검토용 초안이어야 합니다.";
            case "VERIFICATION" -> "`" + topRisk + "` 항목은 출처와 한계 확인 전 승인 지식으로 쓰면 안 됩니다.";
            default -> "`" + topRisk + "` 항목과 score " + value(score) + "/100 결과를 함께 고려했습니다.";
        };
    }

    private List<String> evidenceSummary(String hsCode, String targetCountry, Map<String, Object> dataStatus, List<String> sourceIds) {
        List<String> evidence = new ArrayList<>();
        if (!safe(hsCode).isBlank()) evidence.add("HS Code: " + hsCode);
        if (!safe(targetCountry).isBlank()) evidence.add("대상국: " + targetCountry);
        evidence.add("데이터 상태: " + dataStatus.get("mode"));
        evidence.add("sourceIds: " + sourceIds.size() + "건");
        return evidence;
    }

    private List<String> nextActions(String agent, List<Map<String, Object>> risks, Map<String, Object> actionRoadmap) {
        return switch (agent) {
            case "REGULATION_CERTIFICATION" -> List.of("CE 적용 가능성 확인", "필요 시험성적서와 라벨 문서 확인", "인증기관 또는 담당자 검토");
            case "RECALL_SAFETY" -> List.of("유사 리콜 항목 확인", "제품안전 체크리스트 점검", "라벨 경고문 확인");
            case "FTO_PATENT" -> List.of("KIPRIS 키워드 1차 검색", "경쟁사명 기반 공개 특허 후보 확인", "변리사 FTO 검토");
            case "SUPPORT" -> List.of("공고 원문 확인", "신청 자격 확인", "마감일과 제출 서류 확인");
            case "EXEC_BOARD" -> List.of(firstAction(actionRoadmap));
            default -> List.of(risks.isEmpty() ? "담당자 근거 검토" : stringValue(risks.get(0).getOrDefault("nextAction", "담당자 근거 검토")));
        };
    }

    private List<String> limitations(String agent, Map<String, Object> dataStatus) {
        List<String> limitations = new ArrayList<>();
        limitations.add("담당자 승인 전에는 검토 대기 지식 후보입니다.");
        limitations.add("최종 Gate는 rule score와 risk/fallback 조건으로 결정되며 LLM이 변경하지 않습니다.");
        if (!"live".equals(dataStatus.get("mode"))) {
            limitations.add("일부 근거는 live가 아닌 cache/fallback/seed 상태일 수 있습니다.");
        }
        if ("FTO_PATENT".equals(agent)) {
            limitations.add("법률 판단이 아니며 최종 FTO 검토가 필요합니다.");
        }
        return limitations;
    }

    private Map<String, Object> dataStatus(List<Map<String, Object>> sources, String usedFor) {
        int live = 0;
        int cache = 0;
        int fallback = 0;
        int seed = 0;
        String provider = "";
        String lastChecked = "";
        for (Map<String, Object> source : sources) {
            if (!matchesUsedFor(source, usedFor)) continue;
            provider = provider.isBlank() ? stringValue(source.get("provider")) : provider;
            lastChecked = lastChecked.isBlank() ? firstNonBlank(source.get("fetchedAt"), source.get("retrievedAt")) : lastChecked;
            String type = stringValue(source.get("sourceType")).toLowerCase();
            if ("live".equals(type)) live++;
            else if ("cache".equals(type)) cache++;
            else if ("seed".equals(type)) seed++;
            else if ("fallback".equals(type) || Boolean.TRUE.equals(source.get("fallbackUsed"))) fallback++;
        }
        String mode = live > 0 ? "live" : cache > 0 ? "cache" : fallback > 0 ? "fallback" : seed > 0 ? "seed" : "empty";
        Map<String, Object> dataStatus = new LinkedHashMap<>();
        dataStatus.put("provider", provider);
        dataStatus.put("mode", mode);
        dataStatus.put("lastChecked", lastChecked);
        dataStatus.put("liveCount", live);
        dataStatus.put("cacheCount", cache);
        dataStatus.put("fallbackCount", fallback);
        dataStatus.put("seedCount", seed);
        return dataStatus;
    }

    private List<String> sourceIds(List<Map<String, Object>> sources, String usedFor) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> source : sources) {
            if (!matchesUsedFor(source, usedFor)) continue;
            String id = stringValue(source.get("sourceId"));
            if (!id.isBlank()) ids.add(id);
        }
        return ids;
    }

    private boolean matchesUsedFor(Map<String, Object> source, String usedFor) {
        if (usedFor == null || usedFor.isBlank()) return true;
        String field = stringValue(source.get("field"));
        String rawUsedFor = stringValue(source.get("usedFor"));
        return usedFor.equalsIgnoreCase(field) || usedFor.equalsIgnoreCase(rawUsedFor);
    }

    private Map<String, Object> llmMeta(AgentInferenceResult llmResult) {
        Map<String, Object> llm = new LinkedHashMap<>();
        llm.put("used", llmResult != null && llmResult.isSucceeded());
        llm.put("provider", llmResult == null ? "spring-local" : safe(llmResult.getProvider()));
        llm.put("model", llmResult == null ? "rule-fallback" : safe(llmResult.getModel()));
        llm.put("fallbackUsed", llmResult == null || !llmResult.isSucceeded());
        return llm;
    }

    private String title(String agent, String knowledgeType) {
        return switch (agent) {
            case "REGULATION_CERTIFICATION" -> "규제·인증 문서 확인 필요";
            case "RECALL_SAFETY" -> "리콜·제품안전 출하 전 점검 필요";
            case "FTO_PATENT" -> "공개 특허 기준 FTO 검토 메모";
            case "SUPPORT" -> "지원사업 후보 검토 필요";
            case "EXEC_BOARD" -> "실행보드 업무 템플릿 후보";
            default -> knowledgeType + " 지식 후보";
        };
    }

    private String usedFor(String agent) {
        return switch (agent) {
            case "MARKET" -> "market";
            case "REGULATION_CERTIFICATION" -> "regulation";
            case "RECALL_SAFETY" -> "recall";
            case "FTO_PATENT" -> "fto";
            case "SUPPORT" -> "support";
            default -> "";
        };
    }

    private String scoreKey(String agent) {
        return switch (agent) {
            case "MARKET" -> "market";
            case "REGULATION_CERTIFICATION" -> "regulation";
            case "RECALL_SAFETY" -> "recall";
            case "FTO_PATENT" -> "fto";
            case "SUPPORT" -> "support";
            default -> "overall";
        };
    }

    @SuppressWarnings("unchecked")
    private String firstAction(Map<String, Object> actionRoadmap) {
        Object thisWeek = actionRoadmap.get("thisWeek");
        if (thisWeek instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> map) {
            return stringValue(((Map<String, Object>) map).getOrDefault("title", "담당자 근거 검토"));
        }
        return "담당자 근거 검토";
    }

    private String firstNonBlank(Object left, Object right) {
        String leftValue = stringValue(left);
        return leftValue.isBlank() ? stringValue(right) : leftValue;
    }

    private String value(Integer score) {
        return score == null ? "0" : String.valueOf(score);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record CaptureResult(Map<String, Object> summary,
                                List<Map<String, Object>> explanations,
                                List<Map<String, Object>> candidates) {
    }
}
