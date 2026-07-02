package com.growgate.agent.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AgentInferenceServiceTest {

    @Test
    void inferUsesGeminiFirstWhenItSucceeds() {
        FakeProvider gemini = new FakeProvider("gemini", AgentInferenceResult.success(
                "gemini", "gemini-2.5-flash", Map.of("summary", "gemini ok"), List.of(), 31));
        FakeProvider openRouter = new FakeProvider("openrouter", AgentInferenceResult.success(
                "openrouter", "google/gemini-flash", Map.of("summary", "backup ok"), List.of(), 44));

        AgentInferenceService service = new AgentInferenceService(List.of(gemini, openRouter));
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "maxDailyRequestsPerUser", 30);
        ReflectionTestUtils.setField(service, "maxDailyRequestsGlobal", 300);
        ReflectionTestUtils.setField(service, "maxPromptChars", 12000);

        AgentInferenceResult result = service.infer(request());

        assertThat(result.isSucceeded()).isTrue();
        assertThat(result.getProvider()).isEqualTo("gemini");
        assertThat(gemini.calls).isEqualTo(1);
        assertThat(openRouter.calls).isZero();
    }

    @Test
    void inferFallsBackToOpenRouterWhenGeminiFails() {
        FakeProvider gemini = new FakeProvider("gemini", AgentInferenceResult.failure("gemini", "quota"));
        FakeProvider openRouter = new FakeProvider("openrouter", AgentInferenceResult.success(
                "openrouter", "google/gemini-flash", Map.of("summary", "backup ok"), List.of(), 44));

        AgentInferenceService service = new AgentInferenceService(List.of(gemini, openRouter));
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "maxDailyRequestsPerUser", 30);
        ReflectionTestUtils.setField(service, "maxDailyRequestsGlobal", 300);
        ReflectionTestUtils.setField(service, "maxPromptChars", 12000);

        AgentInferenceResult result = service.infer(request());

        assertThat(result.isSucceeded()).isTrue();
        assertThat(result.getProvider()).isEqualTo("openrouter");
        assertThat(result.getFallbackReasons()).contains("gemini: quota");
        assertThat(gemini.calls).isEqualTo(1);
        assertThat(openRouter.calls).isEqualTo(1);
    }

    @Test
    void inferReturnsFallbackResultWhenAllProvidersFail() {
        AgentInferenceService service = new AgentInferenceService(List.of(
                new FakeProvider("gemini", AgentInferenceResult.failure("gemini", "timeout")),
                new FakeProvider("openrouter", AgentInferenceResult.failure("openrouter", "429"))
        ));
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "maxDailyRequestsPerUser", 30);
        ReflectionTestUtils.setField(service, "maxDailyRequestsGlobal", 300);
        ReflectionTestUtils.setField(service, "maxPromptChars", 12000);

        AgentInferenceResult result = service.infer(request());

        assertThat(result.isSucceeded()).isFalse();
        assertThat(result.getProvider()).isEqualTo("spring-local");
        assertThat(result.getFallbackReasons()).containsExactly("gemini: timeout", "openrouter: 429");
    }

    @Test
    void inferRedactsProviderFailureSecretsFromFallbackReasons() {
        AgentInferenceService service = new AgentInferenceService(List.of(
                new FakeProvider("openrouter", AgentInferenceResult.failure("openrouter",
                        "401 Bearer sk-or-v1-test-secret-token apiKey=sk-or-v1-test-secret-token"))
        ));
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "maxDailyRequestsPerUser", 30);
        ReflectionTestUtils.setField(service, "maxDailyRequestsGlobal", 300);
        ReflectionTestUtils.setField(service, "maxPromptChars", 12000);

        AgentInferenceResult result = service.infer(request());

        assertThat(result.getFallbackReasons().get(0)).contains("[REDACTED]");
        assertThat(result.getFallbackReasons().get(0)).doesNotContain("sk-or-v1-test-secret-token");
    }

    @Test
    void inferStopsBeforeProviderWhenUserDailyQuotaIsReached() {
        FakeProvider gemini = new FakeProvider("gemini", AgentInferenceResult.success(
                "gemini", "gemini-2.5-flash", Map.of("summary", "ok"), List.of(), 10));
        AgentInferenceService service = new AgentInferenceService(List.of(gemini));
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "maxDailyRequestsPerUser", 1);
        ReflectionTestUtils.setField(service, "maxDailyRequestsGlobal", 300);
        ReflectionTestUtils.setField(service, "maxPromptChars", 12000);

        AgentInferenceResult first = service.infer(request());
        AgentInferenceResult second = service.infer(request());

        assertThat(first.isSucceeded()).isTrue();
        assertThat(second.isSucceeded()).isFalse();
        assertThat(second.getFallbackReasons()).contains("budget guard: user daily LLM quota reached");
        assertThat(gemini.calls).isEqualTo(1);
    }

    @Test
    void inferStopsBeforeProviderWhenPromptContextIsTooLarge() {
        FakeProvider gemini = new FakeProvider("gemini", AgentInferenceResult.success(
                "gemini", "gemini-2.5-flash", Map.of("summary", "ok"), List.of(), 10));
        AgentInferenceService service = new AgentInferenceService(List.of(gemini));
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "maxDailyRequestsPerUser", 30);
        ReflectionTestUtils.setField(service, "maxDailyRequestsGlobal", 300);
        ReflectionTestUtils.setField(service, "maxPromptChars", 20);

        AgentInferenceResult result = service.infer(request());

        assertThat(result.isSucceeded()).isFalse();
        assertThat(result.getFallbackReasons().get(0)).contains("budget guard: prompt context too large");
        assertThat(gemini.calls).isZero();
    }

    private AgentInferenceRequest request() {
        return AgentInferenceRequest.builder()
                .userId("user-1")
                .diagnosisId("diagnosis-1")
                .productName("Battery Pack")
                .hsCode("850760")
                .targetCountry("DE")
                .evidenceContext(Map.of("knowledge", "FTO review needed"))
                .build();
    }

    private static class FakeProvider implements AgentProviderClient {
        private final String name;
        private final AgentInferenceResult result;
        private int calls;

        private FakeProvider(String name, AgentInferenceResult result) {
            this.name = name;
            this.result = result;
        }

        @Override
        public String providerName() {
            return name;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public Optional<AgentInferenceResult> infer(AgentInferenceRequest request) {
            calls++;
            return Optional.of(result);
        }
    }
}
