package com.growgate.diagnosis.service;

// 진단 생성·조회
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.common.DateTimeUtils;
import com.growgate.diagnosis.dto.CreateDiagnosisRequest;
import com.growgate.diagnosis.dto.CreateDiagnosisResponse;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse.ActionItem;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse.ActionRoadmap;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse.CertSnapshot;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse.ConfidencePanel;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse.DiagnosisScore;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse.GateDecision;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse.ImpactEstimate;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse.KnowledgeSnapshot;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse.ResultData;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse.RiskItem;
import com.growgate.diagnosis.dto.DiagnosisListResponse;
import com.growgate.diagnosis.dto.DiagnosisListResponse.DiagnosisSummary;
import com.growgate.diagnosis.dto.DiagnosisReportResponse;
import com.growgate.diagnosis.dto.DiagnosisSourcesResponse;
import com.growgate.diagnosis.dto.DiagnosisSourcesResponse.SourceEntry;
import com.growgate.diagnosis.entity.DiagnosisRun;
import com.growgate.diagnosis.enums.DiagnosisStatus;
import com.growgate.diagnosis.repository.DiagnosisRepository;
import com.growgate.company.entity.Company;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.knowledge.entity.Certification;
import com.growgate.membership.entity.Membership;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.knowledge.entity.ProductKnowledge;
import com.growgate.knowledge.repository.BuyerRequirementRepository;
import com.growgate.knowledge.repository.CertificationRepository;
import com.growgate.knowledge.repository.FtoRepository;
import com.growgate.knowledge.repository.ProductKnowledgeRepository;
import com.growgate.product.entity.Product;
import com.growgate.product.repository.ProductRepository;
import com.growgate.user.entity.User;
import com.growgate.user.enums.UserRole;
import com.growgate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DiagnosisService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int EXPIRY_WARNING_DAYS = 90;

    private final DiagnosisRepository diagnosisRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ProductRepository productRepository;
    private final ProductKnowledgeRepository knowledgeRepository;
    private final CertificationRepository certificationRepository;
    private final FtoRepository ftoRepository;
    private final BuyerRequirementRepository buyerRequirementRepository;
    private final ObjectMapper objectMapper;
    private final DiagnosisAsyncProcessor asyncProcessor;

    @Transactional(readOnly = true)
    public DiagnosisListResponse getList(String userId) {
        User user = findUser(userId);
        List<DiagnosisRun> runs;

        Company company = resolveCompany(userId);
        if (company != null) {
            runs = diagnosisRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId());
        } else {
            runs = diagnosisRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        List<DiagnosisSummary> summaries = runs.stream().map(run -> {
            String productId = run.getProduct() != null ? run.getProduct().getId() : null;
            String productName = resolveProductName(run);
            String hsCode = resolveHsCode(run);

            return DiagnosisSummary.builder()
                    .id(run.getId())
                    .productId(productId)
                    .productName(productName)
                    .hsCode(hsCode)
                    .targetCountry(run.getTargetCountry())
                    .status(run.getStatus().name())
                    .createdAt(DateTimeUtils.formatUtc(run.getCreatedAt()))
                    .overallScore(extractOverallScore(run.getResult()))
                    .build();
        }).collect(Collectors.toList());

        return DiagnosisListResponse.builder().diagnoses(summaries).build();
    }

    public CreateDiagnosisResponse create(String userId, CreateDiagnosisRequest request) {
        if (request.getProductId() == null && request.getInputData() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "productId 또는 inputData 중 하나는 필수입니다.");
        }
        if (request.getProductId() == null) {
            validateDirectInput(request.getInputData());
        }

        User user = findUser(userId);
        requireDiagnosisPermission(user);

        Product product = null;
        String inputDataJson = null;
        String knowledgeSnapshotJson = null;

        if (request.getProductId() != null) {
            product = findProductByUser(user, request.getProductId());
            inputDataJson = serializeInputData(request.getInputData(), product);
            knowledgeSnapshotJson = buildKnowledgeSnapshot(product, request.getInputData(), request.getTargetCountry());
        } else {
            inputDataJson = serializeInputData(request.getInputData(), null);
            knowledgeSnapshotJson = buildInputSnapshot(request.getInputData(), request.getTargetCountry());
        }

        DiagnosisRun run = DiagnosisRun.builder()
                .user(user)
                .company(resolveCompany(userId))
                .product(product)
                .inputData(inputDataJson)
                .knowledgeSnapshot(knowledgeSnapshotJson)
                .status(DiagnosisStatus.pending)
                .targetCountry(request.getTargetCountry())
                .build();

        diagnosisRepository.save(run);

        dispatchAfterCommit(run.getId());

        return new CreateDiagnosisResponse(
                run.getId(),
                DiagnosisStatus.pending.name(),
                "진단이 요청되었습니다. 잠시 후 결과를 확인해주세요."
        );
    }

    @Transactional(readOnly = true)
    public DiagnosisDetailResponse getDetail(String userId, String diagnosisId) {
        User user = findUser(userId);
        DiagnosisRun run = findDiagnosisRun(user, diagnosisId);

        ResultData resultData = null;
        if (run.getResult() != null) {
            try {
                Map<String, Object> resultMap = objectMapper.readValue(run.getResult(),
                        new TypeReference<Map<String, Object>>() {});

                DiagnosisScore score = parseDiagnosisScore(resultMap);
                String summary = (String) resultMap.getOrDefault("summary", "");

                List<RiskItem> riskItems = parseRiskItems(resultMap);
                ActionRoadmap actionRoadmap = parseActionRoadmap(resultMap);
                List<String> evidenceSources = parseStringList(resultMap, "evidenceSources");

                resultData = ResultData.builder()
                        .diagnosisScore(score)
                        .gateDecision(parseGateDecision(resultMap))
                        .impactEstimate(parseImpactEstimate(resultMap))
                        .confidencePanel(parseConfidencePanel(resultMap))
                        .summary(summary)
                        .riskItems(riskItems)
                        .actionRoadmap(actionRoadmap)
                        .evidenceSources(evidenceSources)
                        .agentTrace(parseObjectMap(resultMap, "agentTrace"))
                        .evidenceRetrieval(parseObjectMap(resultMap, "evidenceRetrieval"))
                        .exportKnowledgeTimeline(parseMapList(resultMap, "exportKnowledgeTimeline"))
                        .evidenceGuardrails(parseObjectMap(resultMap, "evidenceGuardrails"))
                        .sourceTrustSummary(parseObjectMap(resultMap, "sourceTrustSummary"))
                        .regionalIndustryProfile(parseObjectMap(resultMap, "regionalIndustryProfile"))
                        .buyerRequestTrace(parseObjectMap(resultMap, "buyerRequestTrace"))
                        .scoreContributionLedger(parseMapList(resultMap, "scoreContributionLedger"))
                        .pythonWorker(parseObjectMap(resultMap, "pythonWorker"))
                        .pythonAgenticLoop(parseObjectMap(resultMap, "pythonAgenticLoop"))
                        .normalizedMetrics(parseObjectMap(resultMap, "normalizedMetrics"))
                        .compiledWiki(parseObjectMap(resultMap, "compiledWiki"))
                        .scoringRubric(parseObjectMap(resultMap, "scoringRubric"))
                        .agentInference(parseObjectMap(resultMap, "agentInference"))
                        .knowledgeLoopSuggestions(parseMapList(resultMap, "knowledgeLoopSuggestions"))
                        .publicDataCollectionRunId(asString(resultMap.get("publicDataCollectionRunId")))
                        .inputTrace(parseObjectMap(resultMap, "inputTrace"))
                        .stageCards(parseObjectMap(resultMap, "stageCards"))
                        .reportMarkdown(asString(resultMap.get("reportMarkdown")))
                        .reportFastApiUsed(Boolean.TRUE.equals(resultMap.get("reportFastApiUsed")))
                        .errorType(asString(resultMap.get("errorType")))
                        .errorMessage(asString(resultMap.get("errorMessage")))
                        .failedAt(asString(resultMap.get("failedAt")))
                        .build();
            } catch (Exception e) {
                log.warn("Failed to parse result JSON for diagnosis: {}", diagnosisId);
            }
        }

        KnowledgeSnapshot snapshot = parseKnowledgeSnapshot(run.getKnowledgeSnapshot());

        String productId = run.getProduct() != null ? run.getProduct().getId() : null;
        String productName = resolveProductName(run);
        String companyId = run.getCompany() != null ? run.getCompany().getId() : null;
        String knowledgeSnapshotId = productId != null
                ? "snap-" + productId + "-" + run.getCreatedAt().toLocalDate().format(DATE_FMT)
                : null;

        return DiagnosisDetailResponse.builder()
                .id(run.getId())
                .productId(productId)
                .companyId(companyId)
                .knowledgeSnapshotId(knowledgeSnapshotId)
                .productName(productName)
                .targetCountry(run.getTargetCountry())
                .status(run.getStatus().name())
                .result(resultData)
                .knowledgeSnapshot(snapshot)
                .createdAt(DateTimeUtils.formatUtc(run.getCreatedAt()))
                .build();
    }

    private Integer extractOverallScore(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) return null;
        try {
            Map<String, Object> resultMap = objectMapper.readValue(resultJson,
                    new TypeReference<Map<String, Object>>() {});
            DiagnosisScore score = parseDiagnosisScore(resultMap);
            return score.getOverall();
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public DiagnosisSourcesResponse getSources(String userId, String diagnosisId) {
        User user = findUser(userId);
        DiagnosisRun run = findDiagnosisRun(user, diagnosisId);

        List<SourceEntry> sources = new ArrayList<>();

        if (run.getResult() != null) {
            try {
                Map<String, Object> resultMap = objectMapper.readValue(run.getResult(),
                        new TypeReference<Map<String, Object>>() {});
                Object sourcesObj = resultMap.get("sources");
                if (sourcesObj instanceof List) {
                    for (Object item : (List<?>) sourcesObj) {
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> s = (Map<String, Object>) item;
                            sources.add(SourceEntry.builder()
                                    .field((String) s.get("field"))
                                    .sourceId(asString(s.get("sourceId")))
                                    .provider(asString(s.get("provider")))
                                    .agency(asString(s.get("agency")))
                                    .ministry(asString(s.get("ministry")))
                                    .dataFamily(asString(s.get("dataFamily")))
                                    .motieRelated(asBoolean(s.get("motieRelated")))
                                    .datasetName(asString(s.get("datasetName")))
                                    .documentName((String) s.get("documentName"))
                                    .url((String) s.get("url"))
                                    .retrievedAt((String) s.get("retrievedAt"))
                                    .recordId(asString(s.get("recordId")))
                                    .matchedKey(asString(s.get("matchedKey")))
                                    .usedFor(asString(s.get("usedFor")))
                                    .scoreContribution(asInteger(s.get("scoreContribution")))
                                    .rawScoreContribution(asInteger(s.get("rawScoreContribution")))
                                    .confidenceAdjustedContribution(asInteger(s.get("confidenceAdjustedContribution")))
                                    .confidence(asDouble(s.get("confidence")))
                                    .scoreFormula(asString(s.get("scoreFormula")))
                                    .scoreInputs(s.get("scoreInputs"))
                                    .sourceType(asString(s.get("sourceType")))
                                    .fallbackUsed(asBoolean(s.get("fallbackUsed")))
                                    .cacheHit(asBoolean(s.get("cacheHit")))
                                    .collectionRunId(asString(s.get("collectionRunId")))
                                    .errorMessage(asString(s.get("errorMessage")))
                                    .reason(asString(s.get("reason")))
                                    .collectionEvidence(s.get("collectionEvidence"))
                                    .endpointConfigured(asBoolean(s.get("endpointConfigured")))
                                    .operationEndpoint(asBoolean(s.get("operationEndpoint")))
                                    .endpointType(asString(s.get("endpointType")))
                                    .apiKeyConfigured(asBoolean(s.get("apiKeyConfigured")))
                                    .apiKeyStored(asBoolean(s.get("apiKeyStored")))
                                    .fetchedAt(asString(s.get("fetchedAt")))
                                    .sourcePeriodStart(asString(s.get("sourcePeriodStart")))
                                    .sourcePeriodEnd(asString(s.get("sourcePeriodEnd")))
                                    .metricSummary(asString(s.get("metricSummary")))
                                    .decisionImpact(asString(s.get("decisionImpact")))
                                    .metrics(s.get("metrics"))
                                    .domainSignals(s.get("domainSignals"))
                                    .build());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse sources from result for diagnosis: {}", diagnosisId);
            }
        }

        return DiagnosisSourcesResponse.builder()
                .diagnosisId(run.getId())
                .sources(sources)
                .build();
    }

    @Transactional(readOnly = true)
    public DiagnosisReportResponse getReport(String userId, String diagnosisId) {
        User user = findUser(userId);
        DiagnosisRun run = findDiagnosisRun(user, diagnosisId);
        String markdown = null;
        boolean fastApiUsed = false;

        if (run.getResult() != null) {
            try {
                Map<String, Object> resultMap = objectMapper.readValue(run.getResult(),
                        new TypeReference<Map<String, Object>>() {});
                markdown = asString(resultMap.get("reportMarkdown"));
                fastApiUsed = Boolean.TRUE.equals(resultMap.get("reportFastApiUsed"));
            } catch (Exception e) {
                log.warn("Failed to parse report from result JSON for diagnosis: {}", diagnosisId);
            }
        }

        if (markdown == null || markdown.isBlank()) {
            markdown = "# GROW-GATE Local EX 진단 리포트\n\n"
                    + "- 제품: " + resolveProductName(run) + "\n"
                    + "- 대상국: " + run.getTargetCountry() + "\n"
                    + "- 상태: " + run.getStatus().name() + "\n";
        }

        return new DiagnosisReportResponse(
                run.getId(),
                "grow-gate-diagnosis-" + run.getId() + ".md",
                "text/markdown; charset=UTF-8",
                markdown,
                fastApiUsed
        );
    }

    private UserRole resolveRole(User user) {
        return membershipRepository.findFirstByUserId(user.getId())
                .map(Membership::getRole)
                .orElse(user.getUserType().name().equals("personal") ? UserRole.owner : null);
    }

    private void dispatchAfterCommit(String diagnosisId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            asyncProcessor.process(diagnosisId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                asyncProcessor.process(diagnosisId);
            }
        });
    }

    private Company resolveCompany(String userId) {
        return membershipRepository.findFirstByUserId(userId)
                .map(Membership::getCompany)
                .orElse(null);
    }

    private void requireDiagnosisPermission(User user) {
        UserRole role = resolveRole(user);
        if (role == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "진단을 실행할 회사 멤버십이 없습니다.");
        }
        if (role == UserRole.viewer) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "진단을 실행할 권한이 없습니다.");
        }
    }

    private Product findProductByUser(User user, String productId) {
        Company company = resolveCompany(user.getId());
        if (company != null) {
            return productRepository.findByIdAndCompany_IdAndDeletedAtIsNull(productId, company.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "등록된 제품을 찾을 수 없습니다."));
        }
        return productRepository.findByIdAndOwnerIdAndDeletedAtIsNull(productId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "등록된 제품을 찾을 수 없습니다."));
    }

    private void validateDirectInput(CreateDiagnosisRequest.InputData inputData) {
        if (inputData == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "직접 입력 진단은 제품 정보가 필요합니다.");
        }
        if (isBlank(inputData.getProductName())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "직접 입력 진단은 제품명이 필요합니다.");
        }
        if (isBlank(inputData.getHsCode()) || !inputData.getHsCode().matches("^\\d{6,10}$")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "직접 입력 진단은 숫자 6~10자리 HS Code가 필요합니다.");
        }
    }

    private DiagnosisRun findDiagnosisRun(User user, String diagnosisId) {
        Company company = resolveCompany(user.getId());
        if (company != null) {
            return diagnosisRepository.findByIdAndCompanyId(diagnosisId, company.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "진단 결과를 찾을 수 없습니다."));
        }
        return diagnosisRepository.findByIdAndUserId(diagnosisId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "진단 결과를 찾을 수 없습니다."));
    }

    private String buildKnowledgeSnapshot(Product product, CreateDiagnosisRequest.InputData inputData, String targetCountry) {
        try {
            String productId = product.getId();
            ProductKnowledge knowledge = knowledgeRepository.findByProductId(productId).orElse(null);
            List<Certification> certs = certificationRepository.findByProductId(productId);
            boolean hasFto = !ftoRepository.findByProductId(productId).isEmpty();
            boolean hasBuyer = !buyerRequirementRepository.findByProductId(productId).isEmpty();

            List<String> missingFields = new ArrayList<>();
            if (!hasFto) missingFields.add("fto");
            if (!hasBuyer) missingFields.add("buyerRequirements");

            List<Map<String, Object>> certList = certs.stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("name", c.getName());
                m.put("expiresAt", c.getExpiresAt() != null ? c.getExpiresAt().format(DATE_FMT) : null);
                long days = c.getExpiresAt() != null
                        ? ChronoUnit.DAYS.between(LocalDate.now(), c.getExpiresAt()) : 0;
                m.put("daysUntilExpiry", days);
                return m;
            }).collect(Collectors.toList());

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("productName", firstNonBlank(
                    inputData != null ? inputData.getProductName() : null,
                    product.getName()));
            snapshot.put("hsCode", firstNonBlank(
                    inputData != null ? inputData.getHsCode() : null,
                    product.getHsCode()));
            snapshot.put("targetCountry", firstNonBlank(targetCountry, product.getTargetCountry()));
            snapshot.put("description", firstNonBlank(
                    inputData != null ? inputData.getDescription() : null,
                    product.getDescription()));
            snapshot.put("region", inputData != null ? inputData.getRegion() : null);
            snapshot.put("industry", inputData != null ? inputData.getIndustry() : null);
            snapshot.put("buyerRequest", inputData != null ? inputData.getBuyerRequest() : null);
            snapshot.put("buyerDeadline", inputData != null && inputData.getBuyerDeadline() != null
                    ? inputData.getBuyerDeadline().toString() : null);
            snapshot.put("expectedShipmentDate", inputData != null && inputData.getExpectedShipmentDate() != null
                    ? inputData.getExpectedShipmentDate().toString() : null);
            snapshot.put("requestedCertifications", inputData != null && inputData.getRequestedCertifications() != null
                    ? inputData.getRequestedCertifications() : List.of());
            snapshot.put("currentDocuments", inputData != null && inputData.getCurrentDocuments() != null
                    ? inputData.getCurrentDocuments() : List.of());
            snapshot.put("trustGrade", knowledge != null && knowledge.getTrustGrade() != null
                    ? knowledge.getTrustGrade().name() : null);
            snapshot.put("sourceType", knowledge != null ? knowledge.getSourceType() : null);
            snapshot.put("certifications", certList);
            snapshot.put("missingFields", missingFields);
            snapshot.put("snapshotAt", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "T00:00:00Z");

            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.error("Failed to build knowledge snapshot", e);
            return null;
        }
    }

    private String serializeInputData(CreateDiagnosisRequest.InputData inputData, Product product) {
        if (inputData == null && product == null) return null;
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("productName", firstNonBlank(inputData != null ? inputData.getProductName() : null,
                    product != null ? product.getName() : null));
            map.put("hsCode", firstNonBlank(inputData != null ? inputData.getHsCode() : null,
                    product != null ? product.getHsCode() : null));
            map.put("description", firstNonBlank(inputData != null ? inputData.getDescription() : null,
                    product != null ? product.getDescription() : null));
            map.put("region", inputData != null ? inputData.getRegion() : null);
            map.put("industry", inputData != null ? inputData.getIndustry() : null);
            map.put("buyerRequest", inputData != null ? inputData.getBuyerRequest() : null);
            map.put("buyerDeadline", inputData == null || inputData.getBuyerDeadline() == null ? null : inputData.getBuyerDeadline().toString());
            map.put("expectedShipmentDate", inputData == null || inputData.getExpectedShipmentDate() == null ? null : inputData.getExpectedShipmentDate().toString());
            map.put("requestedCertifications", inputData == null || inputData.getRequestedCertifications() == null ? List.of() : inputData.getRequestedCertifications());
            map.put("currentDocuments", inputData == null || inputData.getCurrentDocuments() == null ? List.of() : inputData.getCurrentDocuments());
            map.put("exportExperience", inputData != null ? inputData.getExportExperience() : null);
            map.put("budgetLimitKrw", inputData != null ? inputData.getBudgetLimitKrw() : null);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildInputSnapshot(CreateDiagnosisRequest.InputData inputData, String targetCountry) {
        if (inputData == null) return null;
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("productName", inputData.getProductName());
            snapshot.put("hsCode", inputData.getHsCode());
            snapshot.put("targetCountry", targetCountry);
            snapshot.put("region", inputData.getRegion());
            snapshot.put("industry", inputData.getIndustry());
            snapshot.put("buyerRequest", inputData.getBuyerRequest());
            snapshot.put("buyerDeadline", inputData.getBuyerDeadline() == null ? null : inputData.getBuyerDeadline().toString());
            snapshot.put("expectedShipmentDate", inputData.getExpectedShipmentDate() == null ? null : inputData.getExpectedShipmentDate().toString());
            snapshot.put("requestedCertifications", inputData.getRequestedCertifications() == null ? List.of() : inputData.getRequestedCertifications());
            snapshot.put("currentDocuments", inputData.getCurrentDocuments() == null ? List.of() : inputData.getCurrentDocuments());
            snapshot.put("trustGrade", null);
            snapshot.put("sourceType", "direct_input");
            snapshot.put("certifications", List.of());
            snapshot.put("missingFields", List.of("productKnowledge", "certifications", "fto", "buyerRequirements"));
            snapshot.put("snapshotAt", LocalDate.now().format(DATE_FMT) + "T00:00:00Z");
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.warn("Failed to build direct input knowledge snapshot");
            return null;
        }
    }

    private KnowledgeSnapshot parseKnowledgeSnapshot(String json) {
        if (json == null) return null;
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            Object certsObj = map.get("certifications");
            List<CertSnapshot> certSnapshots = new ArrayList<>();
            if (certsObj instanceof List) {
                for (Object item : (List<?>) certsObj) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> certMap = (Map<String, Object>) item;
                        certSnapshots.add(CertSnapshot.builder()
                                .name((String) certMap.get("name"))
                                .expiresAt((String) certMap.get("expiresAt"))
                                .daysUntilExpiry(certMap.get("daysUntilExpiry") != null
                                        ? ((Number) certMap.get("daysUntilExpiry")).longValue() : 0)
                                .build());
                    }
                }
            }

            @SuppressWarnings("unchecked")
            List<String> missingFields = (List<String>) map.getOrDefault("missingFields", new ArrayList<>());

            return KnowledgeSnapshot.builder()
                    .productName((String) map.get("productName"))
                    .hsCode((String) map.get("hsCode"))
                    .targetCountry((String) map.get("targetCountry"))
                    .trustGrade((String) map.get("trustGrade"))
                    .sourceType((String) map.get("sourceType"))
                    .certifications(certSnapshots)
                    .missingFields(missingFields)
                    .snapshotAt((String) map.get("snapshotAt"))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse knowledge snapshot JSON");
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<RiskItem> parseRiskItems(Map<String, Object> resultMap) {
        Object obj = resultMap.get("riskItems");
        if (!(obj instanceof List)) return List.of();
        List<RiskItem> items = new ArrayList<>();
        for (Object item : (List<?>) obj) {
            if (item instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) item;
                items.add(RiskItem.builder()
                        .riskId(asString(m.get("riskId")))
                        .category(asString(m.get("category")))
                        .level(asString(m.get("level")))
                        .label(asString(m.get("label")))
                        .severity(asString(m.getOrDefault("severity", m.get("level"))))
                        .probability(asString(m.get("probability")))
                        .businessImpact(asString(m.get("businessImpact")))
                        .requiredEvidence(asString(m.get("requiredEvidence")))
                        .ownerRole(asString(m.get("ownerRole")))
                        .deadline(asString(m.get("deadline")))
                        .nextAction(asString(m.get("nextAction")))
                        .sourceIds(parseStringList(m, "sourceIds"))
                        .build());
            }
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private DiagnosisScore parseDiagnosisScore(Map<String, Object> resultMap) {
        Object obj = resultMap.get("diagnosisScore");
        if (!(obj instanceof Map)) return DiagnosisScore.builder().build();
        Map<String, Object> m = (Map<String, Object>) obj;
        return DiagnosisScore.builder()
                .overall(m.containsKey("overall") ? ((Number) m.get("overall")).intValue() : 0)
                .market(m.containsKey("market") ? ((Number) m.get("market")).intValue() : 0)
                .regulation(m.containsKey("regulation") ? ((Number) m.get("regulation")).intValue() : 0)
                .recall(m.containsKey("recall") ? ((Number) m.get("recall")).intValue() : 0)
                .fto(m.containsKey("fto") ? ((Number) m.get("fto")).intValue() : 0)
                .support(m.containsKey("support") ? ((Number) m.get("support")).intValue() : 0)
                .build();
    }

    @SuppressWarnings("unchecked")
    private ActionRoadmap parseActionRoadmap(Map<String, Object> resultMap) {
        Object obj = resultMap.get("actionRoadmap");
        if (!(obj instanceof Map)) return null;
        Map<String, Object> m = (Map<String, Object>) obj;
        return ActionRoadmap.builder()
                .d30(m.containsKey("d30") ? ((Number) m.get("d30")).intValue() : 0)
                .d60(m.containsKey("d60") ? ((Number) m.get("d60")).intValue() : 0)
                .d90(m.containsKey("d90") ? ((Number) m.get("d90")).intValue() : 0)
                .thisWeek(parseActionItems(m, "thisWeek"))
                .beforeQuote(parseActionItems(m, "beforeQuote"))
                .beforeShipment(parseActionItems(m, "beforeShipment"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private GateDecision parseGateDecision(Map<String, Object> resultMap) {
        Object obj = resultMap.get("gateDecision");
        if (!(obj instanceof Map)) return null;
        Map<String, Object> m = (Map<String, Object>) obj;
        return GateDecision.builder()
                .status(asString(m.get("status")))
                .label(asString(m.get("label")))
                .oneLineReason(asString(m.get("oneLineReason")))
                .allowedNextStep(asString(m.get("allowedNextStep")))
                .blockedStep(asString(m.get("blockedStep")))
                .buyerReplyGuidance(asString(m.get("buyerReplyGuidance")))
                .mustFixBeforeQuote(parseStringList(m, "mustFixBeforeQuote"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private ImpactEstimate parseImpactEstimate(Map<String, Object> resultMap) {
        Object obj = resultMap.get("impactEstimate");
        if (!(obj instanceof Map)) return null;
        Map<String, Object> m = (Map<String, Object>) obj;
        return ImpactEstimate.builder()
                .timeSavedHours(asInteger(m.get("timeSavedHours")) == null ? 0 : asInteger(m.get("timeSavedHours")))
                .delayRiskDays(asInteger(m.get("delayRiskDays")) == null ? 0 : asInteger(m.get("delayRiskDays")))
                .avoidableCostKrw(asLong(m.get("avoidableCostKrw")))
                .supportFundingOpportunityKrw(asLong(m.get("supportFundingOpportunityKrw")))
                .explanation(asString(m.get("explanation")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private ConfidencePanel parseConfidencePanel(Map<String, Object> resultMap) {
        Object obj = resultMap.get("confidencePanel");
        if (!(obj instanceof Map)) return null;
        Map<String, Object> m = (Map<String, Object>) obj;
        return ConfidencePanel.builder()
                .level(asString(m.get("level")))
                .liveSourceCount(asInteger(m.get("liveSourceCount")) == null ? 0 : asInteger(m.get("liveSourceCount")))
                .cacheSourceCount(asInteger(m.get("cacheSourceCount")) == null ? 0 : asInteger(m.get("cacheSourceCount")))
                .fallbackSourceCount(asInteger(m.get("fallbackSourceCount")) == null ? 0 : asInteger(m.get("fallbackSourceCount")))
                .missingEvidence(parseStringList(m, "missingEvidence"))
                .humanReviewRequired(Boolean.TRUE.equals(m.get("humanReviewRequired")))
                .reviewReason(asString(m.get("reviewReason")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<ActionItem> parseActionItems(Map<String, Object> map, String key) {
        Object obj = map.get(key);
        if (!(obj instanceof List)) return List.of();
        List<ActionItem> items = new ArrayList<>();
        for (Object raw : (List<?>) obj) {
            if (raw instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) raw;
                items.add(ActionItem.builder()
                        .title(asString(m.get("title")))
                        .owner(asString(m.get("owner")))
                        .output(asString(m.get("output")))
                        .dueDate(asString(m.get("dueDate")))
                        .reason(asString(m.get("reason")))
                        .blockedIfNotDone(asString(m.get("blockedIfNotDone")))
                        .sourceIds(parseStringList(m, "sourceIds"))
                        .build());
            }
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(Map<String, Object> resultMap, String key) {
        Object obj = resultMap.get(key);
        if (obj instanceof List) return (List<String>) obj;
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseObjectMap(Map<String, Object> resultMap, String key) {
        Object obj = resultMap.get(key);
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseMapList(Map<String, Object> resultMap, String key) {
        Object obj = resultMap.get(key);
        if (!(obj instanceof List<?> list)) return List.of();
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private String resolveProductName(DiagnosisRun run) {
        if (run.getProduct() != null) {
            return run.getProduct().getName();
        }
        if (run.getInputData() != null) {
            try {
                Map<String, Object> map = objectMapper.readValue(run.getInputData(),
                        new TypeReference<Map<String, Object>>() {});
                return asString(map.getOrDefault("productName", ""));
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    private String resolveHsCode(DiagnosisRun run) {
        if (run.getProduct() != null) {
            return run.getProduct().getHsCode();
        }
        if (run.getInputData() != null) {
            try {
                Map<String, Object> map = objectMapper.readValue(run.getInputData(),
                        new TypeReference<Map<String, Object>>() {});
                return asString(map.getOrDefault("hsCode", ""));
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String firstNonBlank(String primary, String fallback) {
        return isBlank(primary) ? fallback : primary;
    }

    private Integer asInteger(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private Double asDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : null;
    }

    private Long asLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private Boolean asBoolean(Object value) {
        return value instanceof Boolean ? (Boolean) value : null;
    }

}
