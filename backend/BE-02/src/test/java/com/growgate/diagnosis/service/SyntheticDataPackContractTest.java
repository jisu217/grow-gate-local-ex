package com.growgate.diagnosis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SyntheticDataPackContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path root = Path.of("..", "..").normalize();

    @Test
    void syntheticProfileFixtureContainsThreeJudgeScenariosWithFixedGates() throws Exception {
        JsonNode rootNode = objectMapper.readTree(
                getClass().getResourceAsStream("/synthetic/synthetic_company_profiles.json"));
        JsonNode companies = rootNode.path("companies");

        assertThat(rootNode.path("classification").asText()).isEqualTo("synthetic_demo_not_real_customer");
        assertThat(companies).hasSize(3);
        assertThat(companies.findValuesAsText("gate")).containsExactlyInAnyOrder("HOLD", "CONDITIONAL_GO", "STOP");
        assertThat(companies.findValuesAsText("diag_id")).containsExactlyInAnyOrder(
                "2b52340c-d51d-50dc-b6cb-592627abb62d",
                "6bc9800e-755c-55f9-b9ce-87a69dc2c450",
                "cf7d23b4-51d8-53f7-be26-a92bf6682a6d"
        );
    }

    @Test
    void expectedTrajectoryFixtureSeparatesSeedFromLiveProofAndKeepsRuleGateOwnership() throws Exception {
        JsonNode rootNode = objectMapper.readTree(
                getClass().getResourceAsStream("/synthetic/expected_trajectory_eval_cases.json"));

        assertThat(rootNode.path("synthetic").asBoolean()).isTrue();
        List<String> allAssertions = new ArrayList<>();
        rootNode.path("cases").forEach(testCase -> {
            assertThat(textArray(testCase.path("expectedAgents")))
                    .contains("PlannerAgent", "ReportAgent", "VerifierAgent");
            assertThat(testCase.path("expectedGate").asText()).isIn("HOLD", "CONDITIONAL_GO", "STOP");
            allAssertions.addAll(textArray(testCase.path("assertions")));
        });
        assertThat(allAssertions).contains("llmInfluencedGate=false", "seed is not live proof");
    }

    @Test
    void mysqlSyntheticSeedKeepsSeedAsNotLiveProofAndUsesGrowgateAdminPassword() throws Exception {
        String sql = Files.readString(root.resolve("database/mysql/init/05_seed_synthetic_companies.sql"));
        String adminHash = "$2y$10$5BlFP36IjiTq20fBl/kkP.tbeQDqStrGae.empkMuMzCmfWDiXQlW";

        assertThat(sql).contains("synthetic_demo_not_real_customer");
        assertThat(sql).contains("owner+battery@growgate.local", "owner+marine@growgate.local", "owner+medical@growgate.local");
        assertThat(sql).doesNotContain("\"sourceType\":\"live\"", "\"sourceType\":\"cache\"");
        assertThat(sql).doesNotContain("\"operationEndpoint\":true", "\"apiKeyConfigured\":true");
        assertThat(new BCryptPasswordEncoder().matches("growgateadmin", adminHash)).isTrue();
        assertThat(sql.split(java.util.regex.Pattern.quote(adminHash), -1).length - 1).isEqualTo(6);

        Set<String> expectedLabels = Set.of("HOLD", "CONDITIONAL_GO", "STOP");
        for (String label : expectedLabels) {
            assertThat(sql).contains("\"finalGate\":\"" + label + "\"");
        }
    }

    private List<String> textArray(JsonNode arrayNode) {
        List<String> values = new ArrayList<>();
        arrayNode.forEach(node -> values.add(node.asText()));
        return values;
    }
}
