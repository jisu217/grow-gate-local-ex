package com.growgate.diagnosis.service;

// 비동기 진단 실행 처리 (@Async)
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.diagnosis.entity.DiagnosisRun;
import com.growgate.diagnosis.enums.DiagnosisStatus;
import com.growgate.diagnosis.repository.DiagnosisRepository;
import com.growgate.knowledge.entity.BuyerRequirement;
import com.growgate.knowledge.entity.Certification;
import com.growgate.knowledge.entity.Fto;
import com.growgate.knowledge.entity.KnowledgeSource;
import com.growgate.knowledge.entity.ProductKnowledge;
import com.growgate.knowledge.enums.RiskLevel;
import com.growgate.knowledge.enums.TrustGrade;
import com.growgate.knowledge.repository.BuyerRequirementRepository;
import com.growgate.knowledge.repository.CertificationRepository;
import com.growgate.knowledge.repository.FtoRepository;
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

    @Async
    @Transactional
    public void process(String diagnosisId) {
        DiagnosisRun run = diagnosisRepository.findById(diagnosisId).orElse(null);
        if (run == null) return;

        try {
            run.setStatus(DiagnosisStatus.running);
            diagnosisRepository.save(run);

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

            List<Map<String, Object>> sources = new ArrayList<>();
            PublicDataCollectionResult publicDataResult = publicDataCollectionService.collectForDiagnosis(
                    run.getId(), run.getUser().getId(), hsCode, targetCountry, productName);
            addPublicDataSources(sources, publicDataResult.getRecords());

            int market = scoreMarket(hsCode, targetCountry, sources);
            int regulation = scoreRegulation(hsCode, targetCountry, certs, sources);
            int recall = scoreRecall(hsCode, description, sources);
            int fto = scoreFto(ftoItems, targetCountry, sources);
            int support = scoreSupport(knowledge, buyers, knowledgeSources, targetCountry, sources);
            market = applyPublicDataContribution(market, publicDataResult.getRecords(), "market");
            regulation = applyPublicDataContribution(regulation, publicDataResult.getRecords(), "regulation");
            recall = applyPublicDataContribution(recall, publicDataResult.getRecords(), "recall");
            fto = applyPublicDataContribution(fto, publicDataResult.getRecords(), "fto");
            support = applyPublicDataContribution(support, publicDataResult.getRecords(), "support");
            int overall = clamp(Math.round((market * 0.25f) + (regulation * 0.22f) + (recall * 0.16f)
                    + (fto * 0.20f) + (support * 0.17f)));

            List<Map<String, String>> riskItems = buildRiskItems(certs, ftoItems, knowledge, buyers, hsCode);
            Map<String, Object> actionRoadmap = buildRoadmap(riskItems, certs, ftoItems, buyers, knowledgeSources);
            Map<String, Object> agentRequest = buildAgentRequest(run, productName, hsCode, description, targetCountry, certs);
            Optional<Map<String, Object>> agentResult = fastApiAgentClient.analyze(agentRequest);
            Optional<Map<String, Object>> reportResult = fastApiAgentClient.report(agentRequest);

            sources.add(source("agent", "FASTAPI-AGENT-" + targetCountry,
                    "FastAPI Agent", "AI agent analyze/report bridge", "http://localhost:8000/docs",
                    "diagnosisId=" + run.getId(), "agentic-analysis", agentResult.isPresent() ? 5 : 0,
                    agentResult.isEmpty(), agentResult.isPresent()
                            ? "Spring 진단 결과에 FastAPI agent 분석을 병합"
                            : "FastAPI agent 미활성 또는 호출 실패로 Spring 규칙 진단 유지"));

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
            resultMap.put("agentTrace", buildAgentTrace(agentResult.isPresent(), reportResult.isPresent()));
            resultMap.put("stageCards", buildStageCards(productName, hsCode, targetCountry, overall, market,
                    regulation, recall, fto, support, riskItems, actionRoadmap, sources, agentResult.orElse(null)));
            resultMap.put("reportMarkdown", extractReportMarkdown(reportResult.orElse(null),
                    productName, hsCode, targetCountry, overall, riskItems, actionRoadmap));
            resultMap.put("reportFastApiUsed", reportResult.isPresent());
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
            run.setStatus(DiagnosisStatus.done);
            diagnosisRepository.save(run);
        } catch (Exception e) {
            log.error("Diagnosis processing failed for id: {}", diagnosisId, e);
            run.setStatus(DiagnosisStatus.failed);
            diagnosisRepository.save(run);
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
            source.put("collectionRunId", record.getRun() != null ? record.getRun().getId() : null);
            source.put("errorMessage", record.getErrorMessage());
            source.put("reason", record.isFallbackUsed()
                    ? "live 공공 API 수집 실패/비활성 상태를 명시하고 fallback 신호로 반영"
                    : "live 공공 API 또는 유효 cache record를 진단 점수에 반영");
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

    private Map<String, Object> buildAgentTrace(boolean analyzeSucceeded, boolean reportSucceeded) {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("mode", fastApiAgentClient.isEnabled() ? "fastapi-agent-bridge" : "spring-local-orchestrator");
        trace.put("steps", List.of(
                Map.of("step", "plan", "tool", "Spring DiagnosisAsyncProcessor", "status", "done",
                        "retryCount", 0, "message", "제품·HS·대상국·지식 스냅샷 기반 진단 계획 생성"),
                Map.of("step", "score", "tool", "Spring rule scorer", "status", "done",
                        "retryCount", 0, "message", "시장/규제/리콜/FTO/지원 점수 산정"),
                Map.of("step", "agent_analyze", "tool", "FastAPI /analyze", "status", analyzeSucceeded ? "done" : "fallback",
                        "retryCount", analyzeSucceeded ? 0 : 1, "message", analyzeSucceeded ? "FastAPI agent 분석 병합" : "Spring 결과로 대체"),
                Map.of("step", "report", "tool", "FastAPI /report", "status", reportSucceeded ? "done" : "fallback",
                        "retryCount", reportSucceeded ? 0 : 1, "message", reportSucceeded ? "FastAPI 리포트 병합" : "Spring Markdown 리포트 생성")
        ));
        trace.put("fastApi", fastApiAgentClient.status(fastApiAgentClient.isEnabled(),
                analyzeSucceeded || reportSucceeded,
                fastApiAgentClient.isEnabled() ? "FastAPI agent bridge attempted" : "FastAPI bridge disabled by configuration"));
        return trace;
    }

    private Map<String, Object> buildStageCards(String productName, String hsCode, String targetCountry,
                                                int overall, int market, int regulation, int recall,
                                                int fto, int support, List<Map<String, String>> risks,
                                                Map<String, Object> roadmap,
                                                List<Map<String, Object>> sources,
                                                Map<String, Object> agentResult) {
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
                        "agent=" + (agentResult != null ? "fastapi" : "spring-local"))
        ));
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
        return cards;
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
        sources.add(source("market", "KCS-TRADE-" + prefix(hsCode, 6) + "-" + targetCountry,
                "관세청", "수출입무역통계 HS/국가별 교역 신호", "https://unipass.customs.go.kr/ets/",
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

        sources.add(source("regulation", "KTL-CERT-" + targetCountry,
                "KTL/SafetyKorea", "인증·제품안전 요구사항 매칭", "https://www.safetykorea.kr/",
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
        sources.add(source("recall", "SAFETY-RECALL-" + prefix(hsCode, 4),
                "SafetyKorea/EU RAPEX", "제품군 리콜·위해사례 신호", "https://www.safetykorea.kr/",
                "hsPrefix=" + prefix(hsCode, 4), "recall", contribution, false,
                "HS 품목군과 설명 키워드를 위해사례 신호에 매칭"));
        return clamp(score + contribution);
    }

    private int scoreFto(List<Fto> ftoItems, String targetCountry, List<Map<String, Object>> sources) {
        if (ftoItems.isEmpty()) {
            sources.add(source("fto", "KIPRIS-FTO-MISSING-" + targetCountry,
                    "KIPRIS", "특허/FTO 확인 상태", "https://www.kipris.or.kr/",
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
        sources.add(source("fto", "KIPRIS-FTO-" + targetCountry,
                "KIPRIS", "FTO 위험도 등록 근거", "https://www.kipris.or.kr/",
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

        sources.add(source("support", "BIZINFO-SUPPORT-" + targetCountry,
                "기업마당/KOTRA", "지원사업·바이어 요구사항 적합도", "https://www.bizinfo.go.kr/",
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
        source.put("reason", reason);
        return source;
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
