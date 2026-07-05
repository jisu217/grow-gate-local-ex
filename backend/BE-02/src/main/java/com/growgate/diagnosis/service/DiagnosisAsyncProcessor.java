package com.growgate.diagnosis.service;

// 비동기 진단 실행 처리 (@Async)
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.agent.service.AgentInferenceRequest;
import com.growgate.agent.service.AgentInferenceResult;
import com.growgate.agent.service.AgentInferenceService;
import com.growgate.agent.service.ClaimVerificationService;
import com.growgate.agent.service.EvidenceContextBuilder;
import com.growgate.diagnosis.entity.DiagnosisRun;
import com.growgate.diagnosis.enums.DiagnosisStatus;
import com.growgate.diagnosis.report.MarkdownReportRenderer;
import com.growgate.diagnosis.report.ReportClaimVerifier;
import com.growgate.diagnosis.report.ReportFrontMatterBuilder;
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
import com.growgate.regional.service.RegionalIndustryProfileService;
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
    private final EvidenceContextBuilder evidenceContextBuilder;
    private final KnowledgeLoopItemRepository knowledgeLoopItemRepository;
    private final ClaimVerificationService claimVerificationService;
    private final RegionalIndustryProfileService regionalIndustryProfileService;

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
            Map<String, Object> buyerContext = buildBuyerContext(inputData, buyersFromInput(inputData));
            Map<String, Object> regionalIndustryProfile = buildRegionalIndustryProfile(run, inputData, hsCode, productName);
            Map<String, Object> buyerRequestTrace = buildBuyerRequestTrace(buyerContext);

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
            addRegionalIndustrySource(sources, regionalIndustryProfile);
            addBuyerRequestTraceSource(sources, buyerRequestTrace);
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
            Map<String, Object> workerRequest = buildWorkerRequest(run, product, productName, hsCode, description,
                    targetCountry, publicDataResult.getRecords(), knowledgeSources, certs, ftoItems, buyers,
                    acceptedLoopItems, regionalIndustryProfile, buyerRequestTrace);
            Optional<Map<String, Object>> workerResult = agentLoop.runTool(
                    "python_data_ai_engine",
                    "Python Worker /worker/diagnosis-v1",
                    2,
                    Optional.empty(),
                    () -> fastApiAgentClient.workerDiagnosis(workerRequest),
                    result -> result != null && result.isPresent()
                            && "python-worker-diagnosis-v1".equals(stringValue(result.get().get("engineVersion")))
                            ? AgentVerification.pass("Python worker returned normalized metrics and evidence artifacts")
                            : AgentVerification.fail("Python worker unavailable; Spring will use local evidence artifacts")
            );

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
                        int supportScore = scoreSupport(knowledge, buyers, knowledgeSources, targetCountry, sources,
                                regionalIndustryProfile);
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
                        List<Map<String, Object>> risks = buildRiskItems(certs, ftoItems, knowledge, buyers, hsCode, buyerContext, sources);
                        Map<String, Object> roadmap = buildActionPlan(risks, certs, ftoItems, buyers, knowledgeSources, buyerContext);
                        return Map.of("riskItems", risks, "actionRoadmap", roadmap);
                    },
                    result -> result != null && result.get("riskItems") instanceof List<?> risks && !risks.isEmpty()
                            ? AgentVerification.pass("risk items and roadmap generated")
                            : AgentVerification.fail("risk planner did not produce risk items")
            );
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> riskItems = (List<Map<String, Object>>) planningResult.get("riskItems");
            @SuppressWarnings("unchecked")
            Map<String, Object> actionRoadmap = (Map<String, Object>) planningResult.get("actionRoadmap");
            Map<String, Object> confidencePanel = buildConfidencePanel(sources);
            Map<String, Object> gateDecision = buildGateDecision(overall, market, regulation, recall, fto, support,
                    riskItems, confidencePanel, buyerContext);
            Map<String, Object> impactEstimate = buildImpactEstimate(riskItems, actionRoadmap, buyerContext, support);
            Map<String, Object> agentRequest = buildAgentRequest(run, productName, hsCode, description, targetCountry,
                    certs, regionalIndustryProfile, buyerRequestTrace);
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
            Map<String, Object> evidenceContext = evidenceContextBuilder.build(knowledge, certs, ftoItems, buyers,
                    knowledgeSources, publicDataResult.getRecords(), acceptedLoopItems);
            AgentInferenceRequest inferenceRequest = AgentInferenceRequest.builder()
                    .userId(run.getUser() != null ? run.getUser().getId() : null)
                    .diagnosisId(run.getId())
                    .productName(productName)
                    .hsCode(hsCode)
                    .targetCountry(targetCountry)
                    .evidenceContext(evidenceContext)
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
            Map<String, Object> claimVerificationResult = claimVerificationService.verifyClaims(
                    extractLlmClaims(llmResult), validSourceIds(sources));
            List<String> llmFallbackReasons = combineFallbackReasons(llmResult, claimVerificationResult);

            sources.add(source("agent", "WORKFLOW-TRACE-" + targetCountry,
                    "WORKFLOW_TRACE", "Optional analysis/report bridge", "configured APP_FASTAPI_BASE_URL",
                    "diagnosisId=" + run.getId(), "agentic-analysis", 0,
                    agentResult.isEmpty(), agentResult.isPresent()
                            ? "Spring 진단 결과에 선택형 분석 bridge 결과를 병합"
                            : "선택형 분석 bridge 미활성 또는 호출 실패로 Spring 규칙 진단 유지"));
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
            resultMap.put("gateDecision", gateDecision);
            resultMap.put("impactEstimate", impactEstimate);
            resultMap.put("confidencePanel", confidencePanel);
            resultMap.put("summary", buildSummary(productName, hsCode, targetCountry, overall, riskItems));
            resultMap.put("evidenceSources", sources.stream()
                    .map(source -> stringValue(source.get("provider")))
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .collect(Collectors.toList()));
            resultMap.put("riskItems", riskItems);
            resultMap.put("actionRoadmap", actionRoadmap);
            resultMap.put("sources", sources);
            resultMap.put("sourceTrustSummary", buildSourceTrustSummary(run, sources));
            resultMap.put("regionalIndustryProfile", regionalIndustryProfile);
            resultMap.put("buyerRequestTrace", buyerRequestTrace);
            resultMap.put("scoreContributionLedger", buildScoreContributionLedger(sources));
            resultMap.put("agentTrace", agentLoop.toTrace(agentResult.isPresent(), reportResult.isPresent(), llmResult));
            resultMap.put("sourceTrace", buildSourceTrace(sources));
            Map<String, Object> springEvidenceRetrieval = Map.of(
                    "mode", stringValue(evidenceContext.get("retrievalMode")),
                    "label", stringValue(evidenceContext.get("retrievalLabel")),
                    "claimBoundary", stringValue(evidenceContext.get("claimBoundary")),
                    "query", stringValue(evidenceContext.get("retrievalQuery")),
                    "topK", evidenceContext.getOrDefault("retrievalTopK", List.of())
            );
            resultMap.put("pythonWorker", workerResult.map(result -> Map.of(
                    "enabled", true,
                    "succeeded", true,
                    "engineVersion", stringValue(result.get("engineVersion")),
                    "generatedAt", stringValue(result.get("generatedAt")),
                    "latencyMs", result.getOrDefault("latencyMs", 0),
                    "budgetGuard", result.getOrDefault("budgetGuard", Map.of()),
                    "sourceBoundVerification", result.getOrDefault("sourceBoundVerification", Map.of()),
                    "retryPlan", result.getOrDefault("retryPlan", List.of()),
                    "claimVerification", result.getOrDefault("claimVerification", Map.of())
            )).orElseGet(() -> Map.of(
                    "enabled", fastApiAgentClient.isEnabled(),
                    "succeeded", false,
                    "engineVersion", "spring-local-fallback",
                    "budgetGuard", Map.of("externalLlmCalls", 0, "estimatedCostUsd", 0)
            )));
            resultMap.put("pythonAgenticLoop", workerResult
                    .map(result -> objectMapOrDefault(result.get("agenticLoop"), Map.of()))
                    .orElseGet(Map::of));
            resultMap.put("normalizedMetrics", workerResult
                    .map(result -> result.getOrDefault("normalizedMetrics", buildNormalizedMetrics(publicDataResult.getRecords())))
                    .orElseGet(() -> buildNormalizedMetrics(publicDataResult.getRecords())));
            resultMap.put("compiledWiki", workerResult
                    .map(result -> result.getOrDefault("compiledWiki", Map.of()))
                    .orElseGet(Map::of));
            resultMap.put("evidenceRetrieval", workerResult
                    .map(result -> objectMapOrDefault(result.get("evidenceRetrieval"), springEvidenceRetrieval))
                    .orElse(springEvidenceRetrieval));
            resultMap.put("exportKnowledgeTimeline", workerResult
                    .map(result -> objectListOrDefault(result.get("exportKnowledgeTimeline"), buildExportKnowledgeTimeline(sources)))
                    .orElseGet(() -> buildExportKnowledgeTimeline(sources)));
            resultMap.put("evidenceGuardrails", workerResult
                    .map(result -> objectMapOrDefault(result.get("evidenceGuardrails"),
                            buildEvidenceGuardrails(sources, publicDataResult.getRecords())))
                    .orElseGet(() -> buildEvidenceGuardrails(sources, publicDataResult.getRecords())));
            resultMap.put("claimVerifications", objectListOrDefault(
                    claimVerificationResult.get("claimVerifications"), List.of()));
            resultMap.put("llmFallbackReasons", llmFallbackReasons);
            resultMap.put("scoringRubric", buildScoringRubric());
            resultMap.put("stageCards", buildStageCards(productName, hsCode, targetCountry, overall, market,
                    regulation, recall, fto, support, riskItems, actionRoadmap, sources, agentResult.orElse(null),
                    llmResult, loopSuggestions));
            resultMap.put("reportMarkdown", extractReportMarkdown(reportResult.orElse(null),
                    run.getId(), productName, hsCode, targetCountry, overall, riskItems, actionRoadmap,
                    publicDataResult.getRun().getId(), objectMapOrDefault(resultMap.get("sourceTrustSummary"), Map.of())));
            resultMap.put("reportFastApiUsed", reportResult.isPresent());
            resultMap.put("agentInference", Map.of(
                    "succeeded", llmResult.isSucceeded(),
                    "provider", nullToEmpty(llmResult.getProvider()),
                    "model", nullToEmpty(llmResult.getModel()),
                    "fallbackReasons", llmFallbackReasons,
                    "summary", llmResult.getContent() != null ? stringValue(llmResult.getContent().get("summary")) : ""
            ));
            resultMap.put("knowledgeLoopSuggestions", loopSuggestions);
            resultMap.put("publicDataCollectionRunId", publicDataResult.getRun().getId());
            Map<String, Object> inputTrace = new LinkedHashMap<>();
            inputTrace.put("productName", nullToEmpty(productName));
            inputTrace.put("hsCode", nullToEmpty(hsCode));
            inputTrace.put("targetCountry", targetCountry);
            inputTrace.put("region", regionalIndustryProfile.get("region"));
            inputTrace.put("industry", regionalIndustryProfile.get("industry"));
            inputTrace.put("buyerRequest", stringValue(buyerContext.get("buyerRequest")));
            inputTrace.put("buyerDeadline", stringValue(buyerContext.get("buyerDeadline")));
            inputTrace.put("expectedShipmentDate", stringValue(buyerContext.get("expectedShipmentDate")));
            inputTrace.put("requestedCertifications", buyerContext.getOrDefault("requestedCertifications", List.of()));
            inputTrace.put("currentDocuments", buyerContext.getOrDefault("currentDocuments", List.of()));
            inputTrace.put("exportExperience", stringValue(buyerContext.get("exportExperience")));
            inputTrace.put("budgetLimitKrw", buyerContext.getOrDefault("budgetLimitKrw", 0));
            inputTrace.put("knowledgeSourceCount", knowledgeSources.size());
            inputTrace.put("certificationCount", certs.size());
            inputTrace.put("buyerRequirementCount", buyers.size());
            resultMap.put("inputTrace", inputTrace);

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
            source.put("agency", record.getProvider().agency());
            source.put("ministry", record.getProvider().ministry());
            source.put("dataFamily", record.getProvider().dataFamily());
            source.put("motieRelated", record.getProvider().motieRelated());
            source.put("sourceName", record.getProvider().sourceName());
            source.put("officialUrl", record.getProvider().officialUrl());
            source.put("providerOperationEndpoint", record.getProvider().operationEndpoint());
            source.put("providerApiKeyRequired", record.getProvider().apiKeyRequired());
            source.put("licenseName", record.getProvider().licenseName());
            source.put("termsUrl", record.getProvider().termsUrl());
            source.put("redistributionAllowed", record.getProvider().redistributionAllowed());
            source.put("storageAllowed", record.getProvider().storageAllowed());
            source.put("reprocessingAllowed", record.getProvider().reprocessingAllowed());
            source.put("attributionRequired", record.getProvider().attributionRequired());
            source.put("usageWarning", record.getProvider().usageWarning());
            source.put("licenseLastVerifiedAt", record.getProvider().lastVerifiedAt());
            source.put("datasetName", publicDataDatasetName(record));
            source.put("documentName", publicDataDatasetName(record));
            source.put("url", record.getEndpoint());
            source.put("retrievedAt", record.getFetchedAt() != null
                    ? record.getFetchedAt().atOffset(ZoneOffset.UTC).toString()
                    : OffsetDateTime.now(ZoneOffset.UTC).toString());
            source.put("fetchedAt", source.get("retrievedAt"));
            source.put("recordId", record.getId());
            source.put("matchedKey", record.getMatchedKey());
            source.put("usedFor", record.getUsedFor());
            int adjustedContribution = adjustedPublicDataContribution(record);
            source.put("rawScoreContribution", record.getScoreContribution());
            source.put("confidenceAdjustedContribution", adjustedContribution);
            source.put("rawContribution", record.getScoreContribution());
            source.put("adjustedContribution", adjustedContribution);
            source.put("finalScoreDelta", adjustedContribution);
            source.put("scoreContribution", adjustedContribution);
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
                source.put("scoreInputs", metrics);
                source.put("rawMetric", metrics);
                source.put("normalizedMetric", metrics);
            }
            Object collectionEvidence = normalizedPayload.get("collectionEvidence");
            if (collectionEvidence instanceof Map<?, ?> evidence) {
                source.put("collectionEvidence", collectionEvidence);
                source.put("endpointConfigured", evidence.get("endpointConfigured"));
                source.put("operationEndpoint", evidence.get("operationEndpoint"));
                source.put("endpointType", evidence.get("endpointType"));
                source.put("apiKeyConfigured", evidence.get("apiKeyConfigured"));
                source.put("apiKeyStored", evidence.get("apiKeyStored"));
            }
            putIfPresent(source, "rawCount", normalizedPayload.get("rawCount"));
            putIfPresent(source, "normalizedCount", normalizedPayload.get("normalizedCount"));
            putIfPresent(source, "filteredCount", normalizedPayload.get("filteredCount"));
            putIfPresent(source, "zeroReason", normalizedPayload.get("zeroReason"));
            putIfPresent(source, "requestParamsMasked", normalizedPayload.get("requestParamsMasked"));
            putIfPresent(source, "sampleRawKeys", normalizedPayload.get("sampleRawKeys"));
            putIfPresent(source, "httpStatus", normalizedPayload.get("httpStatus"));
            putIfPresent(source, "zeroResultDiagnostics", normalizedPayload.get("zeroResultDiagnostics"));
            putIfPresent(source, "publishedAt", normalizedPayload.get("publishedAt"));
            putIfPresent(source, "sourcePeriodStart", normalizedPayload.get("sourcePeriodStart"));
            putIfPresent(source, "sourcePeriodEnd", normalizedPayload.get("sourcePeriodEnd"));
            putIfPresent(source, "eventDate", normalizedPayload.get("eventDate"));
            source.put("metricSummary", publicDataMetricSummary(record, normalizedPayload));
            source.put("decisionImpact", publicDataDecisionImpact(record, adjustedContribution));
            source.put("scoreFormula", normalizedPayload.getOrDefault("scoreFormula",
                    "scoreContribution = provider metric contribution adjusted by confidence"));
            source.put("reason", record.isFallbackUsed()
                    ? "live 공공 API 수집 실패/비활성 상태를 명시하고 fallback 신호로 반영"
                    : "live 공공 API 또는 유효 cache record의 도메인 신호를 진단 점수에 반영");
            sources.add(source);
        }
    }

    private Map<String, Object> buildSourceTrustSummary(DiagnosisRun run, List<Map<String, Object>> sources) {
        long liveCount = sources.stream().filter(source -> "live".equals(source.get("sourceType"))).count();
        long cacheCount = sources.stream().filter(source -> "cache".equals(source.get("sourceType"))).count();
        long fallbackCount = sources.stream().filter(source -> "fallback".equals(source.get("sourceType"))).count();
        long crawlCount = sources.stream().filter(source -> "crawl".equals(source.get("sourceType"))).count();
        long externalFeedCount = sources.stream().filter(source -> "external_feed".equals(source.get("sourceType"))).count();
        long seedCount = sources.stream().filter(source -> "seed".equals(source.get("sourceType"))).count();
        long operationEndpointLiveCount = sources.stream()
                .filter(source -> ("live".equals(source.get("sourceType")) || "cache".equals(source.get("sourceType")))
                        && Boolean.TRUE.equals(source.get("operationEndpoint")))
                .count();
        long apiKeyConfiguredLiveCount = sources.stream()
                .filter(source -> ("live".equals(source.get("sourceType")) || "cache".equals(source.get("sourceType")))
                        && Boolean.TRUE.equals(source.get("apiKeyConfigured")))
                .count();
        long officialAttemptCount = liveCount + cacheCount + fallbackCount;
        double liveSuccessRate = officialAttemptCount == 0 ? 0.0
                : Math.round(((liveCount + cacheCount) / (double) officialAttemptCount) * 1000.0) / 1000.0;
        long motieContribution = sources.stream()
                .filter(source -> Boolean.TRUE.equals(source.get("motieRelated")))
                .mapToLong(source -> source.get("scoreContribution") instanceof Number number ? number.longValue() : 0)
                .sum();
        List<String> sourceIds = sources.stream()
                .map(source -> stringValue(source.get("sourceId")))
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(12)
                .toList();
        List<String> collectedAt = sources.stream()
                .map(source -> stringValue(source.get("fetchedAt")).isBlank()
                        ? stringValue(source.get("retrievedAt")) : stringValue(source.get("fetchedAt")))
                .filter(value -> !value.isBlank())
                .sorted()
                .toList();
        List<String> periodStarts = sources.stream()
                .map(source -> stringValue(source.get("sourcePeriodStart")))
                .filter(value -> !value.isBlank())
                .sorted()
                .toList();
        List<String> periodEnds = sources.stream()
                .map(source -> stringValue(source.get("sourcePeriodEnd")))
                .filter(value -> !value.isBlank())
                .sorted()
                .toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("diagnosisId", run.getId());
        summary.put("createdAt", run.getCreatedAt() != null ? run.getCreatedAt().atOffset(ZoneOffset.UTC).toString() : "");
        summary.put("collectionRunIds", sources.stream()
                .map(source -> stringValue(source.get("collectionRunId")))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList());
        summary.put("liveCount", liveCount);
        summary.put("cacheCount", cacheCount);
        summary.put("fallbackCount", fallbackCount);
        summary.put("crawlCount", crawlCount);
        summary.put("externalFeedCount", externalFeedCount);
        summary.put("seedCount", seedCount);
        summary.put("operationEndpointLiveCount", operationEndpointLiveCount);
        summary.put("apiKeyConfiguredLiveCount", apiKeyConfiguredLiveCount);
        summary.put("liveSuccessRate", liveSuccessRate);
        summary.put("lastCollectedAt", collectedAt.isEmpty() ? "" : collectedAt.get(collectedAt.size() - 1));
        summary.put("sourcePeriodStart", periodStarts.isEmpty() ? "" : periodStarts.get(0));
        summary.put("sourcePeriodEnd", periodEnds.isEmpty() ? "" : periodEnds.get(periodEnds.size() - 1));
        summary.put("sourcePeriod", periodStarts.isEmpty() || periodEnds.isEmpty()
                ? "source period 확인 대기"
                : periodStarts.get(0) + " ~ " + periodEnds.get(periodEnds.size() - 1));
        summary.put("providerHealth", buildProviderHealth(sources));
        summary.put("sourceIds", sourceIds);
        summary.put("motieRelatedContribution", motieContribution);
        summary.put("motieRelatedSummary", motieContribution == 0
                ? "산업부 관련 출처 contribution 없음 또는 확인 대기"
                : "산업부 관련 출처가 Gate 판단에 " + (motieContribution > 0 ? "+" : "") + motieContribution + "점 기여");
        summary.put("retryPayload", retryPayload(run));
        return summary;
    }

    private List<Map<String, Object>> buildProviderHealth(List<Map<String, Object>> sources) {
        Map<String, List<Map<String, Object>>> byProvider = new LinkedHashMap<>();
        for (Map<String, Object> source : sources) {
            String provider = stringValue(source.get("provider"));
            if (provider.isBlank()) continue;
            byProvider.computeIfAbsent(provider, ignored -> new ArrayList<>()).add(source);
        }
        List<Map<String, Object>> health = new ArrayList<>();
        byProvider.forEach((provider, items) -> {
            int live = sourceTypeCount(items, "live");
            int cache = sourceTypeCount(items, "cache");
            int fallback = sourceTypeCount(items, "fallback");
            int crawl = sourceTypeCount(items, "crawl");
            int externalFeed = sourceTypeCount(items, "external_feed");
            List<String> fetched = items.stream().map(item -> stringValue(item.get("fetchedAt")))
                    .map(value -> value.isBlank() ? "" : value)
                    .filter(value -> !value.isBlank()).sorted().toList();
            if (fetched.isEmpty()) {
                fetched = items.stream().map(item -> stringValue(item.get("retrievedAt")))
                    .filter(value -> !value.isBlank()).sorted().toList();
            }
            List<String> periodStart = items.stream().map(item -> stringValue(item.get("sourcePeriodStart")))
                    .filter(value -> !value.isBlank()).sorted().toList();
            List<String> periodEnd = items.stream().map(item -> stringValue(item.get("sourcePeriodEnd")))
                    .filter(value -> !value.isBlank()).sorted().toList();
            String failureReason = items.stream()
                    .map(item -> stringValue(item.get("errorMessage")).isBlank()
                            ? stringValue(item.get("reason")) : stringValue(item.get("errorMessage")))
                    .filter(value -> !value.isBlank())
                    .findFirst()
                    .orElse("");
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("provider", provider);
            row.put("liveCount", live);
            row.put("cacheCount", cache);
            row.put("fallbackCount", fallback);
            row.put("crawlCount", crawl);
            row.put("externalFeedCount", externalFeed);
            row.put("operationEndpoint", items.stream().anyMatch(item -> Boolean.TRUE.equals(item.get("operationEndpoint"))));
            row.put("apiKeyConfigured", items.stream().anyMatch(item -> Boolean.TRUE.equals(item.get("apiKeyConfigured"))));
            row.put("operationEndpointLiveCount", items.stream()
                    .filter(item -> ("live".equals(item.get("sourceType")) || "cache".equals(item.get("sourceType")))
                            && Boolean.TRUE.equals(item.get("operationEndpoint")))
                    .count());
            row.put("apiKeyConfiguredLiveCount", items.stream()
                    .filter(item -> ("live".equals(item.get("sourceType")) || "cache".equals(item.get("sourceType")))
                            && Boolean.TRUE.equals(item.get("apiKeyConfigured")))
                    .count());
            row.put("endpointType", items.stream()
                    .map(item -> stringValue(item.get("endpointType")))
                    .filter(value -> !value.isBlank())
                    .findFirst()
                    .orElse(""));
            row.put("lastCollectedAt", fetched.isEmpty() ? "" : fetched.get(fetched.size() - 1));
            row.put("sourcePeriodStart", periodStart.isEmpty() ? "" : periodStart.get(0));
            row.put("sourcePeriodEnd", periodEnd.isEmpty() ? "" : periodEnd.get(periodEnd.size() - 1));
            row.put("failureReason", failureReason);
            row.put("status", fallback > 0 && live + cache + crawl + externalFeed == 0 ? "fallback"
                    : fallback > 0 ? "partial"
                    : live > 0 ? "live"
                    : cache > 0 ? "cache"
                    : externalFeed > 0 ? "external_feed"
                    : crawl > 0 ? "crawl" : "no_record");
            health.add(row);
        });
        return health;
    }

    private Map<String, Object> retryPayload(DiagnosisRun run) {
        Product product = run.getProduct();
        Map<String, Object> payload = new LinkedHashMap<>();
        if (product != null) {
            payload.put("hsCode", nullToEmpty(product.getHsCode()).replaceAll("[^0-9]", ""));
            payload.put("targetCountry", run.getTargetCountry());
            payload.put("productName", product.getName());
        } else {
            Map<String, Object> input = parseObjectMap(run.getInputData());
            payload.put("hsCode", nullToEmpty(stringValue(input.get("hsCode"))).replaceAll("[^0-9]", ""));
            payload.put("targetCountry", run.getTargetCountry());
            payload.put("productName", stringValue(input.getOrDefault("productName", input.getOrDefault("name", ""))));
        }
        return payload;
    }

    private List<Map<String, Object>> buildScoreContributionLedger(List<Map<String, Object>> sources) {
        return sources.stream()
                .filter(source -> source.get("sourceId") != null)
                .map(source -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("sourceId", stringValue(source.get("sourceId")));
                    row.put("provider", stringValue(source.get("provider")));
                    row.put("usedFor", stringValue(source.get("usedFor")));
                    row.put("sourceType", stringValue(source.get("sourceType")));
                    row.put("rawMetric", source.getOrDefault("rawMetric", source.getOrDefault("metrics", Map.of())));
                    row.put("normalizedMetric", source.getOrDefault("normalizedMetric", source.getOrDefault("scoreInputs", Map.of())));
                    row.put("operationEndpoint", source.getOrDefault("operationEndpoint", false));
                    row.put("apiKeyConfigured", source.getOrDefault("apiKeyConfigured", false));
                    row.put("endpointType", stringValue(source.get("endpointType")));
                    row.put("fetchedAt", stringValue(source.get("fetchedAt")));
                    row.put("rawContribution", source.getOrDefault("rawContribution", source.getOrDefault("rawScoreContribution", 0)));
                    row.put("confidence", source.getOrDefault("confidence", 0));
                    row.put("adjustedContribution", source.getOrDefault("adjustedContribution",
                            source.getOrDefault("confidenceAdjustedContribution", 0)));
                    row.put("finalScoreDelta", source.getOrDefault("finalScoreDelta",
                            source.getOrDefault("scoreContribution", 0)));
                    row.put("metricSummary", stringValue(source.get("metricSummary")));
                    return row;
                })
                .toList();
    }

    private int applyPublicDataContribution(int baseScore, List<PublicDataRecord> records, String usedFor) {
        int contribution = records.stream()
                .filter(record -> usedFor.equals(record.getUsedFor()))
                .mapToInt(this::adjustedPublicDataContribution)
                .sum();
        return clamp(baseScore + contribution);
    }

    private int adjustedPublicDataContribution(PublicDataRecord record) {
        int raw = record.getScoreContribution() == null ? 0 : record.getScoreContribution();
        double confidence = record.getConfidence() == null ? 0.0 : Math.max(0.0, Math.min(1.0, record.getConfidence()));
        if (record.isFallbackUsed()) return Math.min(raw, -1) - 2;
        if (confidence < 0.20) return Math.min(raw, 0) - 1;
        if (raw == 0) return 0;
        int adjusted = (int) Math.round(raw * confidence);
        if (raw > 0 && adjusted == 0) return 1;
        if (raw < 0 && adjusted == 0) return -1;
        return adjusted;
    }

    private String publicDataDatasetName(PublicDataRecord record) {
        return switch (record.getProvider()) {
            case KCS_TRADE -> "관세청 수출입무역통계 live/cache 수집";
            case KOTRA_MARKET -> "KOTRA 시장·뉴스 live/cache 수집";
            case SAFETY_RECALL -> "제품안전·리콜 live/cache 수집";
            case KIPRIS_FTO -> "KIPRIS Plus 특허/FTO live/cache 수집";
            case BIZINFO_SUPPORT -> "기업마당 지원사업 live/cache 수집";
            case MOTIE_WEB_CRAWL -> "산업부 공개 웹 안전 크롤링 수집";
            case EXPORT_EVIDENCE_FEED -> "수출 뉴스·보고서 RSS/API feed 수집";
        };
    }

    private String publicDataMetricSummary(PublicDataRecord record, Map<String, Object> normalizedPayload) {
        Object summary = normalizedPayload.get("metricSummary");
        if (summary instanceof String text && !text.isBlank()) return text;
        Object metrics = normalizedPayload.get("metrics");
        if (metrics instanceof Map<?, ?> metricMap && !metricMap.isEmpty()) {
            return metricMap.entrySet().stream()
                    .limit(4)
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("metric record 없음");
        }
        return record.isFallbackUsed()
                ? "fallback record: live metric 미확보"
                : "live/cache record 수집됨, provider parser metric 확인 필요";
    }

    private String publicDataDecisionImpact(PublicDataRecord record, int adjustedContribution) {
        if (record.isFallbackUsed()) {
            return "fallback은 점수를 올리지 않고 confidence 하락 및 담당자 확인 항목으로만 반영됩니다.";
        }
        String direction = adjustedContribution > 0 ? "상향" : adjustedContribution < 0 ? "하향" : "중립";
        return record.getProvider().name() + " " + record.getUsedFor()
                + " metric이 Gate 판단에 " + direction + " 기여(" + adjustedContribution + "점)했습니다.";
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value == null) return;
        String text = stringValue(value);
        if (!text.isBlank()) target.put(key, text);
    }

    private Map<String, Object> buildAgentRequest(DiagnosisRun run, String productName, String hsCode,
                                                  String description, String targetCountry,
                                                  List<Certification> certs,
                                                  Map<String, Object> regionalIndustryProfile,
                                                  Map<String, Object> buyerRequestTrace) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("company_name", run.getCompany() != null ? run.getCompany().getName() : "개인 사업자");
        payload.put("region", regionalIndustryProfile.getOrDefault("region", "지역 미입력"));
        payload.put("industry", regionalIndustryProfile.getOrDefault("industry",
                run.getCompany() != null ? nullToEmpty(run.getCompany().getIndustry()) : "제조"));
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
        payload.put("regional_industry_profile", regionalIndustryProfile);
        payload.put("buyer_request_trace", buyerRequestTrace);
        return payload;
    }

    private Map<String, Object> buildWorkerRequest(DiagnosisRun run, Product product, String productName,
                                                   String hsCode, String description, String targetCountry,
                                                   List<PublicDataRecord> records,
                                                   List<KnowledgeSource> knowledgeSources,
                                                   List<Certification> certs,
                                                   List<Fto> ftoItems,
                                                   List<BuyerRequirement> buyers,
                                                   List<KnowledgeLoopItem> acceptedLoopItems,
                                                   Map<String, Object> regionalIndustryProfile,
                                                   Map<String, Object> buyerRequestTrace) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("diagnosisId", run.getId());
        payload.put("userId", run.getUser() != null ? run.getUser().getId() : null);
        payload.put("companyId", run.getCompany() != null ? run.getCompany().getId() : null);
        payload.put("productSnapshot", Map.of(
                "productId", product != null ? product.getId() : "",
                "productName", nullToEmpty(productName).isBlank() ? "입력 제품" : productName,
                "hsCode", nullToEmpty(hsCode),
                "targetCountry", targetCountry,
                "region", regionalIndustryProfile.getOrDefault("region", ""),
                "industry", regionalIndustryProfile.getOrDefault("industry", ""),
                "descriptionPresent", !nullToEmpty(description).isBlank()
        ));
        payload.put("knowledgeSnapshot", Map.of(
                "knowledgeSourceCount", knowledgeSources.size(),
                "certificationCount", certs.size(),
                "ftoItemCount", ftoItems.size(),
                "buyerRequirementCount", buyers.size(),
                "acceptedLoopItemCount", acceptedLoopItems.size()
        ));
        payload.put("regionalIndustryProfile", regionalIndustryProfile);
        payload.put("buyerRequestTrace", buyerRequestTrace);
        payload.put("publicDataRecords", records.stream()
                .map(this::workerRecord)
                .toList());
        return payload;
    }

    private Map<String, Object> workerRecord(PublicDataRecord record) {
        Map<String, Object> normalizedPayload = parseObjectMap(record.getNormalizedPayload());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", nullToEmpty(record.getId()));
        item.put("provider", record.getProvider() != null ? record.getProvider().name() : "");
        item.put("endpoint", nullToEmpty(record.getEndpoint()));
        item.put("requestKey", nullToEmpty(record.getRequestKey()));
        item.put("hsCode", nullToEmpty(record.getHsCode()));
        item.put("targetCountry", nullToEmpty(record.getTargetCountry()));
        item.put("matchedKey", nullToEmpty(record.getMatchedKey()));
        item.put("usedFor", nullToEmpty(record.getUsedFor()));
        item.put("sourceType", sourceType(record));
        item.put("fallbackUsed", record.isFallbackUsed());
        item.put("cacheHit", record.isCacheHit());
        item.put("scoreContribution", record.getScoreContribution() == null ? 0 : record.getScoreContribution());
        item.put("confidence", record.getConfidence() == null ? 0.0 : record.getConfidence());
        item.put("metrics", normalizedPayload.getOrDefault("metrics", Map.of()));
        item.put("metricSummary", publicDataMetricSummary(record, normalizedPayload));
        item.put("fetchedAt", record.getFetchedAt() != null ? record.getFetchedAt().toString() : "");
        item.put("publishedAt", stringValue(normalizedPayload.get("publishedAt")));
        item.put("sourcePeriodStart", stringValue(normalizedPayload.get("sourcePeriodStart")));
        item.put("sourcePeriodEnd", stringValue(normalizedPayload.get("sourcePeriodEnd")));
        return item;
    }

    private Map<String, Object> buildNormalizedMetrics(List<PublicDataRecord> records) {
        Map<String, Map<String, Object>> buckets = new LinkedHashMap<>();
        for (PublicDataRecord record : records) {
            String usedFor = nullToEmpty(record.getUsedFor()).isBlank() ? "unknown" : record.getUsedFor();
            Map<String, Object> bucket = buckets.computeIfAbsent(usedFor, ignored -> new LinkedHashMap<>(Map.of(
                    "recordCount", 0,
                    "liveOrCacheCount", 0,
                    "fallbackCount", 0,
                    "scoreContributionSum", 0,
                    "averageConfidence", 0.0
            )));
            int count = ((Number) bucket.get("recordCount")).intValue() + 1;
            int contribution = record.getScoreContribution() == null ? 0 : record.getScoreContribution();
            double confidence = record.getConfidence() == null ? 0.0 : record.getConfidence();
            bucket.put("recordCount", count);
            bucket.put("scoreContributionSum", ((Number) bucket.get("scoreContributionSum")).intValue() + contribution);
            bucket.put("averageConfidence", ((Number) bucket.get("averageConfidence")).doubleValue() + confidence);
            if ("live".equals(sourceType(record)) || "cache".equals(sourceType(record)) || "crawl".equals(sourceType(record))) {
                bucket.put("liveOrCacheCount", ((Number) bucket.get("liveOrCacheCount")).intValue() + 1);
            }
            if (record.isFallbackUsed() || "fallback".equals(sourceType(record))) {
                bucket.put("fallbackCount", ((Number) bucket.get("fallbackCount")).intValue() + 1);
            }
        }
        buckets.values().forEach(bucket -> {
            int count = Math.max(1, ((Number) bucket.get("recordCount")).intValue());
            double average = ((Number) bucket.get("averageConfidence")).doubleValue() / count;
            bucket.put("averageConfidence", Math.round(average * 1000.0) / 1000.0);
        });
        return new LinkedHashMap<>(buckets);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMapOrDefault(Object value, Map<String, Object> fallback) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> objectListOrDefault(Object value, List<Map<String, Object>> fallback) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> {
                        Map<String, Object> result = new LinkedHashMap<>();
                        ((Map<?, ?>) item).forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
                        return result;
                    })
                    .toList();
        }
        return fallback;
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
                                                int fto, int support, List<Map<String, Object>> risks,
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
                                                           List<Map<String, Object>> risks) {
        String riskClass = recall >= 75 ? "l" : recall >= 60 ? "m" : "h";
        String badge = recall >= 75 ? "저위험" : recall >= 60 ? "중위험" : "고위험";
        int publicCount = sourceCount(sources, "recall", "live", "cache");
        String topRisk = risks.isEmpty() ? "출처 최신성 점검" : stringValue(risks.get(0).get("label"));
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
                                                                    List<Map<String, Object>> risks,
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
        String topRisk = risks.isEmpty() ? "출처 최신성 점검" : stringValue(risks.get(0).get("label"));
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
                "title", defaultString(item.get("title"), "workflow 근거 지식화 후보"),
                "evidence", defaultString(item.get("evidence"), ""),
                "rationale", defaultString(item.get("rationale"), defaultString(item.get("reasoning"), "")),
                "impact", defaultString(item.get("impact"), "다음 진단의 evidence context로 재사용")
        );
    }

    private void persistKnowledgeLoopSuggestions(DiagnosisRun run, Product product, List<Map<String, Object>> suggestions) {
        if (product == null || suggestions == null || suggestions.isEmpty()) return;
        for (Map<String, Object> suggestion : suggestions) {
            KnowledgeLoopItem item = KnowledgeLoopItem.builder()
                    .product(product)
                    .diagnosisId(run.getId())
                    .type(prefix(defaultString(suggestion.get("type"), "policy_note"), 60))
                    .title(prefix(defaultString(suggestion.get("title"), "workflow 근거 지식화 후보"), 200))
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
    private String extractReportMarkdown(Map<String, Object> reportResult, String diagnosisId,
                                         String productName, String hsCode,
                                         String targetCountry, int overall,
                                         List<Map<String, Object>> risks,
                                         Map<String, Object> roadmap,
                                         String publicDataProofRunId,
                                         Map<String, Object> sourceTrustSummary) {
        MarkdownReportRenderer renderer = new MarkdownReportRenderer(
                new ReportFrontMatterBuilder(),
                new ReportClaimVerifier()
        );
        String officialMarkdown = renderer.render(MarkdownReportRenderer.ReportContext.builder()
                .diagnosisId(diagnosisId)
                .productName(productName)
                .hsCode(hsCode)
                .targetCountry(targetCountry)
                .overallScore(overall)
                .publicDataProofRunId(publicDataProofRunId)
                .risks(risks)
                .roadmap(roadmap)
                .sourceTrustSummary(sourceTrustSummary)
                .build());
        if (reportResult != null && reportResult.get("markdown") instanceof String markdown && !markdown.isBlank()) {
            if (markdown.contains("publicDataProofRunId:") && markdown.contains("## Source Appendix")) {
                return markdown;
            }
            return officialMarkdown
                    + "\n## Optional Report Bridge Draft\n\n"
                    + markdown;
        }
        return officialMarkdown;
    }

    private String actionTitles(Map<String, Object> roadmap, String key) {
        Object value = roadmap.get(key);
        if (!(value instanceof List<?> list) || list.isEmpty()) return "담당자 확인 항목 없음";
        return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> stringValue(item.get("title")))
                .filter(title -> !title.isBlank())
                .limit(3)
                .reduce((left, right) -> left + " / " + right)
                .orElse("담당자 확인 항목 없음");
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
                             List<Map<String, Object>> sources,
                             Map<String, Object> regionalIndustryProfile) {
        int score = 58;
        int contribution = 0;
        if (knowledge != null && knowledge.getTrustGrade() == TrustGrade.high) contribution += 12;
        else if (knowledge != null && knowledge.getTrustGrade() == TrustGrade.medium) contribution += 6;
        else contribution -= 6;
        contribution += Math.min(12, buyers.size() * 6);
        contribution += Math.min(10, knowledgeSources.size() * 3);
        contribution += regionalIndustryProfile.get("scoreContribution") instanceof Number number
                ? number.intValue() : 0;

        sources.add(source("support", "RULE-SUPPORT-" + targetCountry,
                "RULE_ENGINE", "로컬 지식·바이어·출처 기반 지원 적합도 휴리스틱", "",
                "country=" + targetCountry + ";buyerCount=" + buyers.size(), "support", contribution,
                knowledgeSources.isEmpty(), "지식 출처 수, 바이어 요구사항, 신뢰 등급, 지역 산업 매칭을 실행 가능성에 반영"));
        return clamp(score + contribution);
    }

    private Map<String, Object> buildRegionalIndustryProfile(DiagnosisRun run, Map<String, Object> inputData,
                                                             String hsCode, String productName) {
        String region = firstNonBlank(stringValue(inputData.get("region")), "지역 미입력");
        String industry = firstNonBlank(stringValue(inputData.get("industry")),
                run.getCompany() != null ? nullToEmpty(run.getCompany().getIndustry()) : "제조");
        return regionalIndustryProfileService.resolve(region, industry, hsCode, productName);
    }

    private void addRegionalIndustrySource(List<Map<String, Object>> sources, Map<String, Object> profile) {
        int contribution = profile.get("scoreContribution") instanceof Number number ? number.intValue() : 0;
        Map<String, Object> source = source("local_industry",
                "RULE-LOCAL-INDUSTRY-" + normalizeRegionKey(stringValue(profile.get("region"))),
                "RULE_ENGINE", "지역 주력산업·지원기관 매칭 규칙", "",
                "region=" + stringValue(profile.get("region")) + ";industry=" + stringValue(profile.get("industry")),
                "support", contribution, contribution <= 0,
                "지역, 업종, HS Code 기반으로 지역 주력산업과 지원기관 후보를 매칭");
        source.put("metrics", Map.of(
                "region", profile.get("region"),
                "industry", profile.get("industry"),
                "cluster", profile.get("cluster"),
                "leadingIndustry", profile.get("leadingIndustry"),
                "supportOrgCount", profile.get("supportOrgs") instanceof List<?> list ? list.size() : 0
        ));
        source.put("metricSummary", stringValue(profile.get("region")) + " · " + stringValue(profile.get("leadingIndustry"))
                + " · contribution " + contribution);
        source.put("decisionImpact", "지역 산업 매칭이 support Gate에 "
                + (contribution > 0 ? "+" : "") + contribution + "점 반영됩니다.");
        sources.add(source);
    }

    private Map<String, Object> buildBuyerRequestTrace(Map<String, Object> buyerContext) {
        String buyerRequest = stringValue(buyerContext.get("buyerRequest"));
        String joined = (buyerRequest + " "
                + String.join(" ", stringList(buyerContext.get("requestedCertifications"))) + " "
                + String.join(" ", stringList(buyerContext.get("buyerRequirementHints")))).toLowerCase();
        List<Map<String, Object>> impacts = new ArrayList<>();
        if (containsAny(joined, "ce", "rohs", "reach", "iec", "un38.3", "인증", "시험성적")) {
            impacts.add(Map.of("dimension", "regulation", "keyword", "certification", "impact", "요구 인증을 규제 보완 과제로 반영"));
        }
        if (containsAny(joined, "recall", "리콜", "safety", "안전", "화재", "과열")) {
            impacts.add(Map.of("dimension", "recall", "keyword", "safety", "impact", "안전/리콜 키워드를 위해 위험 확인 과제로 반영"));
        }
        if (containsAny(joined, "fto", "patent", "특허", "침해", "회피")) {
            impacts.add(Map.of("dimension", "fto", "keyword", "patent", "impact", "특허/FTO 키워드를 견적 전 검토 과제로 반영"));
        }
        if (containsAny(joined, "sample", "샘플", "납기", "주", "deadline", "견적", "budget", "예산")) {
            impacts.add(Map.of("dimension", "support", "keyword", "execution", "impact", "회신/샘플/예산 조건을 실행 보드 우선순위에 반영"));
        }
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("buyerRequestPresent", !buyerRequest.isBlank());
        trace.put("impactCount", impacts.size());
        trace.put("impacts", impacts);
        trace.put("sourceIds", impacts.isEmpty() ? List.of("USER_INPUT") : List.of("USER-BUYER-REQUEST", "RULE-REGULATION", "RULE-FTO", "RULE-RECALL"));
        trace.put("summary", impacts.isEmpty()
                ? "바이어 문의 키워드가 없어 일반 Gate 판단으로 처리"
                : "바이어 문의 키워드가 " + impacts.size() + "개 판단 축에 연결됨");
        return trace;
    }

    private void addBuyerRequestTraceSource(List<Map<String, Object>> sources, Map<String, Object> trace) {
        if (!Boolean.TRUE.equals(trace.get("buyerRequestPresent"))) return;
        int impactCount = trace.get("impactCount") instanceof Number number ? number.intValue() : 0;
        Map<String, Object> source = source("buyer_request", "USER-BUYER-REQUEST", "USER_INPUT",
                "바이어 문의 기반 판단 trace", "", "buyerRequest=provided", "support",
                0, impactCount == 0, stringValue(trace.get("summary")));
        source.put("metrics", Map.of("impactCount", impactCount, "impacts", trace.getOrDefault("impacts", List.of())));
        source.put("metricSummary", stringValue(trace.get("summary")));
        source.put("decisionImpact", "바이어 문의는 점수 자체보다 risk/action roadmap의 우선순위를 조정합니다.");
        sources.add(source);
    }

    private List<Map<String, Object>> buildRiskItems(List<Certification> certs, List<Fto> ftoItems,
                                                     ProductKnowledge knowledge, List<BuyerRequirement> buyers,
                                                     String hsCode, Map<String, Object> buyerContext,
                                                     List<Map<String, Object>> sources) {
        List<Map<String, Object>> risks = new ArrayList<>();
        LocalDate today = LocalDate.now();
        boolean hasExpired = certs.stream().anyMatch(cert ->
                cert.getExpiresAt() != null && cert.getExpiresAt().isBefore(today));
        boolean hasExpiring = certs.stream().anyMatch(cert ->
                cert.getExpiresAt() != null && !cert.getExpiresAt().isBefore(today)
                        && ChronoUnit.DAYS.between(today, cert.getExpiresAt()) <= 90);
        boolean hasHighFto = ftoItems.stream().anyMatch(fto -> fto.getRiskLevel() == RiskLevel.high);

        String deadline = defaultString(buyerContext.get("buyerDeadline"), LocalDate.now().plusDays(7).toString());
        if (hasHighFto) risks.add(risk("R-FTO-001", "FTO", "high", "대상국 FTO 고위험 항목 보완 필요",
                "HIGH", "MEDIUM", "특허 회피 검토 전 견적 확정 시 법적 분쟁 가능", "FTO 검토 메모",
                "대표/외부 변리사", deadline, "BMS·방열·구조 키워드로 FTO 1차 검색", List.of("RULE-FTO")));
        if (hasExpired) risks.add(risk("R-CERT-001", "CERTIFICATION", "high", "만료된 인증서가 있어 규제 제출 전 갱신 필요",
                "HIGH", "HIGH", "샘플 발송 또는 통관 서류 제출 전 반려 가능", "갱신된 인증서 PDF",
                "품질 담당자", deadline, "만료 인증서 갱신 일정과 시험기관 견적 확인", List.of("RULE-REGULATION")));
        if (hasExpiring) risks.add(risk("R-CERT-002", "CERTIFICATION", "medium", "90일 이내 만료 예정 인증서 확인 필요",
                "MEDIUM", "MEDIUM", "견적 이후 납기 확정 단계에서 인증 갱신 지연 가능", "인증 만료일 검토표",
                "품질 담당자", deadline, "만료 예정 인증서별 갱신 필요 여부 확인", List.of("RULE-REGULATION")));
        if (ftoItems.isEmpty()) risks.add(risk("R-FTO-002", "FTO", "medium", "FTO 검토 근거가 등록되지 않음",
                "MEDIUM", "MEDIUM", "특허 위험을 확인하지 못해 확정 견적 전 전문가 확인 필요", "FTO 1차 검색 결과",
                "대표/외부 변리사", deadline, "KIPRIS 키워드와 경쟁사명을 기준으로 1차 검색", List.of("RULE-FTO", "KIPRIS_FTO")));
        if (buyers.isEmpty() && stringValue(buyerContext.get("buyerRequest")).isBlank()) risks.add(risk("R-BUYER-001", "BUYER_REQUIREMENT", "medium", "바이어 문의·요구사항이 없어 판단 신뢰도 낮음",
                "MEDIUM", "HIGH", "요구 인증과 회신 마감이 없으면 실행 우선순위가 일반화됨", "바이어 문의 원문",
                "해외영업", deadline, "바이어 요구 인증, 견적 마감일, 샘플 요청 여부 입력", List.of("USER_INPUT")));
        if (knowledge == null || knowledge.getTrustGrade() == null) {
            risks.add(risk("R-KNOWLEDGE-001", "PRODUCT_KNOWLEDGE", "medium", "제품 지식 신뢰 등급이 미등록 상태",
                    "MEDIUM", "MEDIUM", "제품 설명과 보유 문서가 부족하면 공공데이터 매칭 근거가 약해짐", "제품 설명서/BOM",
                    "제품 담당자", deadline, "제품 설명서와 주요 인증/시험성적서 지식화", List.of("USER_INPUT")));
        }
        if (startsWithAny(hsCode, "8507")) {
            risks.add(risk("R-SAFETY-001", "SAFETY_RECALL", "medium", "배터리 품목군은 운송·안전 인증 증빙 강화 필요",
                    "MEDIUM", "MEDIUM", "UN38.3 등 운송시험 미확인 시 샘플 발송 전 3주 이상 지연 가능", "UN38.3 시험성적서",
                    "품질 담당자", deadline, "UN38.3/IEC62133 보유 여부와 시험기관 견적 확인", List.of("SAFETY_RECALL", "RULE-RECALL")));
        }
        long fallbackCount = sources.stream().filter(source -> "fallback".equals(source.get("sourceType"))).count();
        if (fallbackCount > 0) {
            risks.add(risk("R-SOURCE-001", "SOURCE_CONFIDENCE", "medium", "일부 공공데이터가 fallback 상태라 담당자 확인 필요",
                    "MEDIUM", "MEDIUM", "live 근거 부족 시 판단 신뢰도가 낮아져 전문가 확인이 필요", "공식 API endpoint/key 또는 담당자 보유 근거",
                    "데이터 담당자", deadline, "fallback provider의 API key/operation endpoint 또는 보유 문서 보강", List.of("PUBLIC_DATA")));
        }
        if (risks.isEmpty()) risks.add(risk("R-LOW-001", "SOURCE_CONFIDENCE", "low", "치명 위험은 낮으나 출처 최신성 점검 필요",
                "LOW", "LOW", "최종 견적 전 source trace 최신성만 확인하면 진행 가능", "최신 공공데이터 source trace",
                "해외영업", deadline, "견적 전 source trace와 바이어 요구사항 최종 확인", List.of("PUBLIC_DATA", "RULE_ENGINE")));
        return risks;
    }

    private Map<String, Object> buildBuyerContext(Map<String, Object> inputData, List<String> buyerRequirements) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("buyerRequest", stringValue(inputData.get("buyerRequest")));
        context.put("buyerDeadline", stringValue(inputData.get("buyerDeadline")));
        context.put("expectedShipmentDate", stringValue(inputData.get("expectedShipmentDate")));
        context.put("region", stringValue(inputData.get("region")));
        context.put("industry", stringValue(inputData.get("industry")));
        context.put("requestedCertifications", stringList(inputData.get("requestedCertifications")));
        context.put("currentDocuments", stringList(inputData.get("currentDocuments")));
        context.put("exportExperience", stringValue(inputData.get("exportExperience")));
        context.put("budgetLimitKrw", inputData.get("budgetLimitKrw") instanceof Number number ? number.longValue() : 0L);
        context.put("buyerRequirementHints", buyerRequirements);
        return context;
    }

    private List<String> buyersFromInput(Map<String, Object> inputData) {
        List<String> values = new ArrayList<>();
        String buyerRequest = stringValue(inputData.get("buyerRequest"));
        if (!buyerRequest.isBlank()) values.add(buyerRequest);
        values.addAll(stringList(inputData.get("requestedCertifications")));
        return values;
    }

    private Map<String, Object> buildGateDecision(int overall, int market, int regulation, int recall, int fto,
                                                  int support, List<Map<String, Object>> risks,
                                                  Map<String, Object> confidencePanel,
                                                  Map<String, Object> buyerContext) {
        long highRiskCount = risks.stream()
                .filter(risk -> "high".equals(risk.get("level")) || "HIGH".equals(risk.get("severity")))
                .count();
        int fallbackCount = confidencePanel.get("fallbackSourceCount") instanceof Number number ? number.intValue() : 0;
        String status;
        if (fto < 45 || regulation < 40 || highRiskCount >= 2) status = "STOP";
        else if (regulation < 55 || recall < 55 || fallbackCount >= 3) status = "HOLD";
        else if (overall >= 75 && highRiskCount == 0 && fallbackCount <= 1) status = "GO";
        else status = "CONDITIONAL_GO";

        String label = switch (status) {
            case "GO" -> "진행 가능";
            case "HOLD" -> "보류";
            case "STOP" -> "중단 후 보완";
            default -> "조건부 진행";
        };
        String topRisk = risks.isEmpty() ? "출처 최신성 점검" : stringValue(risks.get(0).get("label"));
        List<String> mustFix = risks.stream()
                .filter(risk -> !"low".equals(risk.get("level")))
                .map(risk -> stringValue(risk.get("nextAction")))
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(5)
                .toList();
        String buyerRequest = stringValue(buyerContext.get("buyerRequest"));
        String reply = buyerRequest.isBlank()
                ? "바이어 요구사항을 먼저 확인한 뒤 인증/FTO 보완 상태를 기준으로 견적 가능 여부를 회신하십시오."
                : "현재 제품 공급 가능성을 검토 중이며, 인증·FTO·출하 가능 일정 확인 후 견적과 납기를 확정하겠다고 회신하십시오.";

        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("status", status);
        decision.put("finalGate", status);
        decision.put("ruleScore", overall);
        decision.put("riskLevel", highRiskCount >= 2 || fto < 45 || regulation < 40 ? "high"
                : highRiskCount == 1 || fallbackCount > 0 || regulation < 55 || recall < 55 ? "medium" : "low");
        decision.put("fallbackCount", fallbackCount);
        decision.put("reasons", List.of(
                "ruleScore=" + overall,
                "highRiskCount=" + highRiskCount,
                "fallbackCount=" + fallbackCount
        ));
        decision.put("llmInfluencedGate", false);
        decision.put("llmDecisionPolicy", "LLM output is advisory and cannot override deterministic Gate");
        decision.put("label", label);
        decision.put("oneLineReason", topRisk + " 때문에 견적 확정 전 Gate 확인이 필요합니다.");
        decision.put("allowedNextStep", status.equals("STOP") ? "보완 근거 확보 후 재진단" : "바이어에게 검토 중 회신 및 필수 증빙 확인");
        decision.put("blockedStep", status.equals("GO") ? "없음" : "가격·납기 확정 견적 발송");
        decision.put("buyerReplyGuidance", reply);
        decision.put("mustFixBeforeQuote", mustFix.isEmpty() ? List.of("공공데이터 source trace 최신성 확인") : mustFix);
        return decision;
    }

    private Map<String, Object> buildImpactEstimate(List<Map<String, Object>> risks,
                                                    Map<String, Object> actionRoadmap,
                                                    Map<String, Object> buyerContext,
                                                    int supportScore) {
        long high = risks.stream().filter(risk -> "high".equals(risk.get("level"))).count();
        long medium = risks.stream().filter(risk -> "medium".equals(risk.get("level"))).count();
        int actionCount = actionSize(actionRoadmap, "thisWeek") + actionSize(actionRoadmap, "beforeQuote");
        int delayRiskDays = (int) Math.max(7, high * 21 + medium * 7);
        long budget = buyerContext.get("budgetLimitKrw") instanceof Number number ? number.longValue() : 0L;
        long avoidableCost = Math.max(1_500_000L, high * 2_000_000L + medium * 700_000L + budget / 4);
        long supportOpportunity = supportScore >= 65 ? 10_000_000L : 3_000_000L;
        return Map.of(
                "timeSavedHours", Math.max(3, actionCount * 2),
                "delayRiskDays", delayRiskDays,
                "avoidableCostKrw", avoidableCost,
                "supportFundingOpportunityKrw", supportOpportunity,
                "explanation", "인증/FTO/리콜 red flag를 견적 전 확인해 자료 탐색 시간과 샘플·통관 지연 가능성을 줄이는 규칙 기반 추정입니다."
        );
    }

    private Map<String, Object> buildConfidencePanel(List<Map<String, Object>> sources) {
        int live = sourceTypeCount(sources, "live");
        int cache = sourceTypeCount(sources, "cache");
        int fallback = sourceTypeCount(sources, "fallback");
        List<String> missing = new ArrayList<>();
        if (fallback > 0) missing.add("일부 공공데이터 provider가 fallback 상태입니다.");
        if (live + cache < 2) missing.add("live/cache 공공데이터 근거가 2개 미만입니다.");
        boolean humanReview = fallback > 0 || live + cache < 2;
        String level = humanReview ? fallback >= 3 ? "LOW" : "MEDIUM" : "HIGH";
        return Map.of(
                "level", level,
                "liveSourceCount", live,
                "cacheSourceCount", cache,
                "fallbackSourceCount", fallback,
                "missingEvidence", missing,
                "humanReviewRequired", humanReview,
                "reviewReason", humanReview
                        ? "fallback 또는 근거 부족 항목은 담당자/전문가 확인 후 견적 확정이 필요합니다."
                        : "live/cache 근거가 충분해 1차 Gate 판단 신뢰도가 높습니다."
        );
    }

    private Map<String, Object> risk(String riskId, String category, String level, String label,
                                     String severity, String probability, String businessImpact,
                                     String requiredEvidence, String ownerRole, String deadline,
                                     String nextAction, List<String> sourceIds) {
        Map<String, Object> risk = new LinkedHashMap<>();
        risk.put("riskId", riskId);
        risk.put("category", category);
        risk.put("level", level);
        risk.put("label", label);
        risk.put("severity", severity);
        risk.put("probability", probability);
        risk.put("businessImpact", businessImpact);
        risk.put("requiredEvidence", requiredEvidence);
        risk.put("ownerRole", ownerRole);
        risk.put("deadline", deadline);
        risk.put("nextAction", nextAction);
        risk.put("sourceIds", sourceIds);
        return risk;
    }

    private List<Map<String, Object>> actionItems(List<Map<String, Object>> risks, String bucket, int limit) {
        return risks.stream()
                .filter(risk -> !"low".equals(risk.get("level")) || "beforeShipment".equals(bucket))
                .limit(limit)
                .map(risk -> Map.of(
                        "title", stringValue(risk.get("nextAction")),
                        "owner", stringValue(risk.get("ownerRole")),
                        "output", stringValue(risk.get("requiredEvidence")),
                        "dueDate", stringValue(risk.get("deadline")),
                        "reason", stringValue(risk.get("businessImpact")),
                        "blockedIfNotDone", "beforeQuote".equals(bucket) ? "견적 확정 불가" : "Gate 판단 신뢰도 하락",
                        "sourceIds", risk.getOrDefault("sourceIds", List.of())
                ))
                .toList();
    }

    private List<Map<String, Object>> beforeShipmentActions(Map<String, Object> buyerContext,
                                                            List<Map<String, Object>> risks) {
        String dueDate = defaultString(buyerContext.get("expectedShipmentDate"), LocalDate.now().plusDays(30).toString());
        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(Map.of(
                "title", "출하 전 인증·시험성적서 패키지 확인",
                "owner", "품질 담당자",
                "output", "바이어 제출 문서 ZIP",
                "dueDate", dueDate,
                "reason", "출하 직전 문서 누락으로 샘플 반려가 발생하지 않도록 확인",
                "blockedIfNotDone", "샘플/초도물량 발송 보류",
                "sourceIds", List.of("USER_INPUT", "RULE_ENGINE")
        ));
        if (risks.stream().anyMatch(risk -> "SOURCE_CONFIDENCE".equals(risk.get("category")))) {
            actions.add(Map.of(
                    "title", "fallback 공공데이터 provider 재수집",
                    "owner", "데이터 담당자",
                    "output", "live/cache source trace",
                    "dueDate", dueDate,
                    "reason", "출하 전 최신 official source로 판단 근거 보강",
                    "blockedIfNotDone", "전문가 확인 필요",
                    "sourceIds", List.of("PUBLIC_DATA")
            ));
        }
        return actions;
    }

    private int sourceTypeCount(List<Map<String, Object>> sources, String sourceType) {
        return (int) sources.stream().filter(source -> sourceType.equals(source.get("sourceType"))).count();
    }

    private int actionSize(Map<String, Object> roadmap, String key) {
        Object value = roadmap.get(key);
        return value instanceof List<?> list ? list.size() : 0;
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().map(this::stringValue).filter(item -> !item.isBlank()).toList();
    }

    private List<String> validSourceIds(List<Map<String, Object>> sources) {
        return sources.stream()
                .map(source -> stringValue(source.get("sourceId")))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private List<Map<String, Object>> extractLlmClaims(AgentInferenceResult llmResult) {
        if (llmResult == null || llmResult.getContent() == null) return List.of();
        return objectListOrDefault(llmResult.getContent().get("claims"), List.of());
    }

    private List<String> combineFallbackReasons(AgentInferenceResult llmResult,
                                                Map<String, Object> claimVerificationResult) {
        List<String> reasons = new ArrayList<>();
        if (llmResult != null && llmResult.getFallbackReasons() != null) {
            reasons.addAll(llmResult.getFallbackReasons());
        }
        Object claimReasons = claimVerificationResult.get("fallbackReasons");
        if (claimReasons instanceof List<?> list) {
            list.stream()
                    .map(this::stringValue)
                    .filter(value -> !value.isBlank())
                    .forEach(reasons::add);
        }
        return reasons.stream().distinct().toList();
    }

    private Map<String, Object> buildSourceTrace(List<Map<String, Object>> sources) {
        Map<String, Long> countsByType = sources.stream()
                .collect(Collectors.groupingBy(
                        source -> defaultString(source.get("sourceType"), "unknown"),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        List<Map<String, Object>> entries = sources.stream()
                .filter(source -> !stringValue(source.get("sourceId")).isBlank())
                .map(source -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("sourceId", source.get("sourceId"));
                    entry.put("provider", source.get("provider"));
                    entry.put("sourceType", source.get("sourceType"));
                    entry.put("usedFor", source.get("usedFor"));
                    entry.put("metricSummary", source.get("metricSummary"));
                    entry.put("decisionImpact", source.get("decisionImpact"));
                    entry.put("fallbackUsed", source.getOrDefault("fallbackUsed", false));
                    return entry;
                })
                .limit(30)
                .toList();
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("sourceCount", sources.size());
        trace.put("countsByType", countsByType);
        trace.put("sourceIds", validSourceIds(sources));
        trace.put("entries", entries);
        trace.put("llmClaimBoundary", "LLM report claims must reference one of these sourceIds; source-less claims are blocked or surfaced as fallback reasons.");
        return trace;
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

    private List<Map<String, Object>> buildExportKnowledgeTimeline(List<Map<String, Object>> sources) {
        return sources.stream()
                .filter(source -> !stringValue(source.get("sourceId")).isBlank())
                .map(source -> {
                    Map<String, Object> event = new LinkedHashMap<>();
                    String sourceType = stringValue(source.get("sourceType"));
                    String usedFor = stringValue(source.get("usedFor"));
                    event.put("eventId", "EVT-" + stringValue(source.get("sourceId")));
                    event.put("eventType", timelineEventType(sourceType, usedFor, stringValue(source.get("provider"))));
                    event.put("provider", stringValue(source.get("provider")));
                    event.put("sourceId", stringValue(source.get("sourceId")));
                    event.put("sourceType", sourceType);
                    event.put("usedFor", usedFor);
                    event.put("title", stringValue(source.get("datasetName")));
                    event.put("fetchedAt", stringValue(source.get("retrievedAt")));
                    event.put("publishedAt", stringValue(source.get("publishedAt")));
                    event.put("sourcePeriodStart", stringValue(source.get("sourcePeriodStart")));
                    event.put("sourcePeriodEnd", stringValue(source.get("sourcePeriodEnd")));
                    event.put("periodLabel", timelinePeriodLabel(source));
                    event.put("summary", stringValue(source.get("metricSummary")).isBlank()
                            ? stringValue(source.get("reason"))
                            : stringValue(source.get("metricSummary")));
                    event.put("scoreImpact", source.getOrDefault("scoreContribution", 0));
                    event.put("confidence", source.getOrDefault("confidence", sourceConfidenceFromType(sourceType)));
                    event.put("status", timelineStatus(sourceType));
                    return event;
                })
                .limit(30)
                .toList();
    }

    private Map<String, Object> buildEvidenceGuardrails(List<Map<String, Object>> sources,
                                                        List<PublicDataRecord> records) {
        long unsupportedClaims = sources.stream()
                .filter(source -> stringValue(source.get("sourceId")).isBlank())
                .count();
        long fallbackPositive = sources.stream()
                .filter(source -> "fallback".equals(source.get("sourceType")))
                .filter(source -> source.get("scoreContribution") instanceof Number number && number.intValue() > 0)
                .count();
        long officialLiveOrCache = records.stream()
                .filter(record -> !record.isFallbackUsed())
                .filter(record -> "live".equals(sourceType(record)) || "cache".equals(sourceType(record)))
                .count();
        Map<String, Object> guardrails = new LinkedHashMap<>();
        guardrails.put("unsupportedClaimCount", unsupportedClaims);
        guardrails.put("fallbackPositiveContributionCount", fallbackPositive);
        guardrails.put("officialLiveOrCacheRecordCount", officialLiveOrCache);
        guardrails.put("policies", List.of(
                "fallback source는 점수를 올리지 않고 confidence와 담당자 확인 항목에만 반영",
                "화면 claim은 sourceId가 있는 evidence timeline 또는 rule source에 연결",
                "publishedAt/fetchedAt/sourcePeriod를 분리해 최신성과 데이터 기준 기간을 혼동하지 않음",
                "대규모 벡터 검색이나 자동 의사결정이 아니라 source-bound evidence workflow로 표시"
        ));
        guardrails.put("verdict", unsupportedClaims == 0 && fallbackPositive == 0 ? "pass" : "review_required");
        return guardrails;
    }

    private String timelineEventType(String sourceType, String usedFor, String provider) {
        if ("fallback".equals(sourceType)) return "SOURCE_FALLBACK_RECORDED";
        if ("heuristic".equals(sourceType)) return "RULE_HEURISTIC_APPLIED";
        if ("agent".equals(sourceType)) return "WORKFLOW_TRACE_RECORDED";
        if ("market".equals(usedFor)) return "EXPORT_METRIC_SIGNAL";
        if ("recall".equals(usedFor)) return "SAFETY_RECALL_SIGNAL";
        if ("fto".equals(usedFor)) return "FTO_SEARCH_SIGNAL";
        if ("support".equals(usedFor)) return "SUPPORT_PROGRAM_SIGNAL";
        if (provider.contains("KOTRA")) return "MARKET_DEMAND_SIGNAL";
        return "EVIDENCE_SOURCE_RECORDED";
    }

    private String timelinePeriodLabel(Map<String, Object> source) {
        String start = stringValue(source.get("sourcePeriodStart"));
        String end = stringValue(source.get("sourcePeriodEnd"));
        if (!start.isBlank() && !end.isBlank()) return start + " ~ " + end;
        String fetchedAt = stringValue(source.get("retrievedAt"));
        return fetchedAt.isBlank() ? "기준일 미확인" : "fetched " + fetchedAt.substring(0, Math.min(10, fetchedAt.length()));
    }

    private String timelineStatus(String sourceType) {
        if ("live".equals(sourceType) || "cache".equals(sourceType)) return "evidence_bound";
        if ("external_feed".equals(sourceType) || "crawl".equals(sourceType)) return "context_bound";
        if ("heuristic".equals(sourceType)) return "rule_bound";
        if ("fallback".equals(sourceType)) return "needs_live_evidence";
        return "trace_only";
    }

    private double sourceConfidenceFromType(String sourceType) {
        if ("live".equals(sourceType)) return 0.85;
        if ("cache".equals(sourceType)) return 0.70;
        if ("external_feed".equals(sourceType)) return 0.55;
        if ("crawl".equals(sourceType)) return 0.50;
        if ("heuristic".equals(sourceType)) return 0.45;
        if ("fallback".equals(sourceType)) return 0.20;
        return 0.50;
    }

    private Map<String, Object> buildActionPlan(List<Map<String, Object>> risks, List<Certification> certs,
                                             List<Fto> ftoItems, List<BuyerRequirement> buyers,
                                             List<KnowledgeSource> sources, Map<String, Object> buyerContext) {
        int high = (int) risks.stream().filter(risk -> "high".equals(risk.get("level"))).count();
        int medium = (int) risks.stream().filter(risk -> "medium".equals(risk.get("level"))).count();
        return Map.of(
                "d30", Math.max(2, high + (certs.isEmpty() ? 1 : 0) + (ftoItems.isEmpty() ? 1 : 0)),
                "d60", Math.max(2, medium + (buyers.isEmpty() ? 1 : 0)),
                "d90", Math.max(2, sources.isEmpty() ? 4 : 2),
                "thisWeek", actionItems(risks, "thisWeek", 3),
                "beforeQuote", actionItems(risks, "beforeQuote", 3),
                "beforeShipment", beforeShipmentActions(buyerContext, risks)
        );
    }

    private String buildSummary(String productName, String hsCode, String targetCountry, int overall,
                                List<Map<String, Object>> risks) {
        String topRisk = risks.isEmpty() ? "출처 최신성 점검" : stringValue(risks.get(0).get("label"));
        return String.format("%s(HS %s)의 %s 수출 Gate 판단 점수는 %d점입니다. 주요 보완 항목은 %s입니다.",
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
        if (record.isFallbackUsed()) return "fallback";
        if (record.isCacheHit()) return "cache";
        if (record.getSourceType() != null && !record.getSourceType().isBlank()) return record.getSourceType();
        return "live";
    }

    private String sourceType(String provider, String field) {
        if ("RULE_ENGINE".equals(provider)) return "heuristic";
        if ("USER_INPUT".equals(provider)) return "user_input";
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

    private boolean containsAny(String value, String... needles) {
        String safe = nullToEmpty(value).toLowerCase();
        for (String needle : needles) {
            if (!nullToEmpty(needle).isBlank() && safe.contains(needle.toLowerCase())) return true;
        }
        return false;
    }

    private String firstNonBlank(String first, String fallback) {
        return nullToEmpty(first).isBlank() ? nullToEmpty(fallback) : first;
    }

    private String normalizeRegionKey(String value) {
        return nullToEmpty(value).trim().toLowerCase().replaceAll("\\s+", "");
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
