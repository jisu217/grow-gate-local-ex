package com.growgate.agent.service;

import com.growgate.knowledge.entity.BuyerRequirement;
import com.growgate.knowledge.entity.Certification;
import com.growgate.knowledge.entity.KnowledgeLoopItem;
import com.growgate.knowledge.entity.KnowledgeSource;
import com.growgate.knowledge.entity.ProductKnowledge;
import com.growgate.knowledge.enums.TrustGrade;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRagContextBuilderTest {

    @Test
    @SuppressWarnings("unchecked")
    void buildIncludesKnowledgeSourcesAndAcceptedLoopItems() {
        AgentRagContextBuilder builder = new AgentRagContextBuilder();

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

        assertThat(productKnowledge.get("trustGrade")).isEqualTo("high");
        assertThat(knowledgeSources).extracting(item -> item.get("documentName")).contains("EU label checklist");
        assertThat(loopItems).extracting(item -> item.get("title")).contains("Battery transport label evidence");
    }
}
