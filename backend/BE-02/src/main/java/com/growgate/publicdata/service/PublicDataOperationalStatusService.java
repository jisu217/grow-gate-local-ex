package com.growgate.publicdata.service;

import com.growgate.publicdata.enums.PublicDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PublicDataOperationalStatusService {

    private final Environment environment;

    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", bool("public-data.enabled", true));
        body.put("providers", providerStatuses());
        body.put("policy", Map.of(
                "connectTimeoutMs", intValue("public-data.http.connect-timeout-ms", 3000),
                "readTimeoutMs", intValue("public-data.http.read-timeout-ms", 20000),
                "retryMaxAttempts", intValue("public-data.http.retry.max-attempts", 2),
                "retryStatusCodes", List.of(408, 429, 500, 502, 503, 504),
                "circuitFailureThreshold", intValue("public-data.circuit.failure-threshold", 3),
                "circuitOpenSeconds", intValue("public-data.circuit.open-seconds", 60)
        ));
        body.put("checkedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        return body;
    }

    public Map<String, String> healthSummary() {
        Map<String, String> summary = new LinkedHashMap<>();
        providerStatuses().forEach(provider -> summary.put(
                healthKey(provider.get("provider").toString()),
                provider.get("status").toString()
        ));
        return summary;
    }

    private List<Map<String, Object>> providerStatuses() {
        return List.of(
                providerStatus(PublicDataProvider.KCS_TRADE, prop("public-data.kcs-trade.url"), prop("public-data.kcs-trade.key")),
                providerStatus(PublicDataProvider.KOTRA_MARKET, prop("public-data.kotra-market.url"), prop("public-data.kotra-market.key")),
                providerStatus(PublicDataProvider.SAFETY_RECALL, prop("public-data.safety-recall.url"), prop("public-data.safety-recall.key")),
                fileDataStatus(PublicDataProvider.MOTIE_IMPORT_REGULATION,
                        prop("public-data.motie-import-regulation.csv-resource"),
                        prop("public-data.motie-import-regulation.url"),
                        prop("public-data.motie-import-regulation.key")),
                fileDataStatus(PublicDataProvider.MOTIE_MPE_TRADE_STATISTICS,
                        prop("public-data.motie-mpe-trade-statistics.csv-resource"),
                        prop("public-data.motie-mpe-trade-statistics.url"),
                        prop("public-data.motie-mpe-trade-statistics.key")),
                fileDataStatus(PublicDataProvider.MOTIE_MPE_PRODUCTION_INDEX,
                        prop("public-data.motie-mpe-production-index.csv-resource"),
                        prop("public-data.motie-mpe-production-index.url"),
                        prop("public-data.motie-mpe-production-index.key")),
                fileDataStatus(PublicDataProvider.TRADE_SECURITY_HSK_LINKAGE,
                        prop("public-data.trade-security-hsk-linkage.csv-resource"),
                        prop("public-data.trade-security-hsk-linkage.url"),
                        prop("public-data.trade-security-hsk-linkage.key")),
                fileDataStatus(PublicDataProvider.TRADE_SECURITY_ITEM_KEYWORD,
                        prop("public-data.trade-security-item-keyword.csv-resource"),
                        prop("public-data.trade-security-item-keyword.url"),
                        prop("public-data.trade-security-item-keyword.key")),
                providerStatus(PublicDataProvider.KIPRIS_FTO, prop("public-data.kipris-fto.url"), prop("public-data.kipris-fto.key")),
                providerStatus(PublicDataProvider.BIZINFO_SUPPORT, prop("public-data.bizinfo-support.url"), prop("public-data.bizinfo-support.key")),
                feedStatus(PublicDataProvider.MOTIE_WEB_CRAWL, bool("public-data.crawl.enabled", false), prop("public-data.crawl.urls")),
                feedStatus(PublicDataProvider.EXPORT_EVIDENCE_FEED, bool("public-data.export-evidence.enabled", false), prop("public-data.export-evidence.urls"))
        );
    }

    private Map<String, Object> providerStatus(PublicDataProvider provider, String endpoint, String apiKey) {
        boolean endpointConfigured = endpoint != null && !endpoint.isBlank();
        boolean apiKeyConfigured = apiKey != null && !apiKey.isBlank();
        boolean operationEndpoint = endpointConfigured && !isCatalogOrApplicationPage(endpoint);
        String sourceType = "empty";
        String status = "empty";
        String zeroReason = "";

        if (!bool("public-data.enabled", true)) {
            status = "not_configured";
            zeroReason = "PUBLIC_DATA_DISABLED";
        } else if (!endpointConfigured) {
            status = "not_configured";
            zeroReason = "OPERATION_ENDPOINT_NOT_CONFIGURED";
        } else if (!operationEndpoint) {
            zeroReason = "CATALOG_PAGE_ENDPOINT";
        } else if (!apiKeyConfigured) {
            zeroReason = "API_KEY_NOT_CONFIGURED";
        }

        Map<String, Object> row = baseProvider(provider, status, sourceType, zeroReason);
        row.put("endpointConfigured", endpointConfigured);
        row.put("endpointType", endpointConfigured ? operationEndpoint ? "operation" : "catalog" : "missing");
        row.put("apiKeyConfigured", apiKeyConfigured);
        row.put("apiKeyMasked", apiKeyConfigured ? mask(apiKey) : "");
        row.put("rawCount", 0);
        row.put("normalizedCount", 0);
        row.put("fallbackUsed", false);
        return row;
    }

    private Map<String, Object> fileDataStatus(PublicDataProvider provider, String csvResource,
                                               String refreshEndpoint, String apiKey) {
        boolean csvConfigured = csvResource != null && !csvResource.isBlank();
        boolean endpointConfigured = refreshEndpoint != null && !refreshEndpoint.isBlank();
        boolean apiKeyConfigured = apiKey != null && !apiKey.isBlank();
        Map<String, Object> row = baseProvider(provider, csvConfigured ? "file_ready" : "not_configured",
                csvConfigured ? "file" : "empty", csvConfigured ? "" : "CSV_RESOURCE_NOT_CONFIGURED");
        row.put("endpointConfigured", csvConfigured);
        row.put("endpointType", csvConfigured ? "csv_resource" : "missing");
        row.put("refreshEndpointConfigured", endpointConfigured);
        row.put("apiKeyConfigured", apiKeyConfigured);
        row.put("apiKeyMasked", apiKeyConfigured ? mask(apiKey) : "");
        row.put("rawCount", 0);
        row.put("normalizedCount", 0);
        row.put("fallbackUsed", false);
        return row;
    }

    private Map<String, Object> feedStatus(PublicDataProvider provider, boolean enabled, String urls) {
        boolean configured = enabled && urls != null && !urls.isBlank();
        Map<String, Object> row = baseProvider(provider, configured ? "empty" : "not_configured", "empty",
                configured ? "EMPTY_LIVE_RESPONSE" : "OPERATION_ENDPOINT_NOT_CONFIGURED");
        row.put("endpointConfigured", configured);
        row.put("endpointType", configured ? "operation" : "missing");
        row.put("apiKeyConfigured", false);
        row.put("apiKeyMasked", "");
        row.put("rawCount", 0);
        row.put("normalizedCount", 0);
        row.put("fallbackUsed", false);
        return row;
    }

    private Map<String, Object> baseProvider(PublicDataProvider provider, String status,
                                             String sourceType, String zeroReason) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("provider", provider.name());
        row.put("agency", provider.agency());
        row.put("ministry", provider.ministry());
        row.put("dataFamily", provider.dataFamily());
        row.put("usedFor", provider.usedFor());
        row.put("status", status);
        row.put("sourceType", sourceType);
        row.put("zeroReason", zeroReason);
        return row;
    }

    private String prop(String key) {
        return environment.getProperty(key, "");
    }

    private boolean bool(String key, boolean fallback) {
        return Boolean.parseBoolean(environment.getProperty(key, Boolean.toString(fallback)));
    }

    private int intValue(String key, int fallback) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) return "";
        if (value.length() <= 8) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private boolean isCatalogOrApplicationPage(String endpoint) {
        String normalized = endpoint.toLowerCase();
        return normalized.contains("data.go.kr") && normalized.endsWith("/openapi.do")
                || normalized.contains("plus.kipris.or.kr") && normalized.contains("/contents.do")
                || normalized.equals("https://www.exportvoucher.com/")
                || normalized.equals("https://www.safetykorea.kr/release/openapi");
    }

    private String healthKey(String provider) {
        return switch (provider) {
            case "KCS_TRADE" -> "trade";
            case "KOTRA_MARKET" -> "kotra";
            case "SAFETY_RECALL" -> "safetyKorea";
            case "MOTIE_IMPORT_REGULATION" -> "motieImportRegulation";
            case "MOTIE_MPE_TRADE_STATISTICS" -> "motieMpeTradeStatistics";
            case "MOTIE_MPE_PRODUCTION_INDEX" -> "motieMpeProductionIndex";
            case "TRADE_SECURITY_HSK_LINKAGE" -> "tradeSecurityHskLinkage";
            case "TRADE_SECURITY_ITEM_KEYWORD" -> "tradeSecurityItemKeyword";
            case "KIPRIS_FTO" -> "kipris";
            case "BIZINFO_SUPPORT" -> "bizinfo";
            case "MOTIE_WEB_CRAWL" -> "motie";
            case "EXPORT_EVIDENCE_FEED" -> "rss";
            default -> provider.toLowerCase();
        };
    }
}
