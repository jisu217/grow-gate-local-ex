package com.growgate.agent.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentInstructionRegistryTest {

    @Test
    void registryContainsAllKnowledgeLoopAgents() {
        AgentInstructionRegistry registry = new AgentInstructionRegistry();

        assertThat(registry.all())
                .extracting(AgentInstructionRegistry.AgentInstruction::agent)
                .containsExactly(
                        "DATA_COLLECTION",
                        "NORMALIZATION",
                        "MARKET",
                        "REGULATION_CERTIFICATION",
                        "RECALL_SAFETY",
                        "FTO_PATENT",
                        "SUPPORT",
                        "EXEC_BOARD",
                        "REPORT",
                        "VERIFICATION"
                );
        assertThat(registry.require("REGULATION_CERTIFICATION").knowledgeType())
                .isEqualTo("REGULATION_CHECKLIST");
        assertThat(registry.require("RECALL_SAFETY").reuseLocation())
                .contains("품질 체크리스트");
    }
}
