package com.growgate.agent.service;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ClaimVerificationService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*\\d+([.,]\\d+)?%?.*");
    private static final List<String> UNSAFE_CERTAINTY_PHRASES = List.of(
            "문제 없음", "리스크 없음", "위험 없음", "확정", "보장", "반드시 통과", "FTO 문제 없음"
    );

    public Map<String, Object> verifyClaims(List<Map<String, Object>> claims, Collection<String> validSourceIds) {
        Set<String> allowed = validSourceIds == null ? Set.of() : validSourceIds.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toSet());
        List<Map<String, Object>> safeClaims = claims == null ? List.of() : claims.stream()
                .map(claim -> verification(claim, allowed))
                .filter(item -> "PASS".equals(item.get("status")))
                .map(item -> {
                    Map<String, Object> safe = new LinkedHashMap<>();
                    safe.put("text", item.get("claim"));
                    safe.put("sourceId", item.get("sourceId"));
                    return safe;
                })
                .toList();
        List<Map<String, Object>> verifications = claims == null ? List.of() : claims.stream()
                .map(claim -> verification(claim, allowed))
                .toList();
        List<String> fallbackReasons = verifications.stream()
                .filter(item -> "BLOCK".equals(item.get("status")))
                .map(item -> "LLM claim blocked: " + item.get("reason"))
                .distinct()
                .toList();
        long blockedCount = verifications.stream()
                .filter(item -> "BLOCK".equals(item.get("status")))
                .count();
        long warnCount = verifications.stream()
                .filter(item -> "WARN".equals(item.get("status")))
                .count();
        return Map.of(
                "safeClaims", safeClaims,
                "claimVerifications", verifications,
                "fallbackReasons", fallbackReasons,
                "passCount", safeClaims.size(),
                "warnCount", warnCount,
                "blockCount", blockedCount,
                "verdict", blockedCount > 0 ? "blocked" : warnCount > 0 ? "warn" : "pass"
        );
    }

    private Map<String, Object> verification(Map<String, Object> claim, Set<String> validSourceIds) {
        Map<String, Object> item = new LinkedHashMap<>();
        String text = stringValue(claim == null ? null : claim.get("text"));
        String sourceId = stringValue(claim == null ? null : claim.get("sourceId"));
        item.put("claim", safeClaimText(text));
        item.put("sourceId", sourceId);
        item.put("status", "PASS");
        item.put("reason", "source-bound claim");
        item.put("numericEvidence", claim != null && Boolean.TRUE.equals(claim.get("numericEvidence")));

        String blockReason = blockReason(claim, validSourceIds);
        if (!blockReason.isBlank()) {
            item.put("status", "BLOCK");
            item.put("reason", blockReason);
        }
        return item;
    }

    private String blockReason(Map<String, Object> claim, Set<String> validSourceIds) {
        if (claim == null) return "empty claim";
        String text = stringValue(claim.get("text"));
        String sourceId = stringValue(claim.get("sourceId"));
        if (text.isBlank()) return "empty claim text";
        if (sourceId.isBlank()) return "missing sourceId";
        if (!validSourceIds.contains(sourceId)) return "unknown sourceId";
        if (containsUnsafeCertainty(text)) return "unsafe definitive claim";
        if (NUMBER_PATTERN.matcher(text).matches() && !Boolean.TRUE.equals(claim.get("numericEvidence"))) {
            return "numeric claim without metric evidence";
        }
        return "";
    }

    private boolean containsUnsafeCertainty(String text) {
        return UNSAFE_CERTAINTY_PHRASES.stream().anyMatch(text::contains);
    }

    private String safeClaimText(String text) {
        if (containsUnsafeCertainty(text)) return "[filtered unsafe claim]";
        return LlmSecretSanitizer.sanitize(text);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
