package com.growgate.diagnosis.service;

// 비동기 진단 실행 처리 (@Async)
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.agent.service.AgentInferenceRequest;
import com.growgate.agent.service.AgentInferenceResult;
import com.growgate.agent.service.AgentInferenceService;
import com.growgate.agent.service.AgentRagContextBuilder;
import com.growgate.diagnosis.entity.DiagnosisRun;
import com.growgate.diagnosis.enums.DiagnosisStatus;
import com.growgate.diagnosis.repository.DiagnosisRepository;
import com.growgate.knowledge.entity.BuyerRequirement;
import com.growgate.knowledge.entity.Certification;
import com.growgate.knowledge.entity.Fto;
import com.growgate.knowledge.entity.KnowledgeLoopItem;
import com.growgate.knowledge.entity.KnowledgeSource;
import com.growgate.knowledge.entity.ProductKnowledge;
import com.growgate.knowledge.enums.KnowledgeLoopStatus;
import com.growgate.knowledge.enums.RiskLevel;
import com.growgate.knowledge.enums.TrustGrade;
import com.growgate.knowledge.repository.BuyerRequirementRepository;
import com.growgate.knowledge.repository.CertificationRepository;
import com.growgate.knowledge.repository.FtoRepository;
import com.growgate.knowledge.repository.KnowledgeLoopItemRepository;
import com.growgate.knowledge.repository.KnowledgeSourceRepository;
import com.growgate.knowledge.repository.ProductKnowledgeRepository;
import com.growgate.product.entity.Product;
import com.growgate.publicdata.entity.PublicDataRecord;
import com.growgate.publicdata.service.PublicDataCollectionResult;
import com.growgate.publicdata.service.PublicDataCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisAsyncProcessor {

    private final DiagnosisRepository diagnosisRepository;
    private final ObjectMapper objectMapper;
    private final ProductKnowledgeRepository knowledgeRepository;
    private final CertificationRepository certificationRepository;
    private final FtoRepository ftoRepository;
    private final BuyerRequirementRepository buyerRequirementRepository;
    private final KnowledgeSourceRepository knowledgeSourceRepository;
    private final FastApiAgentClient fastApiAgentClient;
    private final PublicDataCollectionService publicDataCollectionService;
    private final AgentInferenceService agentInferenceService;
    private final AgentRagContextBuilder agentRagContextBuilder;
    private final KnowledgeLoopItemRepository knowledgeLoopItemRepository;

    @Async
    @Transactional
    public void process(String diagnosisId) {
        DiagnosisRun run = diagnosisRepository.findById(diagnosisId).orElse(null);
        if (run == null) {
            log.warn("Diagnosis run not found for async processing: {}", diagnosisId);
            return;
        }

        try {
            updateStatus(run, DiagnosisStatus.running);

            Product product = run.getProduct();
            Map<String, Object> inputData = parseInputData(run.getInputData());
            String productName = product != null ? product.getName() : stringValue(inputData.get("productName"));
            String hsCode = normalizeHsCode(product != null ? product.getHsCode() : stringValue(inputData.get("hsCode")));
            String description = product != null ? product.getDescription() : stringValue(inputData.get("description"));
            String targetCountry = normalizeCountry(run.getTargetCountry());

            ProductKnowledge knowledge = product != null
                    ? knowledgeRepository.findByProductId(product.getId()).orElse(null)
                    : null;
            List<Certification> certs = product != null ? certificationRepository.findByProductId(product.getId()) : List.of();
            List<Fto> ftoItems = product != null ? ftoRepository.findByProductId(product.getId()) : List.of();
            List<BuyerRequirement> buyers = product != null ? buyerRequirementRepository.findByProductId(product.getId()) : List.of();
            List<KnowledgeSource> knowledgeSources = knowledge != null
                    ? knowledgeSourceRepository.findByKnowledgeId(knowledge.getId())
                    : List.of();
            List<KnowledgeLoopItem> acceptedLoopItems = product != null
                    ? knowledgeLoopItemRepository.findTop20ByProductIdAndStatusOrderByCreatedAtDesc(
                    product.getId(), KnowledgeLoopStatus.accepted)
                    : List.of();

            AgentLoop agentLoop = AgentLoop.start(run.getId(), productName, hsCode, targetCountry);
            List<Map<String, Object>> sources = new ArrayList<>();
            PublicDataCollectionResult publicDataResult = agentLoop.runTool(
                    "collect_public_data",
                    "PublicDataCollectionService.collectForDiagnosis",
                    2,
                    null,
                    () -> publicDataCollectionService.collectForDiagnosis(
                            run.getId(), run.getUser().getId(), hsCode, targetCountry, productName),
                    result -> result != null && result.getRun() != null
                            ? AgentVerification.pass("public data run=" + result.getRun().getId()
                            + ", records=" + result.getRecords().size())
                            : AgentVerification.fail("public data collection returned no run")
            );
            addPublicDataSources(sources, publicDataResult.getRecords());

            Map<String, Integer> scoreMap = agentLoop.runTool(
                    "score_readiness",
                    "Spring rule scorer + public data contribution",
                    1,
                    Map.of(),
                    () -> {
                        int marketScore = scoreMarket(hsCode, targetCountry, sources);
                        int regulationScore = scoreRegulation(hsCode, targetCountry, certs, sources);
                        int recallScore = scoreRecall(hsCode, description, sources);
                        int ftoScore = scoreFto(ftoItems, targetCountry, sources);
                        int supportScore = scoreSupport(knowledge, buyers, knowledgeSources, targetCountry, sources);
                        marketScore = applyPublicDataContribution(marketScore, publicDataResult.getRecords(), "market");
                        regulationScore = applyPublicDataContribution(regulationScore, publicDataResult.getRecords(), "regulation");
                        recallScore = applyPublicDataContribution(recallScore, publicDataResult.getRecords(), "recall");
                        ftoScore = applyPublicDataContribution(ftoScore, publicDataResult.getRecords(), "fto");
                        supportScore = applyPublicDataContribution(supportScore, publicDataResult.getRecords(), "support");
                        return Map.of(
                                "market", marketScore,
                                "regulation", regulationScore,
                                "recall", recallScore,
                                "fto", ftoScore,
                                "support", supportScore
                        );
                    },
                    result -> result != null && result.values().stream().allMatch(score -> score >= 0 && score <= 100)
                            ? AgentVerification.pass("all readiness scores are within 0..100")
                            : AgentVerification.fail("readiness scores failed range verification")
            );
            int market = scoreMap.get("market");
            int regulation = scoreMap.get("regulation");
            int recall = scoreMap.get("recall");
            int fto = scoreMap.get("fto");
            int support = scoreMap.get("support");
            int overall = clamp(Math.round((market * 0.25f) + (regulation * 0.22f) + (recall * 0.16f)
                    + (fto * 0.20f) + (support * 0.17f)));

            Map<String, Object> planningResult = agentLoop.runTool(
                    "plan_risk_roadmap",
                    "Risk planner + 30/60/90 roadmap generator",
                    1,
                    Map.of("riskItems", List.of(), "actionRoadmap", Map.of()),
                    () -> {
                        List<Map<String, String>> risks = buildRiskItems(certs, ftoItems, knowledge, buyers, hsCode);
                        Map<String, Object> roadmap = buildRoadmap(risks, certs, ftoItems, buyers, knowledgeSources);
                        return Map.of("riskItems", risks, "actionRoadmap", roadmap);
                    },
                    result -> result != null && result.get("riskItems") instanceof List<?> risks && !risks.isEmpty()
                            ? AgentVerification.pass("risk items and roadmap generated")
                            : AgentVerification.fail("risk planner did not produce risk items")
            );
            @SuppressWarnings("unchecked")
            List<Map<String, String>> riskItems = (List<Map<String, String>>) planningResult.get("riskItems");
            @SuppressWarnings("unchecked")
            Map<String, Object> actionRoadmap = (Map<String, Object>) planningResult.get("actionRoadmap");
            Map<String, Object> agentRequest = buildAgentRequest(run, productName, hsCode, description, targetCountry, certs);
            Optional<Map<String, Object>> agentResult = agentLoop.runTool(
                    "fastapi_analyze",
                    "FastAPI /analyze",
                    2,
                    Optional.empty(),
                    () -> fastApiAgentClient.analyze(agentRequest),
                    result -> result != null && result.isPresent()
                            ? AgentVerification.pass("FastAPI analyze returned payload")
                            : AgentVerification.fail("FastAPI analyze unavailable; retry or fallback to Spring")
            );
            Map<String, Object> ragContext = agentRagContextBuilder.build(knowledge, certs, ftoItems, buyers,
                    knowledgeSources, publicDataResult.getRecords(), acceptedLoopItems);
            AgentInferenceRequest inferenceRequest = AgentInferenceRequest.builder()
                    .diagnosisId(run.getId())
                    .productName(productName)
                    .hsCode(hsCode)
                    .targetCountry(targetCountry)
                    .ragContext(ragContext)
                    .scoringContext(Map.of(
                            "overall", overall,
                            "market", market,
                            "regulation", regulation,
                            "recall", recall,
                            "fto", fto,
                            "support", support,
                            "riskItems", riskItems,
                            "actionRoadmap", actionRoadmap
                    ))
                    .build();
            AgentInferenceResult llmResult = agentLoop.runTool(
                    "llm_reason",
                    "Gemini/OpenRouter provider chain",
                    2,
                    AgentInferenceResult.builder()
                            .succeeded(false)
                            .provider("spring-local")
                            .model("rule-fallback")
                            .content(Map.of())
                            .knowledgeSuggestions(List.of())
                            .fallbackReasons(List.of("agent loop fallback"))
                            .build(),
                    () -> agentInferenceService.infer(inferenceRequest),
                    result -> result != null && !nullToEmpty(result.getProvider()).isBlank()
                            ? AgentVerification.pass(result.isSucceeded()
                            ? "external LLM reasoning succeeded with " + result.getProvider()
                            : "LLM unavailable; verified Spring local fallback result")
                            : AgentVerification.fail("LLM provider chain returned no provider")
            );
            Optional<Map<String, Object>> reportResult = agentLoop.runTool(
                    "render_report",
                    "FastAPI /report",
                    2,
                    Optional.empty(),
                    () -> fastApiAgentClient.report(agentRequest),
                    result -> result != null && result.isPresent()
                            ? AgentVerification.pass("FastAPI report returned markdown")
                            : AgentVerification.fail("FastAPI report unavailable; retry or render Spring markdown")
            );
            List<Map<String, Object>> loopSuggestions = buildKnowledgeLoopSuggestions(
                    llmResult, riskItems, sources, productName, hsCode, targetCountry);
            persistKnowledgeLoopSuggestions(run, product, loopSuggestions);

            sources.add(source("agent", "FASTAPI-AGENT-" + targetCountry,
                    "FastAPI Agent", "Optional analyze/report bridge", "configured APP_FASTAPI_BASE_URL",
                    "diagnosisId=" + run.getId(), "agentic-analysis", agentResult.isPresent() ? 5 : 0,
                    agentResult.isEmpty(), agentResult.isPresent()
                            ? "Spring 진단 결과에 FastAPI agent 분석을 병합"
                            : "FastAPI agent 미활성 또는 호출 실패로 Spring 규칙 진단 유지"));
            agentLoop.runTool(
                    "verify_sources",
                    "Agent source verifier",
                    1,
                    Map.of("valid", false, "message", "not executed"),
                    () -> verifySourceTrace(sources, publicDataResult.getRecords()),
                    result -> Boolean.TRUE.equals(result.get("valid"))
                            ? AgentVerification.pass(stringValue(result.get("message")))
                            : AgentVerification.fail(stringValue(result.get("message")))
            );
            agentLoop.complete("진단 계획, 도구 실행, 검증, fallback 처리를 완료했습니다.");

            Map<String, Object> resultMap = new LinkedHashMap<>();
            resultMap.put("diagnosisScore", Map.of(
                    "overall", overall,
                    "market", market,
                    "regulation", regulation,
                    "recall", recall,
                    "fto", fto,
                    "support", support
            ));
            resultMap.put("summary", buildSummary(productName, hsCode, targetCountry, overall, riskItems));
            resultMap.put("evidenceSources", sources.stream()
                    .map(source -> stringValue(source.get("provider")))
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .collect(Collectors.toList()));
            resultMap.put("riskItems", riskItems);
            resultMap.put("actionRoadmap", actionRoadmap);
            resultMap.put("sources", sources);
            resultMap.put("agentTrace", agentLoop.toTrace(agentResult.isPresent(), reportResult.isPresent(), llmResult));
            resultMap.put("ragRetrieval", Map.of(
                    "mode", stringValue(ragContext.get("retrievalMode")),
                    "query", stringValue(ragContext.get("retrievalQuery")),
                    "topK", ragContext.getOrDefault("retrievalTopK", List.of())
            ));
            resultMap.put("scoringRubric", buildScoringRubric());
            resultMap.put("stageCards", buildStageCards(productName, hsCode, targetCountry, overall, market,
                    regulation, recall, fto, support, riskItems, actionRoadmap, sources, agentResult.orElse(null),
                    llmResult, loopSuggestions));
            resultMap.put("reportMarkdown", extractReportMarkdown(reportResult.orElse(null),
                    productName, hsCode, targetCountry, overall, riskItems, actionRoadmap));
            resultMap.put("reportFastApiUsed", reportResult.isPresent());
            resultMap.put("agentInference", Map.of(
                    "succeeded", llmResult.isSucceeded(),
                    "provider", nullToEmpty(llmResult.getProvider()),
                    "model", nullToEmpty(llmResult.getModel()),
                    "fallbackReasons", llmResult.getFallbackReasons() == null ? List.of() : llmResult.getFallbackReasons(),
                    "summary", llmResult.getContent() != null ? stringValue(llmResult.getContent().get("summary")) : ""
            ));
            resultMap.put("knowledgeLoopSuggestions", loopSuggestions);
            resultMap.put("publicDataCollectionRunId", publicDataResult.getRun().getId());
            resultMap.put("inputTrace", Map.of(
                    "productName", nullToEmpty(productName),
                    "hsCode", nullToEmpty(hsCode),
                    "targetCountry", targetCountry,
                    "knowledgeSourceCount", knowledgeSources.size(),
                    "certificationCount", certs.size(),
                    "buyerRequirementCount", buyers.size()
            ));

            run.setResult(objectMapper.writeValueAsString(resultMap));
            updateStatus(run, DiagnosisStatus.done);
        } catch (Exception e) {
            log.error("Diagnosis processing failed for id: {}", diagnosisId, e);
            run.setResult(buildFailureResult(e));
            updateStatus(run, DiagnosisStatus.failed);
        }
    }

    private void updateStatus(DiagnosisRun run, DiagnosisStatus status) {
        DiagnosisStatus previous = run.getStatus();
        run.setStatus(status);
        diagnosisRepository.save(run);
        log.info("Diagnosis status changed: diagnosisId={}, previous={}, current={}",
                run.getId(), previous, status);
    }

    private String buildFailureResult(Exception e) {
        try {
            Map<String, Object> failure = new LinkedHashMap<>();
            failure.put("summary", "진단 처리 중 오류가 발생했습니다.");
            failure.put("errorType", e.getClass().getSimpleName());
            failure.put("errorMessage", "진단 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            failure.put("failedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
            failure.put("evidenceSources", List.of());
            failure.put("riskItems", List.of());
            failure.put("agentTrace", Map.of(
                    "mode", "failed",
                    "steps", List.of(Map.of(
                            "step", "process",
                            "tool", "Spring DiagnosisAsyncProcessor",
                            "status", "failed",
                            "retryCount", 0,
                            "message", "진단 처리 실패"
                    ))
            ));
            return objectMapper.writeValueAsString(failure);
        } catch (Exception serializationFailure) {
            log.error("Failed to serialize diagnosis failure result", serializationFailure);
            return "{\"summary\":\"진단 처리 중 오류가 발생했습니다.\",\"errorType\":\"ProcessingFailure\","
                    + "\"errorMessage\":\"진단 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.\","
                    + "\"failedAt\":\"" + OffsetDateTime.now(ZoneOffset.UTC) + "\"}";
        }
    }

    private void addPublicDataSources(List<Map<String, Object>> sources, List<PublicDataRecord> records) {
        for (PublicDataRecord record : records) {
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("field", record.getUsedFor());
            source.put("sourceId", record.getProvider().name() + "-" + record.getId());
            source.put("provider", record.getProvider().name());
            source.put("datasetName", publicDataDatasetName(record));
            source.put("documentName", publicDataDatasetName(record));
            source.put("url", record.getEndpoint());
            source.put("retrievedAt", record.getFetchedAt() != null
                    ? record.getFetchedAt().atOffset(ZoneOffset.UTC).toString()
                    : OffsetDateTime.now(ZoneOffset.UTC).toString());
            source.put("recordId", record.getId());
            source.put("matchedKey", record.getMatchedKey());
            source.put("usedFor", record.getUsedFor());
            source.put("scoreContribution", record.getScoreContribution());
            source.put("fallbackUsed", record.isFallbackUsed());
            source.put("cacheHit", record.isCacheHit());
            source.put("sourceType", sourceType(record));
            source.put("collectionRunId", record.getRun() != null ? record.getRun().getId() : null);
            source.put("errorMessage", record.getErrorMessage());
            source.put("confidence", record.getConfidence());
            Map<String, Object> normalizedPayload = parseObjectMap(record.getNormalizedPayload());
            Object domainSignals = normalizedPayload.get("domainSignals");
            if (domainSignals instanceof Map<?, ?>) {
                source.put("domainSignals", domainSignals);
            }
            Object metrics = normalizedPayload.get("metrics");
            if (metrics instanceof Map<?, ?>) {
                source.put("metrics", metrics);
            }
            source.put("reason", record.isFallbackUsed()
                    ? "live 공공 API 수집 실패/비활성 상태를 명시하고 fallback 신호로 반영"
                    : "live 공공 API 또는 유효 cache record의 도메인 신호를 진단 점수에 반영");
            sources.add(source);
        }
    }

    private int applyPublicDataContribution(int baseScore, List<PublicDataRecord> records, String usedFor) {
        int contribution = records.stream()
                .filter(record -> usedFor.equals(record.getUsedFor()))
                .map(PublicDataRecord::getScoreContribution)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        return clamp(baseScore + contribution);
    }

    private String publicDataDatasetName(PublicDataRecord record) {
        return switch (record.getProvider()) {
            case KCS_TRADE -> "관세청 수출입무역통계 live/cache 수집";
            case KOTRA_MARKET -> "KOTRA 시장·뉴스 live/cache 수집";
            case SAFETY_RECALL -> "제품안전·리콜 live/cache 수집";
            case KIPRIS_FTO -> "KIPRIS Plus 특허/FTO live/cache 수집";
            case BIZINFO_SUPPORT -> "기업마당 지원사업 live/cache 수집";
        };
    }

    private Map<String, Object> buildAgentRequest(DiagnosisRun run, String productName, String hsCode,
                                                  String description, String targetCountry,
                                                  List<Certification> certs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("company_name", run.getCompany() != null ? run.getCompany().getName() : "개인 사업자");
        payload.put("region", "지역 미입력");
        payload.put("industry", run.getCompany() != null ? nullToEmpty(run.getCompany().getIndustry()) : "제조");
        payload.put("product_name", nullToEmpty(productName).isBlank() ? "입력 제품" : productName);
        payload.put("hs_code", nullToEmpty(hsCode).isBlank() ? "000000" : hsCode);
        payload.put("target_market", targetCountry);
        payload.put("features", inferFeatures(hsCode, description));
        payload.put("annual_export_usd", 0);
        payload.put("certifications", certs.stream()
                .map(Certification::getName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .collect(Collectors.toList()));
        payload.put("has_bom", true);
        payload.put("has_test_report", !certs.isEmpty());
        payload.put("has_label_draft", false);
        payload.put("notes", nullToEmpty(description));
        return payload;
    }

    private List<String> inferFeatures(String hsCode, String description) {
        List<String> features = new ArrayList<>();
        String lower = nullToEmpty(description).toLowerCase();
        if (startsWithAny(hsCode, "8507") || lower.contains("battery") || lower.contains("배터리")) {
            features.add("battery");
            features.add("electronics");
            features.add("industrial");
        } else if (startsWithAny(hsCode, "8541", "8537") || lower.contains("iot")) {
            features.add("electronics");
            features.add("iot");
            features.add("industrial");
        } else if (startsWithAny(hsCode, "3304", "3401")) {
            features.add("consumer");
        } else {
            features.add("industrial");
        }
        return features;
    }

    private Map<String, Object> verifySourceTrace(List<Map<String, Object>> sources,
                                                  List<PublicDataRecord> records) {
        long heuristicCount = sources.stream()
                .filter(source -> "heuristic".equals(source.get("sourceType")))
                .count();
        long fallbackCount = sources.stream()
                .filter(source -> "fallback".equals(source.get("sourceType")))
                .count();
        long liveOrCacheCount = sources.stream()
                .filter(source -> "live".equals(source.get("sourceType")) || "cache".equals(source.get("sourceType")))
                .count();
        boolean invalidRuleProvider = sources.stream()
                .filter(source -> "heuristic".equals(source.get("sourceType")))
                .anyMatch(source -> !"RULE_ENGINE".equals(source.get("provider")));
        boolean missingPublicSourceType = records.stream()
                .anyMatch(record -> record.getSourceType() == null || record.getSourceType().isBlank());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", !invalidRuleProvider && !missingPublicSourceType && !sources.isEmpty());
        result.put("heuristicCount", heuristicCount);
        result.put("fallbackCount", fallbackCount);
        result.put("liveOrCacheCount", liveOrCacheCount);
        result.put("message", invalidRuleProvider
                ? "heuristic source must use RULE_ENGINE provider"
                : missingPublicSourceType
                ? "public data record has no sourceType"
                : "source trace separates live/cache/fallback/heuristic/agent");
        return result;
    }

    private static class AgentLoop {
        private final String diagnosisId;
        private final List<Map<String, Object>> plan = new ArrayList<>();
        private final List<Map<String, Object>> steps = new ArrayList<>();
        private final OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);
        private OffsetDateTime completedAt;
        private String completionSummary;

        private AgentLoop(String diagnosisId) {
            this.diagnosisId = diagnosisId;
        }

        static AgentLoop start(String diagnosisId, String productName, String hsCode, String targetCountry) {
            AgentLoop loop = new AgentLoop(diagnosisId);
            loop.plan.add(task("collect_public_data", "Collect public data with live/cache/fallback evidence"));
            loop.plan.add(task("score_readiness", "Score market/regulation/recall/FTO/support with source contribution"));
            loop.plan.add(task("plan_risk_roadmap", "Create risk items and 30/60/90 roadmap"));
            loop.plan.add(task("fastapi_analyze", "Try optional FastAPI analyze tool, fallback to Spring if unavailable"));
            loop.plan.add(task("llm_reason", "Call Gemini/OpenRouter provider chain, fallback to Spring rules"));
            loop.plan.add(task("render_report", "Try optional FastAPI report tool, fallback to Spring markdown/PDF"));
            loop.plan.add(task("verify_sources", "Verify sourceType separation and trace integrity"));
            loop.steps.add(Map.of(
                    "step", "plan",
                    "tool", "AgentLoopPlanner",
                    "status", "done",
                    "retryCount", 0,
                    "message", "planned " + loop.plan.size() + " tasks for product="
                            + safe(productName) + ", hsCode=" + safe(hsCode) + ", targetCountry=" + safe(targetCountry)
            ));
            return loop;
        }

        <T> T runTool(String taskId, String toolName, int maxAttempts, T fallback,
                      AgentTool<T> action, AgentVerifier<T> verifier) {
            int attempts = Math.max(1, maxAttempts);
            RuntimeException lastFailure = null;
            for (int attempt = 1; attempt <= attempts; attempt++) {
                OffsetDateTime attemptStartedAt = OffsetDateTime.now(ZoneOffset.UTC);
                try {
                    T result = action.run();
                    AgentVerification verification = verifier.verify(result);
                    steps.add(step(taskId, toolName, verification.passed() ? "done" : "retry",
                            attempt - 1, verification.message(), attemptStartedAt));
                    if (verification.passed()) return result;
                    lastFailure = new IllegalStateException(verification.message());
                } catch (Exception e) {
                    lastFailure = e instanceof RuntimeException runtime ? runtime : new IllegalStateException(e);
                    steps.add(step(taskId, toolName, attempt < attempts ? "retry" : "failed",
                            attempt - 1, safe(lastFailure.getMessage()), attemptStartedAt));
                }
            }
            steps.add(step(taskId, toolName, "fallback", attempts,
                    "max attempts reached; using deterministic fallback", OffsetDateTime.now(ZoneOffset.UTC)));
            if (fallback != null) return fallback;
            throw lastFailure == null ? new IllegalStateException("agent tool failed: " + taskId) : lastFailure;
        }

        void complete(String summary) {
            this.completedAt = OffsetDateTime.now(ZoneOffset.UTC);
            this.completionSummary = summary;
            steps.add(Map.of(
                    "step", "complete",
                    "tool", "complete_task",
                    "status", "done",
                    "retryCount", 0,
                    "message", summary
            ));
        }

        Map<String, Object> toTrace(boolean analyzeSucceeded, boolean reportSucceeded,
                                    AgentInferenceResult llmResult) {
            Map<String, Object> trace = new LinkedHashMap<>();
            trace.put("mode", "agent-loop-planning-tool-verification-retry");
            trace.put("claimBoundary", "Spring AgentLoop가 계획을 만들고 tool을 실행하며 verifier 실패 시 retry/fallback을 기록합니다.");
            trace.put("diagnosisId", diagnosisId);
            trace.put("startedAt", startedAt.toString());
            trace.put("completedAt", completedAt != null ? completedAt.toString() : OffsetDateTime.now(ZoneOffset.UTC).toString());
            trace.put("plan", plan);
            trace.put("steps", steps);
            trace.put("completion", Map.of(
                    "status", "success",
                    "summary", completionSummary == null ? "" : completionSummary
            ));
            trace.put("fastApi", Map.of(
                    "analyzeSucceeded", analyzeSucceeded,
                    "reportSucceeded", reportSucceeded
            ));
            trace.put("llm", Map.of(
                    "succeeded", llmResult != null && llmResult.isSucceeded(),
                    "provider", llmResult == null ? "" : safe(llmResult.getProvider()),
                    "model", llmResult == null ? "" : safe(llmResult.getModel()),
                    "fallbackReasons", llmResult == null || llmResult.getFallbackReasons() == null
                            ? List.of() : llmResult.getFallbackReasons(),
                    "latencyMs", llmResult == null ? 0 : llmResult.getLatencyMs()
            ));
            return trace;
        }

        private static Map<String, Object> task(String id, String objective) {
            return Map.of("id", id, "objective", objective, "status", "planned");
        }

        private static Map<String, Object> step(String taskId, String toolName, String status,
                                                int retryCount, String message,
                                                OffsetDateTime attemptStartedAt) {
            return Map.of(
                    "step", taskId,
                    "tool", toolName,
                    "status", status,
                    "retryCount", retryCount,
                    "message", safe(message),
                    "attemptedAt", attemptStartedAt.toString()
            );
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }

    @FunctionalInterface
    private interface AgentTool<T> {
        T run() throws Exception;
    }

    @FunctionalInterface
    private interface AgentVerifier<T> {
        AgentVerification verify(T result);
    }

    private record AgentVerification(boolean passed, String message) {
        static AgentVerification pass(String message) {
            return new AgentVerification(true, message);
        }

        static AgentVerification fail(String message) {
            return new AgentVerification(false, message);
        }
    }

    private Map<String, Object> buildStageCards(String productName, String hsCode, String targetCountry,
                                                int overall, int market, int regulation, int recall,
                                                int fto, int support, List<Map<String, String>> risks,
                                                Map<String, Object> roadmap,
                                                List<Map<String, Object>> sources,
                                                Map<String, Object> agentResult,
                                                AgentInferenceResult llmResult,
                                                List<Map<String, Object>> loopSuggestions) {
        Map<String, Object> cards = new LinkedHashMap<>();
        cards.put("s00", List.of(
                Map.of("label", "종합 준비도", "value", overall, "note", "Spring scoring + optional FastAPI agent"),
                Map.of("label", "대상 시장", "value", targetCountry, "note", "정규화된 진단 대상국"),
                Map.of("label", "근거 출처", "value", sources.size(), "note", "점수 기여 source trace 수")
        ));
        cards.put("s02", sources.stream().map(source -> Map.of(
                "name", stringValue(source.get("datasetName")),
                "provider", stringValue(source.get("provider")),
                "status", Boolean.TRUE.equals(source.get("fallbackUsed")) ? "fallback" : "connected",
                "contribution", source.get("scoreContribution")
        )).collect(Collectors.toList()));
        cards.put("s03", Map.of(
                "predictions", List.of(
                        Map.of("name", "시장성", "value", market),
                        Map.of("name", "규제 준비도", "value", regulation),
                        Map.of("name", "리콜 안정성", "value", recall),
                        Map.of("name", "FTO", "value", fto),
                        Map.of("name", "지원 적합도", "value", support)
                ),
                "features", List.of("hsCode=" + nullToEmpty(hsCode), "country=" + targetCountry,
                        "agent=" + (llmResult.isSucceeded() ? llmResult.getProvider()
                                : agentResult != null ? "fastapi" : "spring-local"))
        ));
        cards.put("s04", buildMarketStageCard(targetCountry, market, sources));
        cards.put("s05", buildRegulationStageCard(targetCountry, regulation, sources));
        cards.put("s06", buildRecallStageCard(recall, sources, risks));
        cards.put("s07", buildFtoSupportStageCard(targetCountry, fto, support, sources));
        cards.put("s08", roadmap);
        cards.put("s09", Map.of(
                "summary", buildSummary(productName, hsCode, targetCountry, overall, risks),
                "scoreBars", List.of(
                        Map.of("name", "Market", "value", market),
                        Map.of("name", "Regulation", "value", regulation),
                        Map.of("name", "Recall", "value", recall),
                        Map.of("name", "FTO", "value", fto),
                        Map.of("name", "Support", "value", support)
                )
        ));
        cards.put("s10", sources);
        cards.put("knowledgeLoop", loopSuggestions);
        return cards;
    }

    private List<Map<String, Object>> buildMarketStageCard(String targetCountry, int market,
                                                           List<Map<String, Object>> sources) {
        int publicCount = sourceCount(sources, "market", "live", "cache");
        int fallbackCount = sourceCount(sources, "market", "fallback");
        int evidenceStrength = maxEvidenceStrength(sources, "market");
        return List.of(Map.of(
                "flag", countryLabel(targetCountry),
                "score", market,
                "grade", gradeClass(market),
                "gradeLabel", gradeLabel(market),
                "marketSize", publicCount + " live/cache",
                "growth", "evidence " + evidenceStrength + "/5",
                "evidenceNote", "market source trace " + publicCount + "건, fallback " + fallbackCount + "건"
        ));
    }

    private List<Map<String, Object>> buildRegulationStageCard(String targetCountry, int regulation,
                                                               List<Map<String, Object>> sources) {
        int publicCount = sourceCount(sources, "regulation", "live", "cache");
        int fallbackCount = sourceCount(sources, "regulation", "fallback");
        String statusLabel = regulation >= 75 ? "대응가능" : regulation >= 60 ? "보완필요" : "누락위험";
        String statusType = regulation >= 75 ? "ok" : regulation >= 60 ? "warn" : "req";
        return List.of(
                Map.of(
                        "nm", "대상국 규제 준비도",
                        "mkt", "Spring rule score + 인증 지식",
                        "market", List.of("req", targetCountry),
                        "priority", List.of(regulation >= 70 ? "warn" : "req", regulation >= 70 ? "주의" : "긴급"),
                        "status", List.of(statusType, statusLabel),
                        "duration", regulation >= 75 ? "2-4주" : "4-8주",
                        "cost", publicCount > 0 ? "출처확인" : "견적필요"
                ),
                Map.of(
                        "nm", "공공 API 규제 출처",
                        "mkt", "sourceType live/cache/fallback 분리",
                        "market", List.of(publicCount > 0 ? "ok" : "warn", targetCountry),
                        "priority", List.of(fallbackCount > 0 ? "warn" : "ok", fallbackCount > 0 ? "확인" : "정상"),
                        "status", List.of(publicCount > 0 ? "ok" : "warn", publicCount + "건 연결"),
                        "duration", fallbackCount > 0 ? "키/엔드포인트 확인" : "완료",
                        "cost", "내부"
                )
        );
    }

    private List<Map<String, Object>> buildRecallStageCard(int recall, List<Map<String, Object>> sources,
                                                           List<Map<String, String>> risks) {
        String riskClass = recall >= 75 ? "l" : recall >= 60 ? "m" : "h";
        String badge = recall >= 75 ? "저위험" : recall >= 60 ? "중위험" : "고위험";
        int publicCount = sourceCount(sources, "recall", "live", "cache");
        String topRisk = risks.isEmpty() ? "출처 최신성 점검" : risks.get(0).get("label");
        return List.of(
                Map.of(
                        "riskClass", riskClass,
                        "badge", badge,
                        "title", "제품군 위해·리콜 신호",
                        "similarity", String.format("%.2f", recall / 100.0),
                        "checks", List.of(
                                topRisk,
                                "Safety source trace " + publicCount + "건 확인",
                                "fallback 발생 시 담당자 근거 문서 지식화"
                        )
                )
        );
    }

    private Map<String, Object> buildFtoSupportStageCard(String targetCountry, int fto, int support,
                                                         List<Map<String, Object>> sources) {
        int ftoPublicCount = sourceCount(sources, "fto", "live", "cache");
        int supportPublicCount = sourceCount(sources, "support", "live", "cache");
        List<Map<String, Object>> patents = List.of(Map.of(
                "riskClass", fto >= 75 ? "l" : fto >= 60 ? "m" : "h",
                "badge", fto >= 75 ? "저위험" : fto >= 60 ? "중위험" : "고위험",
                "name", "대상국 FTO 준비도",
                "sub", "KIPRIS/public trace " + ftoPublicCount + "건 · score " + fto + "/100"
        ));
        List<Map<String, Object>> supports = List.of(Map.of(
                "name", targetCountry + " 수출 지원 적합도",
                "org", "Bizinfo/KOTRA/public trace " + supportPublicCount + "건 · score " + support + "/100",
                "dday", support >= 70 ? "우선" : "보강",
                "isGreen", support >= 70
        ));
        return Map.of("patents", patents, "supports", supports);
    }

    private List<Map<String, Object>> buildKnowledgeLoopSuggestions(AgentInferenceResult llmResult,
                                                                    List<Map<String, String>> risks,
                                                                    List<Map<String, Object>> sources,
                                                                    String productName,
                                                                    String hsCode,
                                                                    String targetCountry) {
        if (llmResult.getKnowledgeSuggestions() != null && !llmResult.getKnowledgeSuggestions().isEmpty()) {
            return llmResult.getKnowledgeSuggestions().stream()
                    .map(this::normalizeLoopSuggestion)
                    .limit(5)
                    .toList();
        }

        List<Map<String, Object>> suggestions = new ArrayList<>();
        String topRisk = risks.isEmpty() ? "출처 최신성 점검" : risks.get(0).get("label");
        suggestions.add(Map.of(
                "type", "risk_evidence",
                "title", "반복 진단 리스크: " + topRisk,
                "evidence", "제품 " + nullToEmpty(productName) + ", HS " + nullToEmpty(hsCode) + ", 대상국 " + targetCountry,
                "rationale", "진단 결과에서 가장 우선순위가 높은 보완 항목으로 식별됨",
                "impact", "담당자가 지식화하면 다음 진단의 리스크 판단 근거로 재사용"
        ));
        boolean hasFallbackSource = sources.stream().anyMatch(source -> Boolean.TRUE.equals(source.get("fallbackUsed")));
        if (hasFallbackSource) {
            suggestions.add(Map.of(
                    "type", "source_gap",
                    "title", "공공 API fallback 발생 출처 보강",
                    "evidence", "일부 public data source가 fallback 상태로 진단에 반영됨",
                    "rationale", "실제 API key/endpoint 또는 담당자 보유 문서를 보강하면 추론 신뢰도가 상승",
                    "impact", "출처 누락으로 인한 감점과 설명 불확실성을 줄임"
            ));
        }
        suggestions.add(Map.of(
                "type", "action_template",
                "title", "담당자 실행 메모로 보관할 30/60/90일 보완계획",
                "evidence", "진단 roadmap과 riskItems 기반",
                "rationale", "팀 내부 실행 기준을 누적하면 유사 제품 진단 시 반복 업무를 줄일 수 있음",
                "impact", "다음 진단에서 담당자 정책/실행 이력으로 참조"
        ));
        return suggestions.stream().limit(5).toList();
    }

    private Map<String, Object> normalizeLoopSuggestion(Map<String, Object> item) {
        return Map.of(
                "type", defaultString(item.get("type"), "policy_note"),
                "title", defaultString(item.get("title"), "AI 추론 근거 지식화 후보"),
                "evidence", defaultString(item.get("evidence"), ""),
                "rationale", defaultString(item.get("rationale"), defaultString(item.get("reasoning"), "")),
                "impact", defaultString(item.get("impact"), "다음 진단의 RAG context로 재사용")
        );
    }

    private void persistKnowledgeLoopSuggestions(DiagnosisRun run, Product product, List<Map<String, Object>> suggestions) {
        if (product == null || suggestions == null || suggestions.isEmpty()) return;
        for (Map<String, Object> suggestion : suggestions) {
            KnowledgeLoopItem item = KnowledgeLoopItem.builder()
                    .product(product)
                    .diagnosisId(run.getId())
                    .type(prefix(defaultString(suggestion.get("type"), "policy_note"), 60))
                    .title(prefix(defaultString(suggestion.get("title"), "AI 추론 근거 지식화 후보"), 200))
                    .evidence(defaultString(suggestion.get("evidence"), ""))
                    .rationale(defaultString(suggestion.get("rationale"), ""))
                    .impact(defaultString(suggestion.get("impact"), ""))
                    .status(KnowledgeLoopStatus.pending)
                    .createdBy(run.getUser())
                    .build();
            knowledgeLoopItemRepository.save(item);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractReportMarkdown(Map<String, Object> reportResult, String productName, String hsCode,
                                         String targetCountry, int overall,
                                         List<Map<String, String>> risks,
                                         Map<String, Object> roadmap) {
        if (reportResult != null && reportResult.get("markdown") instanceof String markdown && !markdown.isBlank()) {
            return markdown;
        }
        String topRisk = risks.isEmpty() ? "출처 최신성 점검" : risks.get(0).get("label");
        return "# GROW-GATE Local EX 수출 준비도 리포트\n\n"
                + "- 제품: " + (nullToEmpty(productName).isBlank() ? "입력 제품" : productName) + "\n"
                + "- HS Code: " + (nullToEmpty(hsCode).isBlank() ? "미등록" : hsCode) + "\n"
                + "- 대상국: " + targetCountry + "\n"
                + "- 준비도 점수: " + overall + "/100\n\n"
                + "## 핵심 요약\n"
                + topRisk + "을 우선 보완해야 합니다.\n\n"
                + "## 30/60/90일 실행량\n"
                + "- 30일: " + roadmap.getOrDefault("d30", 0) + "개 핵심 태스크\n"
                + "- 60일: " + roadmap.getOrDefault("d60", 0) + "개 보완 태스크\n"
                + "- 90일: " + roadmap.getOrDefault("d90", 0) + "개 확장 태스크\n\n"
                + "## 유의사항\n"
                + "- 본 리포트는 등록 제품 지식과 공공데이터 출처 신호 기반 진단입니다.\n"
                + "- 최종 인증·특허 판단은 전문가 검토가 필요합니다.\n";
    }

    private int scoreMarket(String hsCode, String targetCountry, List<Map<String, Object>> sources) {
        int score = 62;
        int contribution = 0;
        String reason = "HS Code/대상국 기준 로컬 공공데이터 신호 매칭";

        if (startsWithAny(hsCode, "8507", "8541", "8537")) {
            contribution += targetCountry.equals("DE") || targetCountry.equals("EU") ? 18 : 12;
            reason = "배터리·전장 품목군의 최근 수입/전환수요 신호 반영";
        } else if (startsWithAny(hsCode, "3304", "3401")) {
            contribution += targetCountry.equals("JP") || targetCountry.equals("US") ? 12 : 8;
            reason = "소비재 품목군의 시장 접근성 신호 반영";
        } else if (hsCode.length() >= 6) {
            contribution += 6;
        } else {
            contribution -= 10;
            reason = "HS Code가 충분히 정규화되지 않아 시장 매칭 신뢰도 하락";
        }

        score += contribution;
        sources.add(source("market", "RULE-MARKET-" + prefix(hsCode, 6) + "-" + targetCountry,
                "RULE_ENGINE", "로컬 HS/국가 시장성 휴리스틱", "",
                "hsCode=" + nullToEmpty(hsCode) + ";country=" + targetCountry, "market", contribution,
                false, reason));
        return clamp(score);
    }

    private int scoreRegulation(String hsCode, String targetCountry, List<Certification> certs,
                                List<Map<String, Object>> sources) {
        int score = 66;
        int countryPenalty = targetCountry.equals("EU") || targetCountry.equals("DE") || targetCountry.equals("US") ? -8 : -4;
        int certContribution = certs.isEmpty() ? -14 : Math.min(14, certs.size() * 7);
        int expiryPenalty = 0;
        LocalDate today = LocalDate.now();
        for (Certification cert : certs) {
            if (cert.getExpiresAt() == null) continue;
            long days = ChronoUnit.DAYS.between(today, cert.getExpiresAt());
            if (days < 0) expiryPenalty -= 18;
            else if (days <= 90) expiryPenalty -= 8;
        }

        sources.add(source("regulation", "RULE-REGULATION-" + targetCountry,
                "RULE_ENGINE", "로컬 인증·만료일 규제 준비도 휴리스틱", "",
                "country=" + targetCountry + ";certCount=" + certs.size(), "regulation",
                countryPenalty + certContribution + expiryPenalty, certs.isEmpty(),
                certs.isEmpty() ? "등록 인증서가 없어 규제 준비도 감점" : "등록 인증서와 만료일을 규제 준비도에 반영"));
        return clamp(score + countryPenalty + certContribution + expiryPenalty);
    }

    private int scoreRecall(String hsCode, String description, List<Map<String, Object>> sources) {
        int score = 78;
        int contribution = 0;
        String lower = nullToEmpty(description).toLowerCase();
        if (startsWithAny(hsCode, "8507") || lower.contains("battery") || lower.contains("배터리")) {
            contribution -= 13;
        } else if (startsWithAny(hsCode, "3304")) {
            contribution -= 7;
        } else {
            contribution += 4;
        }
        sources.add(source("recall", "RULE-RECALL-" + prefix(hsCode, 4),
                "RULE_ENGINE", "로컬 제품군 위해·리콜 위험 휴리스틱", "",
                "hsPrefix=" + prefix(hsCode, 4), "recall", contribution, false,
                "HS 품목군과 설명 키워드를 위해사례 신호에 매칭"));
        return clamp(score + contribution);
    }

    private int scoreFto(List<Fto> ftoItems, String targetCountry, List<Map<String, Object>> sources) {
        if (ftoItems.isEmpty()) {
            sources.add(source("fto", "RULE-FTO-MISSING-" + targetCountry,
                    "RULE_ENGINE", "로컬 FTO 등록 여부 휴리스틱", "",
                    "country=" + targetCountry + ";ftoCount=0", "fto", -20, true,
                    "FTO 검토 항목이 없어 특허 리스크를 높게 반영"));
            return 50;
        }

        int score = 76;
        int contribution = 0;
        for (Fto item : ftoItems) {
            if (!countryMatches(item.getTargetCountry(), targetCountry)) continue;
            if (item.getRiskLevel() == RiskLevel.high) contribution -= 24;
            else if (item.getRiskLevel() == RiskLevel.medium) contribution -= 12;
            else if (item.getRiskLevel() == RiskLevel.low) contribution += 8;
        }
        sources.add(source("fto", "RULE-FTO-" + targetCountry,
                "RULE_ENGINE", "로컬 FTO 위험도 휴리스틱", "",
                "country=" + targetCountry + ";ftoCount=" + ftoItems.size(), "fto", contribution,
                false, "등록된 FTO 위험 수준을 대상국 기준으로 반영"));
        return clamp(score + contribution);
    }

    private int scoreSupport(ProductKnowledge knowledge, List<BuyerRequirement> buyers,
                             List<KnowledgeSource> knowledgeSources, String targetCountry,
                             List<Map<String, Object>> sources) {
        int score = 58;
        int contribution = 0;
        if (knowledge != null && knowledge.getTrustGrade() == TrustGrade.high) contribution += 12;
        else if (knowledge != null && knowledge.getTrustGrade() == TrustGrade.medium) contribution += 6;
        else contribution -= 6;
        contribution += Math.min(12, buyers.size() * 6);
        contribution += Math.min(10, knowledgeSources.size() * 3);

        sources.add(source("support", "RULE-SUPPORT-" + targetCountry,
                "RULE_ENGINE", "로컬 지식·바이어·출처 기반 지원 적합도 휴리스틱", "",
                "country=" + targetCountry + ";buyerCount=" + buyers.size(), "support", contribution,
                knowledgeSources.isEmpty(), "지식 출처 수, 바이어 요구사항, 신뢰 등급을 실행 가능성에 반영"));
        return clamp(score + contribution);
    }

    private List<Map<String, String>> buildRiskItems(List<Certification> certs, List<Fto> ftoItems,
                                                     ProductKnowledge knowledge, List<BuyerRequirement> buyers,
                                                     String hsCode) {
        List<Map<String, String>> risks = new ArrayList<>();
        LocalDate today = LocalDate.now();
        boolean hasExpired = certs.stream().anyMatch(cert ->
                cert.getExpiresAt() != null && cert.getExpiresAt().isBefore(today));
        boolean hasExpiring = certs.stream().anyMatch(cert ->
                cert.getExpiresAt() != null && !cert.getExpiresAt().isBefore(today)
                        && ChronoUnit.DAYS.between(today, cert.getExpiresAt()) <= 90);
        boolean hasHighFto = ftoItems.stream().anyMatch(fto -> fto.getRiskLevel() == RiskLevel.high);

        if (hasHighFto) risks.add(Map.of("level", "high", "label", "대상국 FTO 고위험 항목 보완 필요"));
        if (hasExpired) risks.add(Map.of("level", "high", "label", "만료된 인증서가 있어 규제 제출 전 갱신 필요"));
        if (hasExpiring) risks.add(Map.of("level", "medium", "label", "90일 이내 만료 예정 인증서 확인 필요"));
        if (ftoItems.isEmpty()) risks.add(Map.of("level", "medium", "label", "FTO 검토 근거가 등록되지 않음"));
        if (buyers.isEmpty()) risks.add(Map.of("level", "medium", "label", "바이어 요구사항이 없어 실행 적합도 신뢰도 낮음"));
        if (knowledge == null || knowledge.getTrustGrade() == null) {
            risks.add(Map.of("level", "medium", "label", "제품 지식 신뢰 등급이 미등록 상태"));
        }
        if (startsWithAny(hsCode, "8507")) {
            risks.add(Map.of("level", "medium", "label", "배터리 품목군은 운송·안전 인증 증빙 강화 필요"));
        }
        if (risks.isEmpty()) risks.add(Map.of("level", "low", "label", "치명 위험은 낮으나 출처 최신성 점검 필요"));
        return risks;
    }

    private Map<String, Object> buildScoringRubric() {
        return Map.of(
                "version", "submission-2026-07-01",
                "formula", "overall = market*0.25 + regulation*0.22 + recall*0.16 + fto*0.20 + support*0.17",
                "dimensions", List.of(
                        Map.of("key", "market", "weight", 0.25, "evidence", "HS code, target country, KCS/KOTRA public-data source trace"),
                        Map.of("key", "regulation", "weight", 0.22, "evidence", "target-market rule, certification count, expiry risk, public-data trace"),
                        Map.of("key", "recall", "weight", 0.16, "evidence", "HS/product keyword risk and SafetyKorea/public recall source trace"),
                        Map.of("key", "fto", "weight", 0.20, "evidence", "registered FTO risk and KIPRIS/public trace"),
                        Map.of("key", "support", "weight", 0.17, "evidence", "knowledge trust, buyer requirements, source count, Bizinfo/KOTRA trace")
                ),
                "guardrails", List.of(
                        "live/cache/fallback/heuristic/agent sourceType을 결과에 함께 저장",
                        "fallback 공공데이터는 감점 또는 낮은 contribution으로만 반영",
                        "LLM provider는 점수 산식의 단일 근거가 아니라 설명과 지식화 후보 보조 역할"
                )
        );
    }

    private Map<String, Object> buildRoadmap(List<Map<String, String>> risks, List<Certification> certs,
                                             List<Fto> ftoItems, List<BuyerRequirement> buyers,
                                             List<KnowledgeSource> sources) {
        int high = (int) risks.stream().filter(risk -> "high".equals(risk.get("level"))).count();
        int medium = (int) risks.stream().filter(risk -> "medium".equals(risk.get("level"))).count();
        return Map.of(
                "d30", Math.max(2, high + (certs.isEmpty() ? 1 : 0) + (ftoItems.isEmpty() ? 1 : 0)),
                "d60", Math.max(2, medium + (buyers.isEmpty() ? 1 : 0)),
                "d90", Math.max(2, sources.isEmpty() ? 4 : 2)
        );
    }

    private String buildSummary(String productName, String hsCode, String targetCountry, int overall,
                                List<Map<String, String>> risks) {
        String topRisk = risks.isEmpty() ? "출처 최신성 점검" : risks.get(0).get("label");
        return String.format("%s(HS %s)의 %s 진단 점수는 %d점입니다. 주요 보완 항목은 %s입니다.",
                nullToEmpty(productName).isBlank() ? "입력 제품" : productName,
                nullToEmpty(hsCode).isBlank() ? "미등록" : hsCode,
                targetCountry,
                overall,
                topRisk);
    }

    private Map<String, Object> source(String field, String sourceId, String provider, String datasetName,
                                       String url, String matchedKey, String usedFor, int scoreContribution,
                                       boolean fallbackUsed, String reason) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("field", field);
        source.put("sourceId", sourceId);
        source.put("provider", provider);
        source.put("datasetName", datasetName);
        source.put("documentName", datasetName);
        source.put("url", url);
        source.put("retrievedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        source.put("recordId", sourceId);
        source.put("matchedKey", matchedKey);
        source.put("usedFor", usedFor);
        source.put("scoreContribution", scoreContribution);
        source.put("fallbackUsed", fallbackUsed);
        source.put("sourceType", sourceType(provider, field));
        source.put("reason", reason);
        return source;
    }

    private String sourceType(PublicDataRecord record) {
        if (record.isCacheHit()) return "cache";
        if (record.isFallbackUsed()) return "fallback";
        return "live";
    }

    private String sourceType(String provider, String field) {
        if ("RULE_ENGINE".equals(provider)) return "heuristic";
        if ("agent".equals(field)) return "agent";
        return "internal";
    }

    private int sourceCount(List<Map<String, Object>> sources, String field, String... sourceTypes) {
        List<String> allowed = List.of(sourceTypes);
        return (int) sources.stream()
                .filter(source -> field.equals(source.get("field")) || field.equals(source.get("usedFor")))
                .filter(source -> allowed.isEmpty() || allowed.contains(stringValue(source.get("sourceType"))))
                .count();
    }

    private int maxEvidenceStrength(List<Map<String, Object>> sources, String field) {
        return sources.stream()
                .filter(source -> field.equals(source.get("field")) || field.equals(source.get("usedFor")))
                .map(source -> source.get("domainSignals"))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(signals -> signals.get("evidenceStrength"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToInt(Number::intValue)
                .max()
                .orElse(0);
    }

    private String gradeClass(int score) {
        if (score >= 75) return "h";
        if (score >= 60) return "m";
        return "l";
    }

    private String gradeLabel(int score) {
        if (score >= 75) return "HIGH";
        if (score >= 60) return "MEDIUM";
        return "LOW";
    }

    private String countryLabel(String targetCountry) {
        return switch (targetCountry) {
            case "DE" -> "Germany / EU";
            case "US" -> "United States";
            case "JP" -> "Japan";
            case "CN" -> "China";
            case "EU" -> "European Union";
            default -> targetCountry;
        };
    }

    private Map<String, Object> parseInputData(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse diagnosis input_data JSON");
            return Map.of();
        }
    }

    private Map<String, Object> parseObjectMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private boolean startsWithAny(String value, String... prefixes) {
        String safe = nullToEmpty(value);
        for (String prefix : prefixes) {
            if (safe.startsWith(prefix)) return true;
        }
        return false;
    }

    private boolean countryMatches(String value, String normalizedCountry) {
        return Objects.equals(normalizeCountry(value), normalizedCountry)
                || "EU".equals(normalizedCountry) && List.of("DE", "FR", "NL", "IT", "ES").contains(normalizeCountry(value));
    }

    private String normalizeCountry(String value) {
        String v = nullToEmpty(value).trim().toLowerCase().replace(" ", "");
        if (v.equals("독일") || v.equals("germany") || v.equals("de")) return "DE";
        if (v.equals("미국") || v.equals("usa") || v.equals("us")) return "US";
        if (v.equals("일본") || v.equals("japan") || v.equals("jp")) return "JP";
        if (v.equals("중국") || v.equals("china") || v.equals("cn")) return "CN";
        if (v.equals("유럽") || v.equals("eu")) return "EU";
        return nullToEmpty(value).trim().toUpperCase();
    }

    private String normalizeHsCode(String value) {
        return nullToEmpty(value).replaceAll("[^0-9]", "");
    }

    private String prefix(String value, int length) {
        String safe = nullToEmpty(value);
        return safe.length() <= length ? safe : safe.substring(0, length);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String defaultString(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
