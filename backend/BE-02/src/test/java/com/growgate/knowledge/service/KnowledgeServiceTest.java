package com.growgate.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.company.entity.Company;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.knowledge.dto.KnowledgeLogsResponse;
import com.growgate.knowledge.dto.UpdateKnowledgeRequest;
import com.growgate.knowledge.dto.UpdateKnowledgeResponse;
import com.growgate.knowledge.repository.AuditLogRepository;
import com.growgate.knowledge.repository.BuyerRequirementRepository;
import com.growgate.knowledge.repository.CertificationRepository;
import com.growgate.knowledge.repository.FtoRepository;
import com.growgate.knowledge.repository.KnowledgeLoopItemRepository;
import com.growgate.knowledge.repository.KnowledgeSourceRepository;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

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
    private KnowledgeSourceRepository knowledgeSourceRepository;
    @Mock
    private KnowledgeLoopItemRepository knowledgeLoopItemRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MembershipRepository membershipRepository;

    @InjectMocks
    private KnowledgeService knowledgeService;

    @Test
    void getDetailFailsWithNotFoundWhenProductIdBelongsToDifferentCompany() {
        User user = user("user-2", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(UserRole.admin).company(company).build();

        when(userRepository.findById("user-2")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-2")).thenReturn(Optional.of(membership));
        when(productRepository.findByIdAndCompany_IdAndDeletedAtIsNull("other-company-product", "company-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeService.getDetail("user-2", "other-company-product"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void updateFailsWithNotFoundWhenProductIdBelongsToDifferentCompany() {
        User user = user("user-2", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(UserRole.admin).company(company).build();

        when(userRepository.findById("user-2")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-2")).thenReturn(Optional.of(membership));
        when(productRepository.findByIdAndCompany_IdAndDeletedAtIsNull("other-company-product", "company-1"))
                .thenReturn(Optional.empty());

        UpdateKnowledgeRequest request = new UpdateKnowledgeRequest();
        ReflectionTestUtils.setField(request, "name", "변경된 이름");

        assertThatThrownBy(() -> knowledgeService.update("user-2", "other-company-product", request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void updateIsForbiddenForViewerRole() {
        User user = user("user-3", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(UserRole.viewer).company(company).build();

        when(userRepository.findById("user-3")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-3")).thenReturn(Optional.of(membership));

        UpdateKnowledgeRequest request = new UpdateKnowledgeRequest();
        ReflectionTestUtils.setField(request, "name", "변경된 이름");

        assertThatThrownBy(() -> knowledgeService.update("user-3", "product-1", request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(productRepository, never()).findByIdAndCompany_IdAndDeletedAtIsNull(any(), any());
        verify(auditLogRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"owner", "admin", "editor"})
    void updateSucceedsForOwnerAdminAndEditorRoles(UserRole role) {
        ReflectionTestUtils.setField(knowledgeService, "objectMapper", new ObjectMapper());

        User user = user("user-4", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(role).company(company).build();
        Product product = Product.builder()
                .id("product-1")
                .owner(user)
                .company(company)
                .name("기존 이름")
                .updatedAt(LocalDateTime.of(2026, 6, 21, 4, 0))
                .build();

        when(userRepository.findById("user-4")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-4")).thenReturn(Optional.of(membership));
        when(productRepository.findByIdAndCompany_IdAndDeletedAtIsNull("product-1", "company-1"))
                .thenReturn(Optional.of(product));
        when(knowledgeRepository.findByProductId("product-1")).thenReturn(Optional.empty());

        UpdateKnowledgeRequest request = new UpdateKnowledgeRequest();
        ReflectionTestUtils.setField(request, "name", "변경된 이름");
        ReflectionTestUtils.setField(request, "reason", "정보 갱신");

        UpdateKnowledgeResponse response = knowledgeService.update("user-4", "product-1", request);

        assertThat(response.getProductId()).isEqualTo("product-1");
        assertThat(product.getName()).isEqualTo("변경된 이름");
        verify(auditLogRepository).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"owner", "admin"})
    void getLogsIsAllowedForOwnerAndAdminRoles(UserRole role) {
        User user = user("user-5", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(role).company(company).build();
        Product product = Product.builder().id("product-1").owner(user).company(company).build();

        when(userRepository.findById("user-5")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-5")).thenReturn(Optional.of(membership));
        when(productRepository.findByIdAndCompany_IdAndDeletedAtIsNull("product-1", "company-1"))
                .thenReturn(Optional.of(product));
        Page<com.growgate.knowledge.entity.AuditLog> emptyPage = new PageImpl<>(List.of());
        when(auditLogRepository.findByProductId(eq("product-1"), any(Pageable.class))).thenReturn(emptyPage);

        KnowledgeLogsResponse response = knowledgeService.getLogs("user-5", "product-1", 0, 20);

        assertThat(response.getProductId()).isEqualTo("product-1");
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"editor", "viewer"})
    void getLogsIsForbiddenForEditorAndViewerRoles(UserRole role) {
        User user = user("user-6", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(role).company(company).build();

        when(userRepository.findById("user-6")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-6")).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> knowledgeService.getLogs("user-6", "product-1", 0, 20))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(productRepository, never()).findByIdAndCompany_IdAndDeletedAtIsNull(any(), any());
        verify(auditLogRepository, never()).findByProductId(any(), any());
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
