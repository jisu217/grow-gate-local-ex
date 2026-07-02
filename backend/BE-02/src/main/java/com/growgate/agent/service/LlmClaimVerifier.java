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
public class LlmClaimVerifier {

    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*\\d+([.,]\\d+)?%?.*");
    private static final List<String> UNSAFE_CERTAINTY_PHRASES = List.of(
            "문제 없음", "리스크 없음", "위험 없음", "확정", "보장", "반드시 통과", "FTO 문제 없음"
    );

    public Map<String, Object> verify(List<Map<String, Object>> claims, Collection<String> validSourceIds) {
        Set<String> allowed = validSourceIds == null ? Set.of() : validSourceIds.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toSet());
        List<Map<String, Object>> accepted = claims == null ? List.of() : claims.stream()
                .filter(claim -> rejectionReason(claim, allowed).isBlank())
                .map(this::compactClaim)
                .toList();
        List<Map<String, Object>> rejected = claims == null ? List.of() : claims.stream()
                .map(claim -> {
                    String reason = rejectionReason(claim, allowed);
                    if (reason.isBlank()) return null;
                    Map<String, Object> item = compactClaim(claim);
                    item.put("rejectionReason", reason);
                    return item;
                })
                .filter(item -> item != null)
                .toList();
        return Map.of(
                "acceptedClaims", accepted,
                "rejectedClaims", rejected,
                "acceptedCount", accepted.size(),
                "rejectedCount", rejected.size(),
                "verdict", rejected.isEmpty() ? "pass" : "filtered"
        );
    }

    private String rejectionReason(Map<String, Object> claim, Set<String> validSourceIds) {
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

    private Map<String, Object> compactClaim(Map<String, Object> claim) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("text", stringValue(claim.get("text")));
        item.put("sourceId", stringValue(claim.get("sourceId")));
        item.put("numericEvidence", Boolean.TRUE.equals(claim.get("numericEvidence")));
        return item;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
