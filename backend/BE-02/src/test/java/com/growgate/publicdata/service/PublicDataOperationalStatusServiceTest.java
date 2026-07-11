package com.growgate.publicdata.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PublicDataOperationalStatusServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void healthSummaryIncludesSafetyKoreaForeignRecallProvider() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("public-data.kats-foreign-recall.url",
                        "https://www.safetykorea.kr/openapi/api/recall/fRecallList.json")
                .withProperty("public-data.kats-foreign-recall.key", "secret-safety-key");
        PublicDataOperationalStatusService service = new PublicDataOperationalStatusService(environment);

        Map<String, String> summary = service.healthSummary();
        Map<String, Object> body = service.status();
        List<Map<String, Object>> providers = (List<Map<String, Object>>) body.get("providers");
        Map<String, Object> foreignRecall = providers.stream()
                .filter(provider -> "KATS_FOREIGN_RECALL".equals(provider.get("provider")))
                .findFirst()
                .orElseThrow();

        assertThat(summary).containsEntry("katsForeignRecall", "empty");
        assertThat(String.valueOf(body)).contains("KATS_FOREIGN_RECALL");
        assertThat(String.valueOf(body)).contains("apiKeyMasked=secr****-key");
        assertThat(String.valueOf(body)).doesNotContain("secret-safety-key");
        assertThat(foreignRecall).containsEntry("endpointType", "operation");
        assertThat(foreignRecall).containsEntry("apiKeyConfigured", true);
    }
}
