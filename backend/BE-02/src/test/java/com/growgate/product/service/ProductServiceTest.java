package com.growgate.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.company.entity.Company;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.knowledge.entity.AuditLog;
import com.growgate.knowledge.entity.BuyerRequirement;
import com.growgate.knowledge.entity.Certification;
import com.growgate.knowledge.entity.Fto;
import com.growgate.knowledge.entity.ProductKnowledge;
import com.growgate.knowledge.enums.TrustGrade;
import com.growgate.knowledge.repository.AuditLogRepository;
import com.growgate.knowledge.repository.BuyerRequirementRepository;
import com.growgate.knowledge.repository.CertificationRepository;
import com.growgate.knowledge.repository.FtoRepository;
import com.growgate.knowledge.repository.ProductKnowledgeRepository;
import com.growgate.membership.entity.Membership;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.product.dto.CreateProductRequest;
import com.growgate.product.dto.DeleteProductResponse;
import com.growgate.product.dto.ProductListResponse;
import com.growgate.product.entity.Product;
import com.growgate.product.repository.ProductRepository;
import com.growgate.user.entity.User;
import com.growgate.user.enums.UserRole;
import com.growgate.user.enums.UserType;
import com.growgate.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductKnowledgeRepository knowledgeRepository;
    @Mock
    private CertificationRepository certificationRepository;
    @Mock
    private FtoRepository ftoRepository;
    @Mock
    private BuyerRequirementRepository buyerRequirementRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MembershipRepository membershipRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createStoresHsCodeAndTargetCountryForAuthenticatedUser() {
        ReflectionTestUtils.setField(productService, "objectMapper", new ObjectMapper());

        User user = user("user-1", UserType.personal);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-1")).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", "product-1");
            ReflectionTestUtils.setField(product, "createdAt", LocalDateTime.of(2026, 6, 21, 4, 0));
            ReflectionTestUtils.setField(product, "updatedAt", LocalDateTime.of(2026, 6, 21, 4, 0));
            return product;
        }).when(productRepository).save(any(Product.class));

        CreateProductRequest request = new CreateProductRequest();
        ReflectionTestUtils.setField(request, "name", "리튬배터리 팩");
        ReflectionTestUtils.setField(request, "category", "전기전자");
        ReflectionTestUtils.setField(request, "description", "충전식 전원 모듈");
        ReflectionTestUtils.setField(request, "hsCode", "850760");
        ReflectionTestUtils.setField(request, "targetCountry", "독일");

        productService.create("user-1", request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();

        assertThat(saved.getOwner()).isEqualTo(user);
        assertThat(saved.getCompany()).isNull();
        assertThat(saved.getHsCode()).isEqualTo("850760");
        assertThat(saved.getTargetCountry()).isEqualTo("독일");
    }

    @Test
    void createSucceedsForAdminRoleWithinCompany() {
        ReflectionTestUtils.setField(productService, "objectMapper", new ObjectMapper());

        User user = user("user-2", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(UserRole.admin).company(company).build();

        when(userRepository.findById("user-2")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-2")).thenReturn(Optional.of(membership));
        doAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", "product-2");
            ReflectionTestUtils.setField(product, "createdAt", LocalDateTime.of(2026, 6, 21, 4, 0));
            return product;
        }).when(productRepository).save(any(Product.class));

        CreateProductRequest request = new CreateProductRequest();
        ReflectionTestUtils.setField(request, "name", "전력 모듈");

        productService.create("user-2", request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getCompany()).isEqualTo(company);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"editor", "viewer"})
    void createIsForbiddenForEditorAndViewerRoles(UserRole role) {
        User user = user("user-3", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(role).company(company).build();

        when(userRepository.findById("user-3")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-3")).thenReturn(Optional.of(membership));

        CreateProductRequest request = new CreateProductRequest();
        ReflectionTestUtils.setField(request, "name", "전력 모듈");

        assertThatThrownBy(() -> productService.create("user-3", request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(productRepository, never()).save(any());
    }

    @Test
    void getListOnlyReturnsProductsOwnedByRequestingUserForPersonalAccount() {
        User user = user("user-1", UserType.personal);
        Product ownProduct = Product.builder()
                .id("product-1")
                .owner(user)
                .name("BMS 모듈")
                .updatedAt(LocalDateTime.of(2026, 6, 21, 4, 0))
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-1")).thenReturn(Optional.empty());
        when(productRepository.findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc("user-1")).thenReturn(List.of(ownProduct));
        when(knowledgeRepository.findByProductIdIn(List.of("product-1"))).thenReturn(List.of());
        when(certificationRepository.findByProductIdIn(List.of("product-1"))).thenReturn(List.of());
        when(ftoRepository.findByProductIdIn(List.of("product-1"))).thenReturn(List.of());
        when(buyerRequirementRepository.findByProductIdIn(List.of("product-1"))).thenReturn(List.of());

        ProductListResponse response = productService.getList("user-1");

        assertThat(response.getProducts()).extracting("productId").containsExactly("product-1");
        verify(productRepository).findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc("user-1");
        verify(productRepository, never()).findByCompany_IdAndDeletedAtIsNullOrderByUpdatedAtDesc(any());
    }

    @Test
    void getListOnlyReturnsProductsWithinRequestingUsersCompanyForCompanyAccount() {
        User user = user("user-2", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(UserRole.admin).company(company).build();
        Product companyProduct = Product.builder()
                .id("product-2")
                .owner(user)
                .company(company)
                .name("전력 모듈")
                .updatedAt(LocalDateTime.of(2026, 6, 21, 4, 0))
                .build();

        when(userRepository.findById("user-2")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-2")).thenReturn(Optional.of(membership));
        when(productRepository.findByCompany_IdAndDeletedAtIsNullOrderByUpdatedAtDesc("company-1")).thenReturn(List.of(companyProduct));
        when(knowledgeRepository.findByProductIdIn(List.of("product-2"))).thenReturn(List.of());
        when(certificationRepository.findByProductIdIn(List.of("product-2"))).thenReturn(List.of());
        when(ftoRepository.findByProductIdIn(List.of("product-2"))).thenReturn(List.of());
        when(buyerRequirementRepository.findByProductIdIn(List.of("product-2"))).thenReturn(List.of());

        ProductListResponse response = productService.getList("user-2");

        assertThat(response.getProducts()).extracting("productId").containsExactly("product-2");
        verify(productRepository).findByCompany_IdAndDeletedAtIsNullOrderByUpdatedAtDesc("company-1");
        verify(productRepository, never()).findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(any());
    }

    @Test
    void getListReturnsOnlyPersonalUserProductsWithTradeFields() {
        User user = user("user-1", UserType.personal);
        Product product = Product.builder()
                .id("product-1")
                .owner(user)
                .name("BMS 모듈")
                .category("전자부품")
                .description("보호회로 제어 모듈")
                .hsCode("853710")
                .targetCountry("미국")
                .updatedAt(LocalDateTime.of(2026, 6, 21, 4, 0))
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-1")).thenReturn(Optional.empty());
        when(productRepository.findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc("user-1")).thenReturn(List.of(product));
        when(knowledgeRepository.findByProductIdIn(List.of("product-1"))).thenReturn(List.of());
        when(certificationRepository.findByProductIdIn(List.of("product-1"))).thenReturn(List.of());
        when(ftoRepository.findByProductIdIn(List.of("product-1"))).thenReturn(List.of());
        when(buyerRequirementRepository.findByProductIdIn(List.of("product-1"))).thenReturn(List.of());

        ProductListResponse response = productService.getList("user-1");

        ProductListResponse.ProductSummary summary = response.getProducts().get(0);
        assertThat(summary.getProductId()).isEqualTo("product-1");
        assertThat(summary.getHsCode()).isEqualTo("853710");
        assertThat(summary.getTargetCountry()).isEqualTo("미국");
        assertThat(summary.getDescription()).isEqualTo("보호회로 제어 모듈");
        verify(productRepository).findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc("user-1");
    }

    @Test
    void getListIncludesKnowledgeRegistrationCountBasis() {
        User user = user("user-1", UserType.personal);
        Product product = Product.builder()
                .id("product-1")
                .owner(user)
                .name("BMS 모듈")
                .hsCode("853710")
                .targetCountry("미국")
                .updatedAt(LocalDateTime.of(2026, 6, 21, 4, 0))
                .build();
        ProductKnowledge knowledge = ProductKnowledge.builder()
                .product(product)
                .trustGrade(TrustGrade.high)
                .build();
        Certification certification = Certification.builder()
                .product(product)
                .name("UL")
                .expiresAt(LocalDate.now().plusDays(120))
                .build();
        Fto fto = Fto.builder()
                .product(product)
                .targetCountry("미국")
                .build();
        BuyerRequirement buyerRequirement = BuyerRequirement.builder()
                .product(product)
                .buyerName("Acme")
                .country("베트남")
                .requirements("UL 인증서")
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-1")).thenReturn(Optional.empty());
        when(productRepository.findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc("user-1")).thenReturn(List.of(product));
        when(knowledgeRepository.findByProductIdIn(List.of("product-1"))).thenReturn(List.of(knowledge));
        when(certificationRepository.findByProductIdIn(List.of("product-1"))).thenReturn(List.of(certification));
        when(ftoRepository.findByProductIdIn(List.of("product-1"))).thenReturn(List.of(fto));
        when(buyerRequirementRepository.findByProductIdIn(List.of("product-1"))).thenReturn(List.of(buyerRequirement));

        ProductListResponse response = productService.getList("user-1");

        ProductListResponse.ProductSummary summary = response.getProducts().get(0);
        assertThat(summary.getCertificationCount()).isEqualTo(1);
        assertThat(summary.getFtoCount()).isEqualTo(1);
        assertThat(summary.getBuyerRequirementCount()).isEqualTo(1);
        assertThat(summary.getBuyerName()).isEqualTo("Acme");
        assertThat(summary.getBuyerCountry()).isEqualTo("베트남");
        assertThat(summary.getBuyerRequest()).isEqualTo("UL 인증서");
        assertThat(summary.getMissingFieldCount()).isZero();
    }

    @Test
    void deleteSoftDeletesProductAndRecordsAuditLogForOwnerRole() {
        User user = user("user-1", UserType.personal);
        Product product = Product.builder()
                .id("product-1")
                .owner(user)
                .name("BMS 모듈")
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-1")).thenReturn(Optional.empty());
        when(productRepository.findByIdAndOwnerIdAndDeletedAtIsNull("product-1", "user-1"))
                .thenReturn(Optional.of(product));

        DeleteProductResponse response = productService.delete("user-1", "product-1");

        assertThat(response.getProductId()).isEqualTo("product-1");
        assertThat(product.getDeletedAt()).isNotNull();
        assertThat(product.getDeletedBy()).isEqualTo("user-1");

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getProduct()).isEqualTo(product);
        verify(productRepository).save(product);
    }

    @Test
    void deleteSoftDeletesProductForAdminRole() {
        User user = user("user-2", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(UserRole.admin).company(company).build();
        Product product = Product.builder()
                .id("product-2")
                .owner(user)
                .company(company)
                .name("전력 모듈")
                .build();

        when(userRepository.findById("user-2")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-2")).thenReturn(Optional.of(membership));
        when(productRepository.findByIdAndCompany_IdAndDeletedAtIsNull("product-2", "company-1"))
                .thenReturn(Optional.of(product));

        productService.delete("user-2", "product-2");

        assertThat(product.getDeletedAt()).isNotNull();
        verify(productRepository).save(product);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"editor", "viewer"})
    void deleteIsForbiddenForEditorAndViewerRoles(UserRole role) {
        User user = user("user-3", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(role).company(company).build();

        when(userRepository.findById("user-3")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-3")).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> productService.delete("user-3", "product-3"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(productRepository, never()).findByIdAndCompany_IdAndDeletedAtIsNull(any(), any());
        verify(auditLogRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteFailsWithNotFoundWhenProductIdBelongsToDifferentOwner() {
        User user = user("user-1", UserType.personal);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-1")).thenReturn(Optional.empty());
        when(productRepository.findByIdAndOwnerIdAndDeletedAtIsNull("other-owner-product", "user-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete("user-1", "other-owner-product"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);

        verify(auditLogRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteFailsWithNotFoundWhenProductIdBelongsToDifferentCompany() {
        User user = user("user-2", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(UserRole.admin).company(company).build();

        when(userRepository.findById("user-2")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-2")).thenReturn(Optional.of(membership));
        when(productRepository.findByIdAndCompany_IdAndDeletedAtIsNull("other-company-product", "company-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete("user-2", "other-company-product"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);

        verify(auditLogRepository, never()).save(any());
        verify(productRepository, never()).save(any());
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
