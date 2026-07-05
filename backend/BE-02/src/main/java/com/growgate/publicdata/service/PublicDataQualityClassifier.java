package com.growgate.publicdata.service;

import com.growgate.publicdata.entity.PublicDataRecord;
import com.growgate.publicdata.enums.SourceQualityStatus;

import java.util.Map;
import java.util.Set;

public final class PublicDataQualityClassifier {

    private static final Set<String> NOT_CONFIGURED_REASONS = Set.of(
            "PUBLIC_DATA_DISABLED",
            "API_KEY_NOT_CONFIGURED",
            "OPERATION_ENDPOINT_NOT_CONFIGURED",
            "CATALOG_PAGE_ENDPOINT"
    );

    private PublicDataQualityClassifier() {
    }

    public static SourceQualityStatus classify(PublicDataRecord record, Map<String, Object> normalizedPayload) {
        if (record == null) return SourceQualityStatus.HEURISTIC_ONLY;
        String explicit = safe(record.getSourceQualityStatus());
        if (!explicit.isBlank()) {
            try {
                return SourceQualityStatus.valueOf(explicit);
            } catch (IllegalArgumentException ignored) {
                // Fall through to derived classification.
            }
        }
        return classify(sourceType(record), record.isFallbackUsed(), normalizedPayload, safe(record.getErrorMessage()));
    }

    public static SourceQualityStatus classify(String sourceType, boolean fallbackUsed,
                                               Map<String, Object> normalizedPayload, String errorMessage) {
        String type = safe(sourceType);
        if (fallbackUsed || "fallback".equals(type)) return SourceQualityStatus.FALLBACK_ERROR;
        if ("seed".equals(type)) return SourceQualityStatus.SEED_SYNTHETIC;
        if ("heuristic".equals(type) || "internal".equals(type) || "user_input".equals(type)) {
            return SourceQualityStatus.HEURISTIC_ONLY;
        }
        if ("cache".equals(type) || "file".equals(type) || "file_stale".equals(type)) {
            return SourceQualityStatus.CACHE_VERIFIED;
        }
        if ("empty".equals(type)) {
            return emptyStatus(normalizedPayload, errorMessage);
        }
        if ("live".equals(type)) {
            return SourceQualityStatus.LIVE_VERIFIED;
        }
        return SourceQualityStatus.HEURISTIC_ONLY;
    }

    public static boolean positiveContributionAllowed(SourceQualityStatus status) {
        return status == SourceQualityStatus.LIVE_VERIFIED
                || status == SourceQualityStatus.CACHE_VERIFIED
                || status == SourceQualityStatus.EMPTY_TRUE_ZERO;
    }

    public static String sourceType(PublicDataRecord record) {
        if (record == null) return "";
        if (record.isFallbackUsed() || "fallback".equals(record.getSourceType())) return "fallback";
        if (record.isCacheHit()
                || "cache".equals(record.getSourceType())
                || "file".equals(record.getSourceType())
                || "file_stale".equals(record.getSourceType())) return "cache";
        if (record.getSourceType() != null && !record.getSourceType().isBlank()) return record.getSourceType();
        return "live";
    }

    private static SourceQualityStatus emptyStatus(Map<String, Object> normalizedPayload, String errorMessage) {
        String zeroReason = safe(value(normalizedPayload, "zeroReason"));
        if (NOT_CONFIGURED_REASONS.contains(zeroReason) || NOT_CONFIGURED_REASONS.contains(safe(errorMessage))) {
            return SourceQualityStatus.EMPTY_NOT_CONFIGURED;
        }
        Map<String, Object> evidence = mapValue(normalizedPayload == null ? null : normalizedPayload.get("collectionEvidence"));
        boolean operationEndpoint = Boolean.TRUE.equals(evidence.get("operationEndpoint"));
        boolean apiKeyConfigured = Boolean.TRUE.equals(evidence.get("apiKeyConfigured"));
        if (!operationEndpoint || !apiKeyConfigured) {
            return SourceQualityStatus.EMPTY_NOT_CONFIGURED;
        }
        return SourceQualityStatus.EMPTY_TRUE_ZERO;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static Object value(Map<String, Object> map, String key) {
        return map == null ? "" : map.getOrDefault(key, "");
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
