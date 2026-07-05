package com.growgate.agent.service;

import com.growgate.knowledge.entity.BuyerRequirement;
import com.growgate.knowledge.entity.Certification;
import com.growgate.knowledge.entity.KnowledgeLoopItem;
import com.growgate.knowledge.entity.KnowledgeSource;
import com.growgate.knowledge.entity.ProductKnowledge;
import com.growgate.knowledge.enums.TrustGrade;
import com.growgate.publicdata.entity.PublicDataRecord;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceContextBuilderTest {

    @Test
    @SuppressWarnings("unchecked")
    void buildIncludesKnowledgeSourcesAndAcceptedLoopItems() {
        EvidenceContextBuilder builder = new EvidenceContextBuilder();

        Map<String, Object> context = builder.build(
                ProductKnowledge.builder()
                        .trustGrade(TrustGrade.high)
                        .sourceType("internal")
                        .notes("EU label policy v2")
                        .build(),
                List.of(Certification.builder()
                        .name("CE")
                        .expiresAt(LocalDate.of(2026, 12, 31))
                        .renewalCycle(12)
                        .build()),
                List.of(),
                List.of(BuyerRequirement.builder()
                        .buyerName("Berlin Buyer")
                        .country("DE")
                        .requirements("German label required")
                        .build()),
                List.of(KnowledgeSource.builder()
                        .documentName("EU label checklist")
                        .url("https://example.test/label")
                        .retrievedAt(LocalDate.of(2026, 6, 30))
                        .build()),
                List.of(),
                List.of(KnowledgeLoopItem.builder()
                        .type("risk_evidence")
                        .title("Battery transport label evidence")
                        .evidence("Repeated diagnosis risk")
                        .rationale("Useful for next diagnosis")
                        .impact("Improves label reasoning")
                        .build())
        );

        Map<String, Object> productKnowledge = (Map<String, Object>) context.get("productKnowledge");
        List<Map<String, Object>> knowledgeSources = (List<Map<String, Object>>) context.get("knowledgeSources");
        List<Map<String, Object>> loopItems = (List<Map<String, Object>>) context.get("acceptedKnowledgeLoop");
        List<Map<String, Object>> retrievalTopK = (List<Map<String, Object>>) context.get("retrievalTopK");

        assertThat(productKnowledge.get("trustGrade")).isEqualTo("high");
        assertThat(knowledgeSources).extracting(item -> item.get("documentName")).contains("EU label checklist");
        assertThat(loopItems).extracting(item -> item.get("title")).contains("Battery transport label evidence");
        assertThat(context.get("retrievalMode")).isEqualTo("source-bound-evidence-search");
        assertThat(context.get("claimBoundary").toString()).contains("bounded evidence retrieval");
        assertThat(retrievalTopK).isNotEmpty();
        assertThat(retrievalTopK.get(0)).containsKeys("chunkId", "score", "sourceId", "sourceBound", "displayRole", "usedSentence");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildRanksPublicDataChunksWithCitationFields() {
        EvidenceContextBuilder builder = new EvidenceContextBuilder();

        Map<String, Object> context = builder.build(
                ProductKnowledge.builder()
                        .trustGrade(TrustGrade.medium)
                        .sourceType("internal")
                        .notes("850760 Germany battery pack market review")
                        .build(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(PublicDataRecord.builder()
                        .provider(PublicDataProvider.KCS_TRADE)
                        .usedFor("market")
                        .hsCode("850760")
                        .targetCountry("DE")
                        .matchedKey("KCS_TRADE:850760:DE")
                        .sourceType("live")
                        .normalizedPayload("{\"metrics\":{\"exportAmountUsd\":45678,\"yoyGrowthPct\":12.4}}")
                        .fallbackUsed(false)
                        .scoreContribution(9)
                        .build()),
                List.of()
        );

        List<Map<String, Object>> retrievalTopK = (List<Map<String, Object>>) context.get("retrievalTopK");

        assertThat(retrievalTopK).isNotEmpty();
        assertThat(retrievalTopK)
                .anySatisfy(chunk -> {
                    assertThat(chunk.get("chunkId").toString()).startsWith("public-data:");
                    assertThat(chunk.get("sourceType")).isEqualTo("live");
                    assertThat(chunk.get("usedSentence").toString()).contains("KCS_TRADE");
                });
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildIncludesSourceQualityAndCollectionLedgerFieldsForAgentContext() {
        EvidenceContextBuilder builder = new EvidenceContextBuilder();

        Map<String, Object> context = builder.build(
                ProductKnowledge.builder()
                        .trustGrade(TrustGrade.medium)
                        .sourceType("internal")
                        .notes("850760 Germany battery pack market review")
                        .build(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(PublicDataRecord.builder()
                        .id("record-1")
                        .provider(PublicDataProvider.KCS_TRADE)
                        .usedFor("market")
                        .hsCode("850760")
                        .targetCountry("DE")
                        .matchedKey("KCS_TRADE:850760:DE")
                        .sourceType("empty")
                        .sourceQualityStatus("EMPTY_NOT_CONFIGURED")
                        .normalizedPayload("""
                                {"rawCount":0,"normalizedCount":0,"filteredCount":0,
                                 "zeroReason":"API_KEY_NOT_CONFIGURED",
                                 "collectionEvidence":{"operationEndpoint":true,"apiKeyConfigured":false,"endpointType":"operation"},
                                 "schemaValidation":{"status":"SKIPPED","reason":"API key missing"},
                                 "metrics":{"recordCount":0}}
                                """)
                        .fallbackUsed(false)
                        .scoreContribution(0)
                        .confidence(0.0)
                        .build()),
                List.of()
        );

        List<Map<String, Object>> signals = (List<Map<String, Object>>) context.get("publicDataSignals");
        Map<String, Object> signal = signals.get(0);

        assertThat(signal).containsEntry("sourceQualityStatus", "EMPTY_NOT_CONFIGURED");
        assertThat(signal).containsEntry("zeroReason", "API_KEY_NOT_CONFIGURED");
        assertThat(signal).containsEntry("rawCount", 0);
        assertThat(signal).containsEntry("normalizedCount", 0);
        assertThat(signal).containsEntry("operationEndpoint", true);
        assertThat(signal).containsEntry("apiKeyConfigured", false);
        assertThat(signal).containsKey("schemaValidation");
        assertThat(signal).containsKey("scoreContributionLedger");
        assertThat(signal.get("scoreContributionLedger").toString()).contains("positiveContributionAllowed=false");
    }
}
