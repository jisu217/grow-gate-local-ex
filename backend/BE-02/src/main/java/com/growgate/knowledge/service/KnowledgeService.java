package com.growgate.knowledge.service;

// 제품 지식 CRUD + 감사 로그
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.knowledge.dto.CreateKnowledgeRequest;
import com.growgate.knowledge.dto.CreateKnowledgeResponse;
import com.growgate.knowledge.dto.DeleteKnowledgeResponse;
import com.growgate.knowledge.dto.KnowledgeDetailResponse;
import com.growgate.knowledge.dto.KnowledgeDetailResponse.BuyerRequirementData;
import com.growgate.knowledge.dto.KnowledgeDetailResponse.CertificationData;
import com.growgate.knowledge.dto.KnowledgeDetailResponse.FtoData;
import com.growgate.knowledge.dto.KnowledgeDetailResponse.KnowledgeData;
import com.growgate.knowledge.dto.KnowledgeListResponse;
import com.growgate.knowledge.dto.KnowledgeListResponse.ProductSummary;
import com.growgate.knowledge.dto.KnowledgeLogsResponse;
import com.growgate.knowledge.dto.KnowledgeLogsResponse.LogEntry;
import com.growgate.knowledge.dto.UpdateKnowledgeRequest;
import com.growgate.knowledge.dto.UpdateKnowledgeResponse;
import com.growgate.knowledge.entity.AuditLog;
import com.growgate.knowledge.entity.BuyerRequirement;
import com.growgate.knowledge.entity.Certification;
import com.growgate.knowledge.entity.Fto;
import com.growgate.knowledge.entity.ProductKnowledge;
import com.growgate.knowledge.enums.ChangeType;
import com.growgate.knowledge.enums.TrustGrade;
import com.growgate.knowledge.repository.AuditLogRepository;
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
public class KnowledgeService {

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int EXPIRY_WARNING_DAYS = 90;

    private final ProductRepository productRepository;
    private final ProductKnowledgeRepository knowledgeRepository;
    private final CertificationRepository certificationRepository;
    private final FtoRepository ftoRepository;
    private final BuyerRequirementRepository buyerRequirementRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public KnowledgeListResponse getList(String userId) {
        User user = findUser(userId);
        List<Product> products = findProductsByUser(user);

        List<ProductSummary> summaries = products.stream().map(product -> {
            ProductKnowledge knowledge = knowledgeRepository.findByProductId(product.getId()).orElse(null);
            List<Certification> certs = certificationRepository.findByProductId(product.getId());
            List<Fto> ftoList = ftoRepository.findByProductId(product.getId());
            List<BuyerRequirement> buyers = buyerRequirementRepository.findByProductId(product.getId());

            LocalDate today = LocalDate.now();

            boolean hasExpired = certs.stream()
                    .anyMatch(c -> c.getExpiresAt() != null && c.getExpiresAt().isBefore(today));

            int expiringCertCount = (int) certs.stream()
                    .filter(c -> c.getExpiresAt() != null &&
                            ChronoUnit.DAYS.between(today, c.getExpiresAt()) <= EXPIRY_WARNING_DAYS &&
                            !c.getExpiresAt().isBefore(today))
                    .count();

            int missingFieldCount = 0;
            if (ftoList.isEmpty()) missingFieldCount++;
            if (buyers.isEmpty()) missingFieldCount++;

            String trustGrade = (knowledge != null && knowledge.getTrustGrade() != null)
                    ? knowledge.getTrustGrade().name() : null;

            String knowledgeStatus = computeKnowledgeStatus(
                    hasExpired, expiringCertCount, missingFieldCount, knowledge, certs);

            return ProductSummary.builder()
                    .productId(product.getId())
                    .name(product.getName())
                    .category(product.getCategory())
                    .trustGrade(trustGrade)
                    .knowledgeStatus(knowledgeStatus)
                    .expiringCertCount(expiringCertCount)
                    .missingFieldCount(missingFieldCount)
                    .updatedAt(product.getUpdatedAt().format(ISO_FMT))
                    .build();
        }).collect(Collectors.toList());

        return KnowledgeListResponse.builder().products(summaries).build();
    }

