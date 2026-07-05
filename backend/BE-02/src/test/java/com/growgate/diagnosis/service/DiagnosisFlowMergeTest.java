package com.growgate.diagnosis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.agent.service.AgentInferenceService;
import com.growgate.agent.service.ClaimVerificationService;
import com.growgate.agent.service.EvidenceContextBuilder;
import com.growgate.diagnosis.dto.CreateDiagnosisRequest;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse;
import com.growgate.diagnosis.repository.DiagnosisRepository;
import com.growgate.knowledge.repository.BuyerRequirementRepository;
import com.growgate.knowledge.repository.CertificationRepository;
import com.growgate.knowledge.repository.FtoRepository;
import com.growgate.knowledge.repository.KnowledgeLoopItemRepository;
import com.growgate.knowledge.repository.KnowledgeSourceRepository;
import com.growgate.knowledge.repository.ProductKnowledgeRepository;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.product.repository.ProductRepository;
import com.growgate.publicdata.entity.PublicDataRecord;
import com.growgate.publicdata.enums.PublicDataProvider;
import com.growgate.publicdata.service.PublicDataCollectionService;
import com.growgate.regional.service.RegionalIndustryProfileService;
import com.growgate.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DiagnosisFlowMergeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void directInputSnapshotKeepsInputIdentityForDetailResponse() {
        DiagnosisService service = newDiagnosisService();
        CreateDiagnosisRequest.InputData inputData = new CreateDiagnosisRequest.InputData();
        ReflectionTestUtils.setField(inputData, "productName", "BMS 모듈");
        ReflectionTestUtils.setField(inputData, "hsCode", "853710");
        ReflectionTestUtils.setField(inputData, "description", "전장 제어 모듈");

        String json = ReflectionTestUtils.invokeMethod(service, "buildInputSnapshot", inputData, "DE");
        DiagnosisDetailResponse.KnowledgeSnapshot snapshot =
                ReflectionTestUtils.invokeMethod(service, "parseKnowledgeSnapshot", json);

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getProductName()).isEqualTo("BMS 모듈");
        assertThat(snapshot.getHsCode()).isEqualTo("853710");
        assertThat(snapshot.getTargetCountry()).isEqualTo("DE");
        assertThat(snapshot.getSourceType()).isEqualTo("direct_input");
        assertThat(snapshot.getMissingFields()).contains("productKnowledge", "certifications", "fto", "buyerRequirements");
        assertThat(snapshot.getSnapshotAt()).endsWith("T00:00:00Z");
    }

    @Test
    void failureResultUsesSafeUserMessageAndTimestamp() throws Exception {
        DiagnosisAsyncProcessor processor = newDiagnosisAsyncProcessor();

        String json = ReflectionTestUtils.invokeMethod(processor, "buildFailureResult",
                new IllegalStateException("jdbc:mysql://secret-host?password=hidden"));

        Map<String, Object> result = objectMapper.readValue(json, new TypeReference<>() {});
        assertThat(result.get("summary")).isEqualTo("진단 처리 중 오류가 발생했습니다.");
        assertThat(result.get("errorType")).isEqualTo("IllegalStateException");
        assertThat(result.get("errorMessage")).isEqualTo("진단 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        assertThat((String) result.get("errorMessage")).doesNotContain("secret-host", "password");
        assertThat(OffsetDateTime.parse((String) result.get("failedAt"))).isNotNull();
        assertThat(result).containsKeys("agentTrace", "evidenceSources", "riskItems");
    }

    @Test
    void emptyPublicDataSourceIsNotDescribedAsLiveOrCacheEvidence() {
        DiagnosisAsyncProcessor processor = newDiagnosisAsyncProcessor();
        PublicDataRecord emptyRecord = PublicDataRecord.builder()
                .provider(PublicDataProvider.SAFETY_RECALL)
                .usedFor("recall")
                .sourceType("empty")
                .fallbackUsed(false)
                .scoreContribution(0)
                .build();

        String metricSummary = ReflectionTestUtils.invokeMethod(processor,
                "publicDataMetricSummary", emptyRecord, Map.of());
        String decisionImpact = ReflectionTestUtils.invokeMethod(processor,
                "publicDataDecisionImpact", emptyRecord, 0);
        String reason = ReflectionTestUtils.invokeMethod(processor,
                "publicDataReason", emptyRecord);

        assertThat(metricSummary).contains("empty record");
        assertThat(decisionImpact).contains("live/cache 성공 근거가 아니며");
        assertThat(reason).contains("live/cache 성공 근거로 표시하지 않고");
    }

    private DiagnosisService newDiagnosisService() {
        return new DiagnosisService(
                mock(DiagnosisRepository.class),
                mock(UserRepository.class),
                mock(MembershipRepository.class),
                mock(ProductRepository.class),
                mock(ProductKnowledgeRepository.class),
                mock(CertificationRepository.class),
                mock(FtoRepository.class),
                mock(BuyerRequirementRepository.class),
                objectMapper,
                mock(DiagnosisAsyncProcessor.class)
        );
    }

    private DiagnosisAsyncProcessor newDiagnosisAsyncProcessor() {
        return new DiagnosisAsyncProcessor(
                mock(DiagnosisRepository.class),
                objectMapper,
                mock(ProductKnowledgeRepository.class),
                mock(CertificationRepository.class),
                mock(FtoRepository.class),
                mock(BuyerRequirementRepository.class),
                mock(KnowledgeSourceRepository.class),
                mock(FastApiAgentClient.class),
                mock(PublicDataCollectionService.class),
                mock(AgentInferenceService.class),
                mock(EvidenceContextBuilder.class),
                mock(KnowledgeLoopItemRepository.class),
                mock(ClaimVerificationService.class),
                mock(RegionalIndustryProfileService.class)
        );
    }
}
