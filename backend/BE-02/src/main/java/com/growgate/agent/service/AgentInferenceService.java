package com.growgate.agent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgentInferenceService {

    private final List<AgentProviderClient> providers;

    @Value("${app.agent.enabled:false}")
    private boolean enabled;

    public AgentInferenceResult infer(AgentInferenceRequest request) {
        List<String> fallbackReasons = new ArrayList<>();

        if (!enabled) {
            return fallback(fallbackReasons);
        }

        for (AgentProviderClient provider : providers) {
            if (!provider.isEnabled()) continue;
            Optional<AgentInferenceResult> candidate = provider.infer(request);
            if (candidate.isEmpty()) {
                fallbackReasons.add(provider.providerName() + ": empty response");
                continue;
            }
            AgentInferenceResult result = candidate.get();
            if (result.isSucceeded()) {
                return result.withFallbackReasons(fallbackReasons);
            }
            fallbackReasons.add(provider.providerName() + ": " + safeReason(result.getErrorMessage()));
        }

        return fallback(fallbackReasons);
    }

    private AgentInferenceResult fallback(List<String> fallbackReasons) {
        return AgentInferenceResult.builder()
                .succeeded(false)
                .provider("spring-local")
                .model("rule-fallback")
                .content(java.util.Map.of())
                .knowledgeSuggestions(List.of())
                .fallbackReasons(fallbackReasons)
                .errorMessage("All external agent providers failed or are disabled")
                .build();
    }

    private String safeReason(String reason) {
        return reason == null || reason.isBlank() ? "unknown" : reason;
    }
}
