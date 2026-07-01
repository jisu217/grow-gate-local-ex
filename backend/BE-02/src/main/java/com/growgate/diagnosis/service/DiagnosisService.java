package com.growgate.diagnosis.service;

// 진단 생성·조회
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.common.DateTimeUtils;
import com.growgate.diagnosis.dto.CreateDiagnosisRequest;
import com.growgate.diagnosis.dto.CreateDiagnosisResponse;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse.ActionRoadmap;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse.CertSnapshot;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse.DiagnosisScore;
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

import java.nio.charset.StandardCharsets;
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

            return DiagnosisSummary.builder()
                    .id(run.getId())
                    .productId(productId)
                    .productName(productName)
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
        if (request.getProductId() != null && request.getInputData() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "productId와 inputData는 동시에 보낼 수 없습니다.");
        }

        User user = findUser(userId);
        requireDiagnosisPermission(user);

        Product product = null;
        String inputDataJson = null;
        String knowledgeSnapshotJson = null;

        if (request.getProductId() != null) {
            product = findProductByUser(user, request.getProductId());
            knowledgeSnapshotJson = buildKnowledgeSnapshot(request.getProductId());
        } else {
            inputDataJson = serializeInputData(request.getInputData());
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
                        .summary(summary)
                        .riskItems(riskItems)
                        .actionRoadmap(actionRoadmap)
                        .evidenceSources(evidenceSources)
                        .agentTrace(parseObjectMap(resultMap, "agentTrace"))
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
                                    .datasetName(asString(s.get("datasetName")))
                                    .documentName((String) s.get("documentName"))
                                    .url((String) s.get("url"))
                                    .retrievedAt((String) s.get("retrievedAt"))
                                    .recordId(asString(s.get("recordId")))
                                    .matchedKey(asString(s.get("matchedKey")))
                                    .usedFor(asString(s.get("usedFor")))
                                    .scoreContribution(asInteger(s.get("scoreContribution")))
                                    .confidence(asDouble(s.get("confidence")))
                                    .sourceType(asString(s.get("sourceType")))
                                    .fallbackUsed(asBoolean(s.get("fallbackUsed")))
                                    .cacheHit(asBoolean(s.get("cacheHit")))
                                    .collectionRunId(asString(s.get("collectionRunId")))
                                    .errorMessage(asString(s.get("errorMessage")))
                                    .reason(asString(s.get("reason")))
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

    @Transactional(readOnly = true)
    public byte[] getReportPdf(String userId, String diagnosisId) {
        DiagnosisReportResponse report = getReport(userId, diagnosisId);
        String text = "GrowGate Local EX Report\n\n"
                + "Diagnosis ID: " + report.getDiagnosisId() + "\n"
                + report.getMarkdown().replaceAll("[^\\x20-\\x7E\\n]", "?");
        return minimalPdf(text);
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

    private DiagnosisRun findDiagnosisRun(User user, String diagnosisId) {
        Company company = resolveCompany(user.getId());
        if (company != null) {
            return diagnosisRepository.findByIdAndCompanyId(diagnosisId, company.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "진단 결과를 찾을 수 없습니다."));
        }
        return diagnosisRepository.findByIdAndUserId(diagnosisId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "진단 결과를 찾을 수 없습니다."));
    }

    private String buildKnowledgeSnapshot(String productId) {
        try {
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

    private String serializeInputData(CreateDiagnosisRequest.InputData inputData) {
        if (inputData == null) return null;
        try {
            Map<String, String> map = new HashMap<>();
            map.put("productName", inputData.getProductName());
            map.put("hsCode", inputData.getHsCode());
            map.put("description", inputData.getDescription());
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
                        .level((String) m.get("level"))
                        .label((String) m.get("label"))
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
                .build();
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

    private String resolveProductName(DiagnosisRun run) {
        if (run.getProduct() != null) {
            return run.getProduct().getName();
        }
        if (run.getInputData() != null) {
            try {
                Map<String, String> map = objectMapper.readValue(run.getInputData(),
                        new TypeReference<Map<String, String>>() {});
                return map.getOrDefault("productName", "");
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

    private Integer asInteger(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private Double asDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : null;
    }

    private Boolean asBoolean(Object value) {
        return value instanceof Boolean ? (Boolean) value : null;
    }

    private byte[] minimalPdf(String text) {
        String[] lines = text.split("\\n");
        StringBuilder content = new StringBuilder("BT /F1 11 Tf 50 780 Td 14 TL ");
        int count = 0;
        for (String line : lines) {
            if (count++ >= 48) break;
            content.append("(").append(escapePdf(line)).append(") Tj T* ");
        }
        content.append("ET");
        byte[] stream = content.toString().getBytes(StandardCharsets.US_ASCII);
        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + stream.length + " >>\nstream\n" + new String(stream, StandardCharsets.US_ASCII) + "\nendstream"
        );
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.length());
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xref = pdf.length();
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
        for (int offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n")
                .append("startxref\n").append(xref).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private String escapePdf(String value) {
        return value.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }
}
