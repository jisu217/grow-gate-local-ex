package com.growgate.agent.service;

import java.util.List;
import java.util.regex.Pattern;

final class LlmSecretSanitizer {

    private static final int MAX_LENGTH = 300;
    private static final List<Replacement> REPLACEMENTS = List.of(
            new Replacement(Pattern.compile("(?i)(Bearer\\s+)[A-Za-z0-9._\\-]+"), "$1[REDACTED]"),
            new Replacement(Pattern.compile("sk-or-v1-[A-Za-z0-9]{16,}"), "[REDACTED_OPENROUTER_KEY]"),
            new Replacement(Pattern.compile("AIza[A-Za-z0-9_\\-]{20,}"), "[REDACTED_GOOGLE_API_KEY]"),
            new Replacement(Pattern.compile("AQ\\.[A-Za-z0-9._\\-]{16,}"), "[REDACTED_GOOGLE_TOKEN]"),
            new Replacement(Pattern.compile("(?i)([?&](?:apiKey|apikey|key|token|access_token|client_secret)=)([^&\\s]+)"), "$1[REDACTED]"),
            new Replacement(Pattern.compile("(?i)((?:apiKey|apikey|key|token|access_token|client_secret|password)\\s*[:=]\\s*)([^,\\s}]+)"), "$1[REDACTED]")
    );

    private LlmSecretSanitizer() {
    }

    static String sanitize(String message) {
        if (message == null || message.isBlank()) return "unknown";
        String sanitized = message;
        for (Replacement replacement : REPLACEMENTS) {
            sanitized = replacement.pattern().matcher(sanitized).replaceAll(replacement.value());
        }
        if (sanitized.length() > MAX_LENGTH) {
            return sanitized.substring(0, MAX_LENGTH) + "...";
        }
        return sanitized;
    }

    private record Replacement(Pattern pattern, String value) {
    }
}
