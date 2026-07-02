package com.growgate.diagnosis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.company.entity.Company;
import com.growgate.diagnosis.dto.CreateDiagnosisRequest;
import com.growgate.diagnosis.dto.CreateDiagnosisResponse;
import com.growgate.diagnosis.repository.DiagnosisRepository;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.knowledge.repository.BuyerRequirementRepository;
import com.growgate.knowledge.repository.CertificationRepository;
import com.growgate.knowledge.repository.FtoRepository;
import com.growgate.knowledge.repository.ProductKnowledgeRepository;
import com.growgate.membership.entity.Membership;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.product.entity.Product;
import com.growgate.product.repository.ProductRepository;
import com.growgate.user.entity.User;
import com.growgate.user.enums.UserRole;
import com.growgate.user.enums.UserType;
import com.growgate.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiagnosisServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DiagnosisRepository diagnosisRepository;
    private UserRepository userRepository;
    private MembershipRepository membershipRepository;
    private ProductRepository productRepository;
    private ProductKnowledgeRepository knowledgeRepository;
    private CertificationRepository certificationRepository;
    private FtoRepository ftoRepository;
    private BuyerRequirementRepository buyerRequirementRepository;
    private DiagnosisAsyncProcessor asyncProcessor;
    private DiagnosisService diagnosisService;

    private void setUp() {
        diagnosisRepository = mock(DiagnosisRepository.class);
        userRepository = mock(UserRepository.class);
        membershipRepository = mock(MembershipRepository.class);
        productRepository = mock(ProductRepository.class);
        knowledgeRepository = mock(ProductKnowledgeRepository.class);
        certificationRepository = mock(CertificationRepository.class);
        ftoRepository = mock(FtoRepository.class);
        buyerRequirementRepository = mock(BuyerRequirementRepository.class);
        asyncProcessor = mock(DiagnosisAsyncProcessor.class);
        diagnosisService = new DiagnosisService(
                diagnosisRepository,
                userRepository,
                membershipRepository,
                productRepository,
                knowledgeRepository,
                certificationRepository,
                ftoRepository,
                buyerRequirementRepository,
                objectMapper,
                asyncProcessor
        );
    }

    @Test
    void createFailsWithNotFoundWhenProductIdBelongsToDifferentCompany() {
        setUp();
        User user = user("user-2", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(UserRole.admin).company(company).build();

        when(userRepository.findById("user-2")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-2")).thenReturn(Optional.of(membership));
        when(productRepository.findByIdAndCompany_IdAndDeletedAtIsNull("other-company-product", "company-1"))
                .thenReturn(Optional.empty());

        CreateDiagnosisRequest request = new CreateDiagnosisRequest();
        ReflectionTestUtils.setField(request, "productId", "other-company-product");
        ReflectionTestUtils.setField(request, "targetCountry", "미국");

        assertThatThrownBy(() -> diagnosisService.create("user-2", request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);

        verify(diagnosisRepository, never()).save(any());
        verify(asyncProcessor, never()).process(any());
    }

    @Test
    void createIsForbiddenForViewerRole() {
        setUp();
        User user = user("user-3", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(UserRole.viewer).company(company).build();

        when(userRepository.findById("user-3")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-3")).thenReturn(Optional.of(membership));

        CreateDiagnosisRequest request = new CreateDiagnosisRequest();
        ReflectionTestUtils.setField(request, "productId", "product-1");
        ReflectionTestUtils.setField(request, "targetCountry", "미국");

        assertThatThrownBy(() -> diagnosisService.create("user-3", request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(productRepository, never()).findByIdAndCompany_IdAndDeletedAtIsNull(any(), any());
        verify(diagnosisRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"owner", "admin", "editor"})
    void createSucceedsForOwnerAdminAndEditorRolesAndDispatchesAsyncProcessing(UserRole role) {
        setUp();
        User user = user("user-4", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(role).company(company).build();
        Product product = Product.builder().id("product-1").owner(user).company(company).name("BMS 모듈").build();

        when(userRepository.findById("user-4")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-4")).thenReturn(Optional.of(membership));
        when(productRepository.findByIdAndCompany_IdAndDeletedAtIsNull("product-1", "company-1"))
                .thenReturn(Optional.of(product));
        when(knowledgeRepository.findByProductId("product-1")).thenReturn(Optional.empty());
        when(certificationRepository.findByProductId("product-1")).thenReturn(List.of());
        when(ftoRepository.findByProductId("product-1")).thenReturn(List.of());
        when(buyerRequirementRepository.findByProductId("product-1")).thenReturn(List.of());

        CreateDiagnosisRequest request = new CreateDiagnosisRequest();
        ReflectionTestUtils.setField(request, "productId", "product-1");
        ReflectionTestUtils.setField(request, "targetCountry", "미국");

        CreateDiagnosisResponse response = diagnosisService.create("user-4", request);

        assertThat(response.getStatus()).isEqualTo("pending");
        verify(diagnosisRepository).save(any());
        verify(asyncProcessor).process(any());
    }

    private User user(String id, UserType type) {
        User user = User.builder()
                .id(id)
                .email(id + "@growgate.local")
                .passwordHash("encoded")
                .name("테스트 사용자")
                .userType(type)
                .build();
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 6, 21, 4, 0));
        ReflectionTestUtils.setField(user, "updatedAt", LocalDateTime.of(2026, 6, 21, 4, 0));
        return user;
    }
}
