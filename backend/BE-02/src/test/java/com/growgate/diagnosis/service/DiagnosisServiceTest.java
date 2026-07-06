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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;
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
        Product product = Product.builder()
                .id("product-1")
                .owner(user)
                .company(company)
                .name("BMS 모듈")
                .hsCode("850760")
                .targetCountry("US")
                .description("battery management system")
                .build();

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

    @Test
    void createRejectsDirectInputWithoutProductNameAndHsCode() {
        setUp();
        User user = user("user-5", UserType.personal);

        when(userRepository.findById("user-5")).thenReturn(Optional.of(user));

        CreateDiagnosisRequest.InputData inputData = new CreateDiagnosisRequest.InputData();
        ReflectionTestUtils.setField(inputData, "buyerRequest", "CE 보유 여부 확인 요청");

        CreateDiagnosisRequest request = new CreateDiagnosisRequest();
        ReflectionTestUtils.setField(request, "targetCountry", "DE");
        ReflectionTestUtils.setField(request, "inputData", inputData);

        assertThatThrownBy(() -> diagnosisService.create("user-5", request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(diagnosisRepository, never()).save(any());
    }

    @Test
    void createWithProductIdStoresProductSnapshotAndBuyerContext() throws Exception {
        setUp();
        User user = user("user-6", UserType.company);
        Company company = Company.builder().id("company-1").build();
        Membership membership = Membership.builder().role(UserRole.editor).company(company).build();
        Product product = Product.builder()
                .id("product-2")
                .owner(user)
                .company(company)
                .name("리튬 배터리팩")
                .hsCode("850760")
                .targetCountry("DE")
                .description("산업용 배터리팩")
                .build();

        when(userRepository.findById("user-6")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("user-6")).thenReturn(Optional.of(membership));
        when(productRepository.findByIdAndCompany_IdAndDeletedAtIsNull("product-2", "company-1"))
                .thenReturn(Optional.of(product));
        when(knowledgeRepository.findByProductId("product-2")).thenReturn(Optional.empty());
        when(certificationRepository.findByProductId("product-2")).thenReturn(List.of());
        when(ftoRepository.findByProductId("product-2")).thenReturn(List.of());
        when(buyerRequirementRepository.findByProductId("product-2")).thenReturn(List.of());

        CreateDiagnosisRequest.InputData inputData = new CreateDiagnosisRequest.InputData();
        ReflectionTestUtils.setField(inputData, "buyerRequest", "독일 바이어가 CE와 UN38.3 증빙을 요청");
        ReflectionTestUtils.setField(inputData, "requestedCertifications", List.of("CE", "UN38.3"));

        CreateDiagnosisRequest request = new CreateDiagnosisRequest();
        ReflectionTestUtils.setField(request, "productId", "product-2");
        ReflectionTestUtils.setField(request, "targetCountry", "DE");
        ReflectionTestUtils.setField(request, "inputData", inputData);

        diagnosisService.create("user-6", request);

        var captor = forClass(com.growgate.diagnosis.entity.DiagnosisRun.class);
        verify(diagnosisRepository).save(captor.capture());
        var savedRun = captor.getValue();
        var inputMap = objectMapper.readValue(savedRun.getInputData(), java.util.Map.class);
        var snapshotMap = objectMapper.readValue(savedRun.getKnowledgeSnapshot(), java.util.Map.class);

        assertThat(inputMap.get("buyerRequest")).isEqualTo("독일 바이어가 CE와 UN38.3 증빙을 요청");
        assertThat(snapshotMap.get("productName")).isEqualTo("리튬 배터리팩");
        assertThat(snapshotMap.get("hsCode")).isEqualTo("850760");
        assertThat(snapshotMap.get("targetCountry")).isEqualTo("DE");
        assertThat(snapshotMap.get("buyerRequest")).isEqualTo("독일 바이어가 CE와 UN38.3 증빙을 요청");
    }

    @Test
    void getDetailPreservesEvidenceContractFieldsFromResultJson() throws Exception {
        setUp();
        User user = user("user-detail", UserType.personal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "evidence contract result");
        result.put("sourceTrustSummary", Map.of(
                        "liveCount", 1,
                        "operationEndpointLiveCount", 1,
                        "apiKeyConfiguredLiveCount", 1,
                        "providerHealth", List.of(Map.of(
                                "provider", "KCS_TRADE",
                                "operationEndpoint", true,
                                "apiKeyConfigured", true,
                                "endpointType", "operation"
                        ))
                ));
        result.put("regionalIndustryProfile", Map.of("region", "경남"));
        result.put("buyerRequestTrace", Map.of("matchedKeywords", List.of("CE")));
        result.put("scoreContributionLedger", List.of(Map.of(
                        "sourceId", "KCS_TRADE-record-1",
                        "provider", "KCS_TRADE",
                        "operationEndpoint", true,
                        "apiKeyConfigured", true,
                        "finalScoreDelta", 7
                )));
        result.put("pythonWorker", Map.of("status", "disabled"));
        result.put("pythonAgenticLoop", Map.of("mode", "python-planning-tool-verification-retry"));
        result.put("normalizedMetrics", Map.of("market", Map.of("recordCount", 1)));
        result.put("compiledWiki", Map.of("knowledgePage", Map.of("title", "850760-DE")));
        result.put("scoringRubric", Map.of("version", "rule-v1"));
        result.put("agentInference", Map.of("enabled", false));
        result.put("sourceTrace", Map.of("sourceCount", 2, "sourceTypes", List.of("live", "fallback")));
        result.put("llmFallbackReasons", List.of("agent disabled", "blocked source-less claim"));
        result.put("claimVerifications", List.of(Map.of(
                "claim", "KCS source 기반 시장 신호입니다.",
                "status", "PASS",
                "sourceId", "KCS_TRADE-record-1"
        )));
        result.put("knowledgeLoopSuggestions", List.of(Map.of("title", "CE evidence memo")));
        result.put("agentExplanationSummary", Map.of("candidateCount", 1, "approvedKnowledgeOnlyReusable", true));
        result.put("agentExplanations", List.of(Map.of(
                "agent", "REGULATION_CERTIFICATION",
                "knowledgeStatus", "PENDING_REVIEW"
        )));
        result.put("agentKnowledgeCandidates", List.of(Map.of(
                "agent", "REGULATION_CERTIFICATION",
                "knowledgeType", "REGULATION_CHECKLIST",
                "status", "PENDING_REVIEW",
                "sourceIds", List.of("KCS_TRADE-record-1")
        )));
        result.put("publicDataCollectionRunId", "run-1");
        result.put("inputTrace", Map.of("hsCode", "850760"));
        var run = diagnosisRun("diag-detail", user, objectMapper.writeValueAsString(result));

        when(userRepository.findById("user-detail")).thenReturn(Optional.of(user));
        when(diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull("diag-detail", "user-detail")).thenReturn(Optional.of(run));

        var response = diagnosisService.getDetail("user-detail", "diag-detail");

        assertThat(response.getResult().getSourceTrustSummary()).containsEntry("liveCount", 1);
        assertThat(response.getResult().getSourceTrustSummary()).containsEntry("operationEndpointLiveCount", 1);
        assertThat(response.getResult().getScoreContributionLedger()).hasSize(1);
        assertThat(response.getResult().getScoreContributionLedger().get(0)).containsEntry("operationEndpoint", true);
        assertThat(response.getResult().getNormalizedMetrics()).containsKey("market");
        assertThat(response.getResult().getPublicDataCollectionRunId()).isEqualTo("run-1");
        assertThat(response.getResult().getInputTrace()).containsEntry("hsCode", "850760");
        assertThat(response.getResult().getRegionalIndustryProfile()).containsEntry("region", "경남");
        assertThat(response.getResult().getSourceTrace()).containsEntry("sourceCount", 2);
        assertThat(response.getResult().getLlmFallbackReasons()).contains("blocked source-less claim");
        assertThat(response.getResult().getClaimVerifications()).hasSize(1);
        assertThat(response.getResult().getClaimVerifications().get(0)).containsEntry("status", "PASS");
        assertThat(response.getResult().getKnowledgeLoopSuggestions()).hasSize(1);
        assertThat(response.getResult().getAgentExplanationSummary()).containsEntry("candidateCount", 1);
        assertThat(response.getResult().getAgentExplanations()).hasSize(1);
        assertThat(response.getResult().getAgentKnowledgeCandidates().get(0))
                .containsEntry("knowledgeType", "REGULATION_CHECKLIST")
                .containsEntry("status", "PENDING_REVIEW");
    }

    @Test
    void getDetailAllowsWatermarkedSyntheticDemoDiagnosisOutsideCurrentCompany() throws Exception {
        setUp();
        User user = user("admin-user", UserType.company);
        Company currentCompany = Company.builder().id("current-company").build();
        Company scenarioCompany = Company.builder().id("scenario-company").build();
        Membership membership = Membership.builder().role(UserRole.admin).company(currentCompany).build();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("syntheticData", true);
        result.put("demoWatermark", "synthetic_demo_not_real_customer");
        result.put("summary", "시연용 진단 결과");
        var run = diagnosisRun("synthetic-demo-diag", user("scenario-owner", UserType.company),
                objectMapper.writeValueAsString(result));
        run.setCompany(scenarioCompany);

        when(userRepository.findById("admin-user")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("admin-user")).thenReturn(Optional.of(membership));
        when(diagnosisRepository.findByIdAndCompanyIdAndDeletedAtIsNull("synthetic-demo-diag", "current-company"))
                .thenReturn(Optional.empty());
        when(diagnosisRepository.findById("synthetic-demo-diag")).thenReturn(Optional.of(run));

        var response = diagnosisService.getDetail("admin-user", "synthetic-demo-diag");

        assertThat(response.getId()).isEqualTo("synthetic-demo-diag");
        assertThat(response.getResult().getSummary()).isEqualTo("시연용 진단 결과");
    }

    @Test
    void getDetailKeepsRealDiagnosisOutsideCurrentCompanyHidden() throws Exception {
        setUp();
        User user = user("admin-user", UserType.company);
        Company currentCompany = Company.builder().id("current-company").build();
        Company otherCompany = Company.builder().id("other-company").build();
        Membership membership = Membership.builder().role(UserRole.admin).company(currentCompany).build();
        Map<String, Object> result = Map.of("summary", "실제 고객 진단 결과");
        var run = diagnosisRun("real-other-company-diag", user("other-owner", UserType.company),
                objectMapper.writeValueAsString(result));
        run.setCompany(otherCompany);

        when(userRepository.findById("admin-user")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("admin-user")).thenReturn(Optional.of(membership));
        when(diagnosisRepository.findByIdAndCompanyIdAndDeletedAtIsNull("real-other-company-diag", "current-company"))
                .thenReturn(Optional.empty());
        when(diagnosisRepository.findById("real-other-company-diag")).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> diagnosisService.getDetail("admin-user", "real-other-company-diag"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void deleteDiagnosisMarksOwnedPersonalRunDeleted() throws Exception {
        setUp();
        User user = user("delete-user", UserType.personal);
        Map<String, Object> result = Map.of("summary", "삭제 가능한 실제 진단");
        var run = diagnosisRun("delete-diag", user, objectMapper.writeValueAsString(result));

        when(userRepository.findById("delete-user")).thenReturn(Optional.of(user));
        when(diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull("delete-diag", "delete-user"))
                .thenReturn(Optional.of(run));

        diagnosisService.delete("delete-user", "delete-diag");

        assertThat(run.getDeletedAt()).isNotNull();
        assertThat(run.getDeletedBy()).isEqualTo("delete-user");
        verify(diagnosisRepository).save(run);
    }

    @Test
    void deleteDiagnosisRejectsWatermarkedSyntheticDemoRun() throws Exception {
        setUp();
        User user = user("delete-user", UserType.personal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("demoWatermark", "synthetic_demo_not_real_customer");
        result.put("summary", "합성 시연 seed");
        var run = diagnosisRun("synthetic-delete-diag", user, objectMapper.writeValueAsString(result));

        when(userRepository.findById("delete-user")).thenReturn(Optional.of(user));
        when(diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull("synthetic-delete-diag", "delete-user"))
                .thenReturn(Optional.of(run));

        assertThatThrownBy(() -> diagnosisService.delete("delete-user", "synthetic-delete-diag"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        assertThat(run.getDeletedAt()).isNull();
        verify(diagnosisRepository, never()).save(any());
    }

    @Test
    void getDetailExposesRuleOwnedGateDecisionAndDoesNotLetLlmOverrideFinalGate() throws Exception {
        setUp();
        User user = user("user-gate", UserType.personal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("diagnosisScore", Map.of("overall", 82, "market", 80, "regulation", 78, "recall", 74, "fto", 76, "support", 86));
        result.put("gateDecision", Map.of(
                "status", "CONDITIONAL_GO",
                "finalGate", "CONDITIONAL_GO",
                "ruleScore", 82,
                "riskLevel", "medium",
                "fallbackCount", 1,
                "reasons", List.of("fallback 1건으로 담당자 검토 필요"),
                "llmInfluencedGate", false,
                "llmDecisionPolicy", "LLM output is advisory and cannot override deterministic Gate"
        ));
        result.put("agentInference", Map.of(
                "succeeded", true,
                "summary", "LLM suggested GO, but this must not alter finalGate"
        ));
        var run = diagnosisRun("diag-gate", user, objectMapper.writeValueAsString(result));

        when(userRepository.findById("user-gate")).thenReturn(Optional.of(user));
        when(diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull("diag-gate", "user-gate")).thenReturn(Optional.of(run));

        var response = diagnosisService.getDetail("user-gate", "diag-gate");

        assertThat(response.getResult().getGateDecision().getStatus()).isEqualTo("CONDITIONAL_GO");
        assertThat(response.getResult().getGateDecision().getFinalGate()).isEqualTo("CONDITIONAL_GO");
        assertThat(response.getResult().getGateDecision().getRuleScore()).isEqualTo(82);
        assertThat(response.getResult().getGateDecision().getRiskLevel()).isEqualTo("medium");
        assertThat(response.getResult().getGateDecision().getFallbackCount()).isEqualTo(1);
        assertThat(response.getResult().getGateDecision().isLlmInfluencedGate()).isFalse();
        assertThat(response.getResult().getGateDecision().getReasons()).contains("fallback 1건으로 담당자 검토 필요");
        assertThat(response.getResult().getGateDecision().getLlmDecisionPolicy()).contains("cannot override");
    }

    @Test
    void getSourcesPreservesCollectionEvidenceForPublicApiProof() throws Exception {
        setUp();
        User user = user("user-sources", UserType.personal);
        Map<String, Object> collectionEvidence = Map.of(
                "endpointConfigured", true,
                "operationEndpoint", true,
                "endpointType", "operation",
                "apiKeyConfigured", true,
                "apiKeyStored", false
        );
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("field", "market");
        sourceMap.put("sourceId", "KCS_TRADE-record-1");
        sourceMap.put("provider", "KCS_TRADE");
        sourceMap.put("sourceType", "live");
        sourceMap.put("fallbackUsed", false);
        sourceMap.put("collectionRunId", "run-1");
        sourceMap.put("collectionEvidence", collectionEvidence);
        sourceMap.put("endpointConfigured", true);
        sourceMap.put("operationEndpoint", true);
        sourceMap.put("endpointType", "operation");
        sourceMap.put("apiKeyConfigured", true);
        sourceMap.put("apiKeyStored", false);
        sourceMap.put("retrievedAt", "2026-07-03T00:00:00Z");
        sourceMap.put("fetchedAt", "2026-07-03T00:00:00Z");
        sourceMap.put("sourcePeriodStart", "2026-06-01");
        sourceMap.put("sourcePeriodEnd", "2026-06-30");
        sourceMap.put("metricSummary", "exportAmountUsd=45678");
        sourceMap.put("decisionImpact", "시장성 +7");
        sourceMap.put("metrics", Map.of("exportAmountUsd", 45678));
        sourceMap.put("domainSignals", Map.of("recordCount", 12));
        Map<String, Object> result = Map.of("sources", List.of(sourceMap));
        var run = diagnosisRun("diag-sources", user, objectMapper.writeValueAsString(result));

        when(userRepository.findById("user-sources")).thenReturn(Optional.of(user));
        when(diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull("diag-sources", "user-sources")).thenReturn(Optional.of(run));

        var response = diagnosisService.getSources("user-sources", "diag-sources");
        var source = response.getSources().get(0);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseEvidence = (Map<String, Object>) source.getCollectionEvidence();
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMetrics = (Map<String, Object>) source.getMetrics();

        assertThat(responseEvidence).containsEntry("operationEndpoint", true);
        assertThat(source.getOperationEndpoint()).isTrue();
        assertThat(source.getEndpointType()).isEqualTo("operation");
        assertThat(source.getApiKeyConfigured()).isTrue();
        assertThat(source.getFetchedAt()).isEqualTo("2026-07-03T00:00:00Z");
        assertThat(source.getMetricSummary()).isEqualTo("exportAmountUsd=45678");
        assertThat(source.getDecisionImpact()).isEqualTo("시장성 +7");
        assertThat(responseMetrics).containsEntry("exportAmountUsd", 45678);
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

    private com.growgate.diagnosis.entity.DiagnosisRun diagnosisRun(String id, User user, String resultJson) {
        var run = com.growgate.diagnosis.entity.DiagnosisRun.builder()
                .id(id)
                .user(user)
                .result(resultJson)
                .status(com.growgate.diagnosis.enums.DiagnosisStatus.done)
                .targetCountry("DE")
                .inputData("{}")
                .knowledgeSnapshot("{}")
                .build();
        ReflectionTestUtils.setField(run, "createdAt", LocalDateTime.of(2026, 7, 3, 9, 0));
        return run;
    }
}
