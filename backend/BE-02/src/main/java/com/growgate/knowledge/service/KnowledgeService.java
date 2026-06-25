package com.growgate.knowledge.service;

// 제품 지식 조회/수정 + 감사 로그
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.knowledge.dto.BuyerRequirementResponse;
import com.growgate.knowledge.dto.CertificationResponse;
import com.growgate.knowledge.dto.CreateBuyerRequirementRequest;
import com.growgate.knowledge.dto.CreateCertificationRequest;
import com.growgate.knowledge.dto.CreateFtoRequest;
import com.growgate.knowledge.dto.CreateKnowledgeSourceRequest;
import com.growgate.knowledge.dto.DeleteEntryResponse;
import com.growgate.knowledge.dto.FtoResponse;
import com.growgate.knowledge.dto.KnowledgeDetailResponse;
import com.growgate.knowledge.dto.KnowledgeDetailResponse.BuyerRequirementData;
import com.growgate.knowledge.dto.KnowledgeDetailResponse.CertificationData;
import com.growgate.knowledge.dto.KnowledgeDetailResponse.FtoData;
import com.growgate.knowledge.dto.KnowledgeDetailResponse.KnowledgeData;
import com.growgate.knowledge.dto.KnowledgeDetailResponse.KnowledgeSourceData;
import com.growgate.knowledge.dto.KnowledgeLogsResponse;
import com.growgate.knowledge.dto.KnowledgeLogsResponse.LogEntry;
import com.growgate.knowledge.dto.KnowledgeSourceResponse;
import com.growgate.knowledge.dto.UpdateBuyerRequirementRequest;
import com.growgate.knowledge.dto.UpdateCertificationRequest;
import com.growgate.knowledge.dto.UpdateFtoRequest;
import com.growgate.knowledge.dto.UpdateKnowledgeRequest;
import com.growgate.knowledge.dto.UpdateKnowledgeResponse;
import com.growgate.knowledge.entity.AuditLog;
import com.growgate.knowledge.entity.BuyerRequirement;
import com.growgate.knowledge.entity.Certification;
import com.growgate.knowledge.entity.Fto;
import com.growgate.knowledge.entity.KnowledgeSource;
import com.growgate.knowledge.entity.ProductKnowledge;
import com.growgate.knowledge.enums.ChangeType;
import com.growgate.knowledge.enums.RiskLevel;
import com.growgate.knowledge.enums.TrustGrade;
import com.growgate.knowledge.repository.AuditLogRepository;
import com.growgate.knowledge.repository.BuyerRequirementRepository;
import com.growgate.knowledge.repository.CertificationRepository;
import com.growgate.knowledge.repository.FtoRepository;
import com.growgate.knowledge.repository.KnowledgeSourceRepository;
import com.growgate.knowledge.repository.ProductKnowledgeRepository;
import com.growgate.company.entity.Company;
import com.growgate.membership.entity.Membership;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.product.entity.Product;
import com.growgate.product.repository.ProductRepository;
import com.growgate.user.entity.User;
import com.growgate.user.enums.UserRole;
import com.growgate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KnowledgeService {

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int EXPIRY_WARNING_DAYS = 90;

    private final ProductRepository productRepository;
    private final ProductKnowledgeRepository knowledgeRepository;
    private final CertificationRepository certificationRepository;
    private final FtoRepository ftoRepository;
    private final BuyerRequirementRepository buyerRequirementRepository;
    private final KnowledgeSourceRepository knowledgeSourceRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public KnowledgeDetailResponse getDetail(String userId, String productId) {
        User user = findUser(userId);
        Product product = findProductByUser(user, productId);
        ProductKnowledge knowledge = knowledgeRepository.findByProductId(productId).orElse(null);
        List<Certification> certs = certificationRepository.findByProductId(productId);
        List<Fto> ftoList = ftoRepository.findByProductId(productId);
        List<BuyerRequirement> buyers = buyerRequirementRepository.findByProductId(productId);
        List<KnowledgeSource> sources = knowledge != null
                ? knowledgeSourceRepository.findByKnowledgeId(knowledge.getId())
                : List.of();

        KnowledgeData knowledgeData = null;
        if (knowledge != null) {
            knowledgeData = KnowledgeData.builder()
                    .trustGrade(knowledge.getTrustGrade() != null ? knowledge.getTrustGrade().name() : null)
                    .sourceType(knowledge.getSourceType())
                    .notes(knowledge.getNotes())
                    .build();
        }

        List<CertificationData> certData = certs.stream().map(c -> {
            long days = 0;
            if (c.getExpiresAt() != null) {
                days = ChronoUnit.DAYS.between(LocalDate.now(), c.getExpiresAt());
            }
            return CertificationData.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .issuedAt(c.getIssuedAt() != null ? c.getIssuedAt().format(DATE_FMT) : null)
                    .expiresAt(c.getExpiresAt() != null ? c.getExpiresAt().format(DATE_FMT) : null)
                    .renewalCycle(c.getRenewalCycle())
                    .daysUntilExpiry(days)
                    .build();
        }).collect(Collectors.toList());

        List<FtoData> ftoData = ftoList.stream().map(f -> FtoData.builder()
                .id(f.getId())
                .targetCountry(f.getTargetCountry())
                .riskLevel(f.getRiskLevel() != null ? f.getRiskLevel().name() : null)
                .summary(f.getSummary())
                .expiresAt(f.getExpiresAt() != null ? f.getExpiresAt().format(DATE_FMT) : null)
                .build()).collect(Collectors.toList());

        List<BuyerRequirementData> buyerData = buyers.stream().map(b -> BuyerRequirementData.builder()
                .id(b.getId())
                .buyerName(b.getBuyerName())
                .country(b.getCountry())
                .requirements(b.getRequirements())
                .build()).collect(Collectors.toList());

        List<KnowledgeSourceData> sourceData = sources.stream().map(s -> KnowledgeSourceData.builder()
                .id(s.getId())
                .url(s.getUrl())
                .documentName(s.getDocumentName())
                .retrievedAt(s.getRetrievedAt() != null ? s.getRetrievedAt().format(DATE_FMT) : null)
                .build()).collect(Collectors.toList());

        return KnowledgeDetailResponse.builder()
                .productId(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .hsCode(product.getHsCode())
                .targetCountry(product.getTargetCountry())
                .description(product.getDescription())
                .knowledge(knowledgeData)
                .certifications(certData)
                .fto(ftoData)
                .buyerRequirements(buyerData)
                .knowledgeSources(sourceData)
                .updatedAt(product.getUpdatedAt().format(ISO_FMT))
                .build();
    }

    public UpdateKnowledgeResponse update(String userId, String productId, UpdateKnowledgeRequest request) {
        User user = findUser(userId);
        requireRole(resolveRole(userId), "edit");
        Product product = findProductByUser(user, productId);
        ProductKnowledge knowledge = knowledgeRepository.findByProductId(productId).orElse(null);

        List<String> changedFields = new ArrayList<>();
        Map<String, String> previousValues = new HashMap<>();
        Map<String, String> newValues = new HashMap<>();

        if (request.getName() != null && !request.getName().equals(product.getName())) {
            changedFields.add("name");
            previousValues.put("name", product.getName());
            newValues.put("name", request.getName());
        }
        if (request.getCategory() != null && !request.getCategory().equals(product.getCategory())) {
            changedFields.add("category");
            previousValues.put("category", product.getCategory());
            newValues.put("category", request.getCategory());
        }
        if (request.getHsCode() != null && !request.getHsCode().equals(product.getHsCode())) {
            changedFields.add("hsCode");
            previousValues.put("hsCode", product.getHsCode());
            newValues.put("hsCode", request.getHsCode());
        }
        if (request.getTargetCountry() != null && !request.getTargetCountry().equals(product.getTargetCountry())) {
            changedFields.add("targetCountry");
            previousValues.put("targetCountry", product.getTargetCountry());
            newValues.put("targetCountry", request.getTargetCountry());
        }
        if (request.getDescription() != null && !request.getDescription().equals(product.getDescription())) {
            changedFields.add("description");
            previousValues.put("description", product.getDescription());
            newValues.put("description", request.getDescription());
        }

        product.update(request.getName(), request.getCategory(), request.getHsCode(),
                request.getTargetCountry(), request.getDescription());

        if (request.getKnowledge() != null && knowledge != null) {
            UpdateKnowledgeRequest.KnowledgeUpdateData kd = request.getKnowledge();

            TrustGrade newTrustGrade = null;
            if (kd.getTrustGrade() != null) {
                try {
                    newTrustGrade = TrustGrade.valueOf(kd.getTrustGrade());
                } catch (IllegalArgumentException ignored) {}
                if (newTrustGrade != null && newTrustGrade != knowledge.getTrustGrade()) {
                    changedFields.add("trustGrade");
                    previousValues.put("trustGrade",
                            knowledge.getTrustGrade() != null ? knowledge.getTrustGrade().name() : null);
                    newValues.put("trustGrade", newTrustGrade.name());
                }
            }
            if (kd.getNotes() != null && !kd.getNotes().equals(knowledge.getNotes())) {
                changedFields.add("notes");
                previousValues.put("notes", knowledge.getNotes());
                newValues.put("notes", kd.getNotes());
            }
            if (kd.getSourceType() != null && !kd.getSourceType().equals(knowledge.getSourceType())) {
                changedFields.add("sourceType");
                previousValues.put("sourceType", knowledge.getSourceType());
                newValues.put("sourceType", kd.getSourceType());
            }

            knowledge.update(newTrustGrade, kd.getSourceType(), kd.getNotes(), user);
        }

        if (!changedFields.isEmpty()) {
            AuditLog log = AuditLog.builder()
                    .product(product)
                    .changedBy(user)
                    .changeType(ChangeType.update)
                    .changedFields(changedFields)
                    .previousValue(toJson(previousValues))
                    .newValue(toJson(newValues))
                    .reason(request.getReason())
                    .build();
            auditLogRepository.save(log);
        }

        return UpdateKnowledgeResponse.builder()
                .productId(product.getId())
                .updatedAt(product.getUpdatedAt().format(ISO_FMT))
                .build();
    }

    public CertificationResponse createCertification(String userId, String productId, CreateCertificationRequest request) {
        User user = findUser(userId);
        requireRole(resolveRole(userId), "edit");
        Product product = findProductByUser(user, productId);

        Certification cert = Certification.builder()
                .product(product)
                .name(request.getName())
                .issuedAt(parseDate(request.getIssuedAt()))
                .expiresAt(parseDate(request.getExpiresAt()))
                .renewalCycle(request.getRenewalCycle())
                .build();
        certificationRepository.save(cert);

        recordAuditLog(product, user, ChangeType.create,
                List.of("name", "issuedAt", "expiresAt", "renewalCycle"), null,
                toJson(Map.of(
                        "name", nullToEmpty(request.getName()),
                        "issuedAt", nullToEmpty(request.getIssuedAt()),
                        "expiresAt", nullToEmpty(request.getExpiresAt()),
                        "renewalCycle", request.getRenewalCycle() != null ? request.getRenewalCycle().toString() : "")),
                request.getReason() != null ? request.getReason() : "인증서 등록");

        return toCertificationResponse(cert);
    }

    public CertificationResponse updateCertification(String userId, String productId, String certificationId,
                                                       UpdateCertificationRequest request) {
        User user = findUser(userId);
        requireRole(resolveRole(userId), "edit");
        findProductByUser(user, productId);
        Certification cert = certificationRepository.findByIdAndProductId(certificationId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "인증서를 찾을 수 없습니다."));

        List<String> changedFields = new ArrayList<>();
        Map<String, String> previousValues = new HashMap<>();
        Map<String, String> newValues = new HashMap<>();

        if (request.getName() != null && !request.getName().equals(cert.getName())) {
            changedFields.add("name");
            previousValues.put("name", cert.getName());
            newValues.put("name", request.getName());
        }
        if (request.getIssuedAt() != null) {
            LocalDate parsed = parseDate(request.getIssuedAt());
            if (!Objects.equals(parsed, cert.getIssuedAt())) {
                changedFields.add("issuedAt");
                previousValues.put("issuedAt", cert.getIssuedAt() != null ? cert.getIssuedAt().format(DATE_FMT) : null);
                newValues.put("issuedAt", request.getIssuedAt());
            }
        }
        if (request.getExpiresAt() != null) {
            LocalDate parsed = parseDate(request.getExpiresAt());
            if (!Objects.equals(parsed, cert.getExpiresAt())) {
                changedFields.add("expiresAt");
                previousValues.put("expiresAt", cert.getExpiresAt() != null ? cert.getExpiresAt().format(DATE_FMT) : null);
                newValues.put("expiresAt", request.getExpiresAt());
            }
        }
        if (request.getRenewalCycle() != null && !request.getRenewalCycle().equals(cert.getRenewalCycle())) {
            changedFields.add("renewalCycle");
            previousValues.put("renewalCycle", cert.getRenewalCycle() != null ? cert.getRenewalCycle().toString() : null);
            newValues.put("renewalCycle", request.getRenewalCycle().toString());
        }

        cert.update(request.getName(), parseDate(request.getIssuedAt()), parseDate(request.getExpiresAt()),
                request.getRenewalCycle());

        if (!changedFields.isEmpty()) {
            recordAuditLog(cert.getProduct(), user, ChangeType.update, changedFields,
                    toJson(previousValues), toJson(newValues), request.getReason());
        }

        return toCertificationResponse(cert);
    }

    public DeleteEntryResponse deleteCertification(String userId, String productId, String certificationId) {
        User user = findUser(userId);
        requireRole(resolveRole(userId), "edit");
        Product product = findProductByUser(user, productId);
        Certification cert = certificationRepository.findByIdAndProductId(certificationId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "인증서를 찾을 수 없습니다."));

        certificationRepository.delete(cert);
        recordAuditLog(product, user, ChangeType.delete, List.of("certification:" + cert.getName()),
                null, null, "인증서 삭제");

        return new DeleteEntryResponse("인증서가 삭제되었습니다.", certificationId);
    }

    public FtoResponse createFto(String userId, String productId, CreateFtoRequest request) {
        User user = findUser(userId);
        requireRole(resolveRole(userId), "edit");
        Product product = findProductByUser(user, productId);

        Fto fto = Fto.builder()
                .product(product)
                .targetCountry(request.getTargetCountry())
                .riskLevel(parseRiskLevel(request.getRiskLevel()))
                .summary(request.getSummary())
                .expiresAt(parseDate(request.getExpiresAt()))
                .build();
        ftoRepository.save(fto);

        recordAuditLog(product, user, ChangeType.create,
                List.of("targetCountry", "riskLevel", "summary", "expiresAt"), null,
                toJson(Map.of(
                        "targetCountry", nullToEmpty(request.getTargetCountry()),
                        "riskLevel", nullToEmpty(request.getRiskLevel()),
                        "summary", nullToEmpty(request.getSummary()),
                        "expiresAt", nullToEmpty(request.getExpiresAt()))),
                request.getReason() != null ? request.getReason() : "FTO 등록");

        return toFtoResponse(fto);
    }

    public FtoResponse updateFto(String userId, String productId, String ftoId, UpdateFtoRequest request) {
        User user = findUser(userId);
        requireRole(resolveRole(userId), "edit");
        findProductByUser(user, productId);
        Fto fto = ftoRepository.findByIdAndProductId(ftoId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "FTO 항목을 찾을 수 없습니다."));

        List<String> changedFields = new ArrayList<>();
        Map<String, String> previousValues = new HashMap<>();
        Map<String, String> newValues = new HashMap<>();

        if (request.getTargetCountry() != null && !request.getTargetCountry().equals(fto.getTargetCountry())) {
            changedFields.add("targetCountry");
            previousValues.put("targetCountry", fto.getTargetCountry());
            newValues.put("targetCountry", request.getTargetCountry());
        }
        RiskLevel newRiskLevel = request.getRiskLevel() != null ? parseRiskLevel(request.getRiskLevel()) : null;
        if (newRiskLevel != null && newRiskLevel != fto.getRiskLevel()) {
            changedFields.add("riskLevel");
            previousValues.put("riskLevel", fto.getRiskLevel() != null ? fto.getRiskLevel().name() : null);
            newValues.put("riskLevel", newRiskLevel.name());
        }
        if (request.getSummary() != null && !request.getSummary().equals(fto.getSummary())) {
            changedFields.add("summary");
            previousValues.put("summary", fto.getSummary());
            newValues.put("summary", request.getSummary());
        }
        if (request.getExpiresAt() != null) {
            LocalDate parsed = parseDate(request.getExpiresAt());
            if (!Objects.equals(parsed, fto.getExpiresAt())) {
                changedFields.add("expiresAt");
                previousValues.put("expiresAt", fto.getExpiresAt() != null ? fto.getExpiresAt().format(DATE_FMT) : null);
                newValues.put("expiresAt", request.getExpiresAt());
            }
        }

        fto.update(request.getTargetCountry(), newRiskLevel, request.getSummary(), parseDate(request.getExpiresAt()));

        if (!changedFields.isEmpty()) {
            recordAuditLog(fto.getProduct(), user, ChangeType.update, changedFields,
                    toJson(previousValues), toJson(newValues), request.getReason());
        }

        return toFtoResponse(fto);
    }

    public DeleteEntryResponse deleteFto(String userId, String productId, String ftoId) {
        User user = findUser(userId);
        requireRole(resolveRole(userId), "edit");
        Product product = findProductByUser(user, productId);
        Fto fto = ftoRepository.findByIdAndProductId(ftoId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "FTO 항목을 찾을 수 없습니다."));

        ftoRepository.delete(fto);
        recordAuditLog(product, user, ChangeType.delete, List.of("fto:" + fto.getTargetCountry()),
                null, null, "FTO 항목 삭제");

        return new DeleteEntryResponse("FTO 항목이 삭제되었습니다.", ftoId);
    }

    public BuyerRequirementResponse createBuyerRequirement(String userId, String productId,
                                                             CreateBuyerRequirementRequest request) {
        User user = findUser(userId);
        requireRole(resolveRole(userId), "edit");
        Product product = findProductByUser(user, productId);

        BuyerRequirement buyer = BuyerRequirement.builder()
                .product(product)
                .buyerName(request.getBuyerName())
                .country(request.getCountry())
                .requirements(request.getRequirements())
                .build();
        buyerRequirementRepository.save(buyer);

        recordAuditLog(product, user, ChangeType.create,
                List.of("buyerName", "country", "requirements"), null,
                toJson(Map.of(
                        "buyerName", nullToEmpty(request.getBuyerName()),
                        "country", nullToEmpty(request.getCountry()),
                        "requirements", nullToEmpty(request.getRequirements()))),
                request.getReason() != null ? request.getReason() : "바이어 요구사항 등록");

        return toBuyerRequirementResponse(buyer);
    }

    public BuyerRequirementResponse updateBuyerRequirement(String userId, String productId, String buyerRequirementId,
                                                             UpdateBuyerRequirementRequest request) {
        User user = findUser(userId);
        requireRole(resolveRole(userId), "edit");
        findProductByUser(user, productId);
        BuyerRequirement buyer = buyerRequirementRepository.findByIdAndProductId(buyerRequirementId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "바이어 요구사항을 찾을 수 없습니다."));

        List<String> changedFields = new ArrayList<>();
        Map<String, String> previousValues = new HashMap<>();
        Map<String, String> newValues = new HashMap<>();

        if (request.getBuyerName() != null && !request.getBuyerName().equals(buyer.getBuyerName())) {
            changedFields.add("buyerName");
            previousValues.put("buyerName", buyer.getBuyerName());
            newValues.put("buyerName", request.getBuyerName());
        }
        if (request.getCountry() != null && !request.getCountry().equals(buyer.getCountry())) {
            changedFields.add("country");
            previousValues.put("country", buyer.getCountry());
            newValues.put("country", request.getCountry());
        }
        if (request.getRequirements() != null && !request.getRequirements().equals(buyer.getRequirements())) {
            changedFields.add("requirements");
            previousValues.put("requirements", buyer.getRequirements());
            newValues.put("requirements", request.getRequirements());
        }

        buyer.update(request.getBuyerName(), request.getCountry(), request.getRequirements());

        if (!changedFields.isEmpty()) {
            recordAuditLog(buyer.getProduct(), user, ChangeType.update, changedFields,
                    toJson(previousValues), toJson(newValues), request.getReason());
        }

        return toBuyerRequirementResponse(buyer);
    }

    public DeleteEntryResponse deleteBuyerRequirement(String userId, String productId, String buyerRequirementId) {
        User user = findUser(userId);
        requireRole(resolveRole(userId), "edit");
        Product product = findProductByUser(user, productId);
        BuyerRequirement buyer = buyerRequirementRepository.findByIdAndProductId(buyerRequirementId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "바이어 요구사항을 찾을 수 없습니다."));

        buyerRequirementRepository.delete(buyer);
        recordAuditLog(product, user, ChangeType.delete, List.of("buyerRequirement:" + buyer.getBuyerName()),
                null, null, "바이어 요구사항 삭제");

        return new DeleteEntryResponse("바이어 요구사항이 삭제되었습니다.", buyerRequirementId);
    }

    public KnowledgeSourceResponse createKnowledgeSource(String userId, String productId,
                                                           CreateKnowledgeSourceRequest request) {
        if ((request.getUrl() == null || request.getUrl().isBlank())
                && (request.getDocumentName() == null || request.getDocumentName().isBlank())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "url 또는 documentName 중 하나는 필수입니다.");
        }
        User user = findUser(userId);
        requireRole(resolveRole(userId), "edit");
        Product product = findProductByUser(user, productId);
        ProductKnowledge knowledge = knowledgeRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "제품 지식을 찾을 수 없습니다."));

        KnowledgeSource source = KnowledgeSource.builder()
                .knowledge(knowledge)
                .url(request.getUrl())
                .documentName(request.getDocumentName())
                .retrievedAt(parseDate(request.getRetrievedAt()))
                .build();
        knowledgeSourceRepository.save(source);

        recordAuditLog(product, user, ChangeType.create,
                List.of("url", "documentName", "retrievedAt"), null,
                toJson(Map.of(
                        "url", nullToEmpty(request.getUrl()),
                        "documentName", nullToEmpty(request.getDocumentName()),
                        "retrievedAt", nullToEmpty(request.getRetrievedAt()))),
                request.getReason() != null ? request.getReason() : "지식 출처 등록");

        return toKnowledgeSourceResponse(source, productId);
    }

    public DeleteEntryResponse deleteKnowledgeSource(String userId, String productId, String sourceId) {
        User user = findUser(userId);
        requireRole(resolveRole(userId), "edit");
        Product product = findProductByUser(user, productId);
        ProductKnowledge knowledge = knowledgeRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "제품 지식을 찾을 수 없습니다."));
        KnowledgeSource source = knowledgeSourceRepository.findByIdAndKnowledgeId(sourceId, knowledge.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "지식 출처를 찾을 수 없습니다."));

        knowledgeSourceRepository.delete(source);
        recordAuditLog(product, user, ChangeType.delete, List.of("knowledgeSource"), null, null, "지식 출처 삭제");

        return new DeleteEntryResponse("지식 출처가 삭제되었습니다.", sourceId);
    }

    @Transactional(readOnly = true)
    public KnowledgeLogsResponse getLogs(String userId, String productId) {
        User user = findUser(userId);
        requireRole(resolveRole(userId), "audit");
        findProductByUser(user, productId);
        List<AuditLog> logs = auditLogRepository.findByProductIdOrderByChangedAtDesc(productId);

        List<LogEntry> entries = logs.stream().map(logEntry -> LogEntry.builder()
                .logId(logEntry.getId())
                .changedBy(logEntry.getChangedBy().getName())
                .changeType(logEntry.getChangeType().name())
                .changedFields(logEntry.getChangedFields())
                .previousValue(logEntry.getPreviousValue())
                .newValue(logEntry.getNewValue())
                .reason(logEntry.getReason())
                .changedAt(logEntry.getChangedAt().format(ISO_FMT))
                .build()).collect(Collectors.toList());

        return KnowledgeLogsResponse.builder()
                .productId(productId)
                .logs(entries)
                .build();
    }

    private UserRole resolveRole(String userId) {
        return membershipRepository.findFirstByUserId(userId)
                .map(Membership::getRole)
                .orElse(UserRole.admin);
    }

    private Company resolveCompany(String userId) {
        return membershipRepository.findFirstByUserId(userId)
                .map(Membership::getCompany)
                .orElse(null);
    }

    private void requireRole(UserRole role, String action) {
        if (role == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "역할이 없는 계정입니다.");
        }
        switch (action) {
            case "edit":
                if (role == UserRole.viewer) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "이 회사 제품 지식을 수정할 권한이 없습니다.");
                }
                break;
            case "audit":
                if (role != UserRole.owner && role != UserRole.admin) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "감사 로그를 조회할 권한이 없습니다.");
                }
                break;
            default:
                break;
        }
    }

    private Product findProductByUser(User user, String productId) {
        Company company = resolveCompany(user.getId());
        if (company != null) {
            return productRepository.findByIdAndCompany_Id(productId, company.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "등록된 제품을 찾을 수 없습니다."));
        }
        return productRepository.findByIdAndOwnerId(productId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "등록된 제품을 찾을 수 없습니다."));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private String toJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Failed to serialize map to JSON");
            return null;
        }
    }

    private void recordAuditLog(Product product, User user, ChangeType type, List<String> changedFields,
                                 String previousValue, String newValue, String reason) {
        AuditLog log = AuditLog.builder()
                .product(product)
                .changedBy(user)
                .changeType(type)
                .changedFields(changedFields)
                .previousValue(previousValue)
                .newValue(newValue)
                .reason(reason)
                .build();
        auditLogRepository.save(log);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value, DATE_FMT);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "날짜 형식이 올바르지 않습니다. (yyyy-MM-dd)");
        }
    }

    private RiskLevel parseRiskLevel(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return RiskLevel.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "riskLevel 값이 올바르지 않습니다. (high|medium|low)");
        }
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private CertificationResponse toCertificationResponse(Certification c) {
        long days = c.getExpiresAt() != null ? ChronoUnit.DAYS.between(LocalDate.now(), c.getExpiresAt()) : 0;
        return CertificationResponse.builder()
                .id(c.getId())
                .productId(c.getProduct().getId())
                .name(c.getName())
                .issuedAt(c.getIssuedAt() != null ? c.getIssuedAt().format(DATE_FMT) : null)
                .expiresAt(c.getExpiresAt() != null ? c.getExpiresAt().format(DATE_FMT) : null)
                .renewalCycle(c.getRenewalCycle())
                .daysUntilExpiry(days)
                .build();
    }

    private FtoResponse toFtoResponse(Fto f) {
        return FtoResponse.builder()
                .id(f.getId())
                .productId(f.getProduct().getId())
                .targetCountry(f.getTargetCountry())
                .riskLevel(f.getRiskLevel() != null ? f.getRiskLevel().name() : null)
                .summary(f.getSummary())
                .expiresAt(f.getExpiresAt() != null ? f.getExpiresAt().format(DATE_FMT) : null)
                .build();
    }

    private BuyerRequirementResponse toBuyerRequirementResponse(BuyerRequirement b) {
        return BuyerRequirementResponse.builder()
                .id(b.getId())
                .productId(b.getProduct().getId())
                .buyerName(b.getBuyerName())
                .country(b.getCountry())
                .requirements(b.getRequirements())
                .build();
    }

    private KnowledgeSourceResponse toKnowledgeSourceResponse(KnowledgeSource s, String productId) {
        return KnowledgeSourceResponse.builder()
                .id(s.getId())
                .productId(productId)
                .url(s.getUrl())
                .documentName(s.getDocumentName())
                .retrievedAt(s.getRetrievedAt() != null ? s.getRetrievedAt().format(DATE_FMT) : null)
                .build();
    }
}
