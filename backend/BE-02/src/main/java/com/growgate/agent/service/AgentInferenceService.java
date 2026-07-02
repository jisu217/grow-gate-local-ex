package com.growgate.agent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class AgentInferenceService {

    private final List<AgentProviderClient> providers;
    private final ConcurrentHashMap<String, AtomicInteger> userDailyCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> globalDailyCounters = new ConcurrentHashMap<>();

    @Value("${app.agent.enabled:false}")
    private boolean enabled;

    @Value("${app.agent.budget.max-daily-requests-per-user:30}")
    private int maxDailyRequestsPerUser;

    @Value("${app.agent.budget.max-daily-requests-global:300}")
    private int maxDailyRequestsGlobal;

    @Value("${app.agent.budget.max-prompt-chars:12000}")
    private int maxPromptChars;

    public AgentInferenceResult infer(AgentInferenceRequest request) {
        List<String> fallbackReasons = new ArrayList<>();

        if (!enabled) {
            return fallback(fallbackReasons);
        }
        Optional<String> budgetBlockReason = enforceBudget(request);
        if (budgetBlockReason.isPresent()) {
            fallbackReasons.add(budgetBlockReason.get());
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
        return LlmSecretSanitizer.sanitize(reason);
    }

    private Optional<String> enforceBudget(AgentInferenceRequest request) {
        if (request == null) {
            return Optional.of("budget guard: empty inference request");
        }
        int estimatedChars = estimatePromptChars(request);
        if (maxPromptChars > 0 && estimatedChars > maxPromptChars) {
            return Optional.of("budget guard: prompt context too large (" + estimatedChars + " chars)");
        }

        String day = LocalDate.now(ZoneOffset.UTC).toString();
        if (maxDailyRequestsPerUser > 0) {
            String userKey = day + ":" + userScope(request);
            int userCount = increment(userKey, userDailyCounters);
            if (userCount > maxDailyRequestsPerUser) {
                return Optional.of("budget guard: user daily LLM quota reached");
            }
        }
        if (maxDailyRequestsGlobal > 0) {
            int globalCount = increment(day, globalDailyCounters);
            if (globalCount > maxDailyRequestsGlobal) {
                return Optional.of("budget guard: global daily LLM quota reached");
            }
        }
        purgePreviousDays(day, globalDailyCounters);
        purgePreviousDays(day, userDailyCounters);
        return Optional.empty();
    }

    private int increment(String key, ConcurrentHashMap<String, AtomicInteger> counters) {
        return counters.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
    }

    private void purgePreviousDays(String currentDay, ConcurrentHashMap<String, AtomicInteger> counters) {
        counters.keySet().removeIf(key -> !key.startsWith(currentDay));
    }

    private String userScope(AgentInferenceRequest request) {
        if (request.getUserId() != null && !request.getUserId().isBlank()) {
            return request.getUserId();
        }
        if (request.getDiagnosisId() != null && !request.getDiagnosisId().isBlank()) {
            return "diagnosis:" + request.getDiagnosisId();
        }
        return "anonymous";
    }

    private int estimatePromptChars(AgentInferenceRequest request) {
        int total = length(request.getUserId())
                + length(request.getDiagnosisId())
                + length(request.getProductName())
                + length(request.getHsCode())
                + length(request.getTargetCountry());
        total += mapLength(request.getEvidenceContext());
        total += mapLength(request.getScoringContext());
        return total;
    }

    private int mapLength(Map<String, Object> value) {
        return value == null ? 0 : value.toString().length();
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }
}