    public CreateKnowledgeResponse create(String userId, CreateKnowledgeRequest request) {
        User user = findUser(userId);
        requireRole(user, "create");

        Product product = Product.builder()
                .owner(user)
                .name(request.getName())
                .category(request.getCategory())
                .description(request.getDescription())
                .build();
        productRepository.save(product);

        ProductKnowledge knowledge = ProductKnowledge.builder()
                .product(product)
                .createdBy(user)
                .updatedBy(user)
                .build();
        knowledgeRepository.save(knowledge);

        AuditLog log = AuditLog.builder()
                .product(product)
                .changedBy(user)
                .changeType(ChangeType.create)
                .changedFields(List.of("name", "category", "description"))
                .previousValue(null)
                .newValue(toJson(Map.of("name", request.getName(),
                        "category", request.getCategory() != null ? request.getCategory() : "",
                        "description", request.getDescription() != null ? request.getDescription() : "")))
                .reason("신규 제품 지식 등록")
                .build();
        auditLogRepository.save(log);

        return CreateKnowledgeResponse.builder()
                .productId(product.getId())
                .name(product.getName())
                .createdAt(product.getCreatedAt().format(ISO_FMT))
                .build();
    }

    @Transactional(readOnly = true)
    public KnowledgeDetailResponse getDetail(String userId, String productId) {
        User user = findUser(userId);
        Product product = findProductByUser(user, productId);
        ProductKnowledge knowledge = knowledgeRepository.findByProductId(productId).orElse(null);
        List<Certification> certs = certificationRepository.findByProductId(productId);
        List<Fto> ftoList = ftoRepository.findByProductId(productId);
        List<BuyerRequirement> buyers = buyerRequirementRepository.findByProductId(productId);

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

        return KnowledgeDetailResponse.builder()
                .productId(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .description(product.getDescription())
                .knowledge(knowledgeData)
                .certifications(certData)
                .fto(ftoData)
                .buyerRequirements(buyerData)
                .updatedAt(product.getUpdatedAt().format(ISO_FMT))
                .build();
    }

    public UpdateKnowledgeResponse update(String userId, String productId, UpdateKnowledgeRequest request) {
        User user = findUser(userId);
        requireRole(user, "edit");
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
        if (request.getDescription() != null && !request.getDescription().equals(product.getDescription())) {
            changedFields.add("description");
            previousValues.put("description", product.getDescription());
            newValues.put("description", request.getDescription());
        }

        product.update(request.getName(), request.getCategory(), request.getDescription());

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

    public DeleteKnowledgeResponse delete(String userId, String productId) {
        User user = findUser(userId);
        requireRole(user, "delete");
        Product product = findProductByUser(user, productId);

        AuditLog log = AuditLog.builder()
                .product(product)
                .changedBy(user)
                .changeType(ChangeType.delete)
                .changedFields(List.of())
                .reason("제품 삭제")
                .build();
        auditLogRepository.save(log);

        certificationRepository.deleteAll(certificationRepository.findByProductId(productId));
        ftoRepository.deleteAll(ftoRepository.findByProductId(productId));
        buyerRequirementRepository.deleteAll(buyerRequirementRepository.findByProductId(productId));
        knowledgeRepository.findByProductId(productId).ifPresent(knowledgeRepository::delete);
        productRepository.delete(product);

        return new DeleteKnowledgeResponse("제품이 삭제되었습니다.", productId);
    }

    @Transactional(readOnly = true)
    public KnowledgeLogsResponse getLogs(String userId, String productId) {
        User user = findUser(userId);
        requireRole(user, "audit");
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

    private void requireRole(User user, String action) {
        UserRole role = user.getRole();
        if (role == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "역할이 없는 계정입니다.");
        }
        switch (action) {
            case "create":
            case "delete":
                if (role != UserRole.owner && role != UserRole.admin) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "이 작업을 수행할 권한이 없습니다.");
                }
                break;
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

    private String computeKnowledgeStatus(boolean hasExpired, int expiringCertCount,
                                           int missingFieldCount, ProductKnowledge knowledge,
                                           List<Certification> certs) {
        if (hasExpired) return "expired";
        if (expiringCertCount > 0) return "expiring_soon";
        if (missingFieldCount > 0) return "needs_review";
        if (knowledge != null && knowledge.getTrustGrade() != null && !certs.isEmpty()) return "valid";
        return "incomplete";
    }

    private List<Product> findProductsByUser(User user) {
        if (user.getCompany() != null) {
            return productRepository.findByOwner_Company_IdOrderByUpdatedAtDesc(user.getCompany().getId());
        }
        return productRepository.findByOwnerIdOrderByUpdatedAtDesc(user.getId());
    }

    private Product findProductByUser(User user, String productId) {
        if (user.getCompany() != null) {
            return productRepository.findByIdAndOwner_Company_Id(productId, user.getCompany().getId())
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
}
