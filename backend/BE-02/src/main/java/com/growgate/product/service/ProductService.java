package com.growgate.product.service;

// 제품 CRUD (목록/생성/삭제)
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.common.DateTimeUtils;
import com.growgate.company.entity.Company;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.knowledge.entity.AuditLog;
import com.growgate.knowledge.entity.BuyerRequirement;
import com.growgate.knowledge.entity.Certification;
import com.growgate.knowledge.entity.Fto;
import com.growgate.knowledge.entity.ProductKnowledge;
import com.growgate.knowledge.enums.ChangeType;
import com.growgate.knowledge.repository.AuditLogRepository;
import com.growgate.knowledge.repository.BuyerRequirementRepository;
import com.growgate.knowledge.repository.CertificationRepository;
import com.growgate.knowledge.repository.FtoRepository;
import com.growgate.knowledge.repository.ProductKnowledgeRepository;
import com.growgate.membership.entity.Membership;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.product.dto.CreateProductRequest;
import com.growgate.product.dto.CreateProductResponse;
import com.growgate.product.dto.DeleteProductResponse;
import com.growgate.product.dto.ProductListResponse;
import com.growgate.product.dto.ProductListResponse.ProductSummary;
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
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private static final int EXPIRY_WARNING_DAYS = 90;

    private final ProductRepository productRepository;
    private final ProductKnowledgeRepository knowledgeRepository;
    private final CertificationRepository certificationRepository;
    private final FtoRepository ftoRepository;
    private final BuyerRequirementRepository buyerRequirementRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ProductListResponse getList(String userId) {
        User user = findUser(userId);
        List<Product> products = findProductsByUser(user);
        List<String> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        Map<String, ProductKnowledge> knowledgeByProductId = knowledgeRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(k -> k.getProduct().getId(), k -> k));
        Map<String, List<Certification>> certsByProductId = certificationRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(c -> c.getProduct().getId()));
        Map<String, List<Fto>> ftoByProductId = ftoRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(f -> f.getProduct().getId()));
        Map<String, List<BuyerRequirement>> buyersByProductId = buyerRequirementRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(b -> b.getProduct().getId()));

        List<ProductSummary> summaries = products.stream().map(product -> {
            ProductKnowledge knowledge = knowledgeByProductId.get(product.getId());
            List<Certification> certs = certsByProductId.getOrDefault(product.getId(), Collections.emptyList());
            List<Fto> ftoList = ftoByProductId.getOrDefault(product.getId(), Collections.emptyList());
            List<BuyerRequirement> buyers = buyersByProductId.getOrDefault(product.getId(), Collections.emptyList());

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
                    .hsCode(product.getHsCode())
                    .targetCountry(product.getTargetCountry())
                    .description(product.getDescription())
                    .trustGrade(trustGrade)
                    .knowledgeStatus(knowledgeStatus)
                    .expiringCertCount(expiringCertCount)
                    .missingFieldCount(missingFieldCount)
                    .updatedAt(DateTimeUtils.formatUtc(product.getUpdatedAt()))
                    .build();
        }).collect(Collectors.toList());

        return ProductListResponse.builder().products(summaries).build();
    }

    public CreateProductResponse create(String userId, CreateProductRequest request) {
        User user = findUser(userId);
        requireRole(resolveRole(user), "create");

        Product product = Product.builder()
                .owner(user)
                .company(resolveCompany(userId))
                .name(request.getName())
                .category(request.getCategory())
                .hsCode(request.getHsCode())
                .targetCountry(request.getTargetCountry())
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
                .changedFields(List.of("name", "category", "hsCode", "targetCountry", "description"))
                .previousValue(null)
                .newValue(toJson(Map.of("name", request.getName(),
                        "category", request.getCategory() != null ? request.getCategory() : "",
                        "hsCode", request.getHsCode() != null ? request.getHsCode() : "",
                        "targetCountry", request.getTargetCountry() != null ? request.getTargetCountry() : "",
                        "description", request.getDescription() != null ? request.getDescription() : "")))
                .reason("신규 제품 지식 등록")
                .build();
        auditLogRepository.save(log);

        return CreateProductResponse.builder()
                .productId(product.getId())
                .name(product.getName())
                .createdAt(DateTimeUtils.formatUtc(product.getCreatedAt()))
                .build();
    }

    public DeleteProductResponse delete(String userId, String productId) {
        User user = findUser(userId);
        requireRole(resolveRole(user), "delete");
        Product product = findProductByUser(user, productId);

        AuditLog log = AuditLog.builder()
                .product(product)
                .changedBy(user)
                .changeType(ChangeType.delete)
                .changedFields(List.of())
                .reason("제품 삭제")
                .build();
        auditLogRepository.save(log);

        product.softDelete(user.getId());
        productRepository.save(product);

        return new DeleteProductResponse("제품이 삭제 처리되었습니다. 감사 로그와 진단 이력은 유지됩니다.", productId);
    }

    private UserRole resolveRole(User user) {
        return membershipRepository.findFirstByUserId(user.getId())
                .map(Membership::getRole)
                .orElse(user.getUserType().name().equals("personal") ? UserRole.owner : null);
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
            case "create":
            case "delete":
                if (role != UserRole.owner && role != UserRole.admin) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "이 작업을 수행할 권한이 없습니다.");
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
        Company company = resolveCompany(user.getId());
        if (company != null) {
            return productRepository.findByCompany_IdAndDeletedAtIsNullOrderByUpdatedAtDesc(company.getId());
        }
        return productRepository.findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(user.getId());
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
