package com.growgate.publicdata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "public-data.crawl", name = "enabled", havingValue = "true")
public class MotieWebCrawlConnector implements PublicDataConnector {

    private static final int MAX_RAW_CHARS = 120_000;
    private static final int MAX_TEXT_CHARS = 3_000;
    private static final int MAX_ENDPOINT_CHARS = 480;
    private static final Pattern SCRIPT_STYLE = Pattern.compile("(?is)<(script|style|noscript)[^>]*>.*?</\\1>");
    private static final Pattern TAG = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern SPACE = Pattern.compile("\\s+");
    private static final Pattern SECRET = Pattern.compile("(?i)(serviceKey|apiKey|apikey|token|secret|password|authorization)\\s*[=:]\\s*[^\\s&]+");
    private static final Pattern EMAIL = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("\\b(?:\\+?82[-\\s]?)?0\\d{1,2}[-\\s]?\\d{3,4}[-\\s]?\\d{4}\\b");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${public-data.crawl.urls:}")
    private String urls;

    @Value("${public-data.crawl.allowed-hosts:www.motie.go.kr,motie.go.kr,www.kotra.or.kr,dream.kotra.or.kr,www.safetykorea.kr,www.data.go.kr}")
    private String allowedHosts;

    @Value("${public-data.crawl.timeout-note:robots-and-terms-must-be-reviewed-before-production}")
    private String timeoutNote;

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.MOTIE_WEB_CRAWL;
    }

    @Override
    public String usedFor() {
        return provider().usedFor();
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return "crawl:" + safe(request.getHsCode()) + ":" + safe(request.getTargetCountry()) + ":" + canonicalProduct(request);
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        List<String> targets = configuredUrls();
        if (targets.isEmpty()) {
            return fallback(request, "PUBLIC_DATA_CRAWL_URLS is empty");
        }

        Set<String> allowlist = allowedHostSet();
        List<Map<String, Object>> pages = targets.stream()
                .filter(url -> isAllowed(url, allowlist))
                .limit(5)
                .map(url -> crawlPage(url, request))
                .toList();

        if (pages.isEmpty()) {
            return fallback(request, "no crawl URL passed allowlist validation");
        }

        long successCount = pages.stream().filter(page -> Boolean.TRUE.equals(page.get("success"))).count();
        if (successCount == 0) {
            return fallback(request, "all crawl URLs failed: " + pages.stream()
                    .map(page -> safeString(page.get("error")))
                    .filter(value -> !value.isBlank())
                    .limit(3)
                    .collect(Collectors.joining("; ")));
        }

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("provider", provider().name());
        raw.put("collectedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        raw.put("pages", pages);

        Map<String, Object> metrics = metrics(request, pages);
        int contribution = scoreContribution(metrics);
        double confidence = confidence(metrics, successCount, pages.size());

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("provider", provider().name());
        normalized.put("usedFor", usedFor());
        normalized.put("hsCode", request.getHsCode());
        normalized.put("targetCountry", request.getTargetCountry());
        normalized.put("productName", request.getProductName());
        normalized.put("matchedKey", matchedKey(request));
        normalized.put("fallbackUsed", false);
        normalized.put("signal", "crawl");
        normalized.put("agency", provider().agency());
        normalized.put("ministry", provider().ministry());
        normalized.put("dataFamily", provider().dataFamily());
        normalized.put("motieRelated", provider().motieRelated());
        normalized.put("refreshCycleHours", provider().refreshCycleHours());
        normalized.put("metrics", metrics);
        normalized.put("metricSchema", metrics.keySet());
        normalized.put("scoreContribution", contribution);
        normalized.put("confidence", confidence);
        normalized.put("collectionEvidence", collectionEvidence(pages, allowlist));
        normalized.put("scoreFormula", "crawl contribution uses keyword/document hits only; it never replaces official API metrics");
        normalized.put("metricSummary", "pages=" + successCount + "/" + pages.size()
                + ", keywordHits=" + metrics.get("keywordHits")
                + ", documentHints=" + metrics.get("documentHintCount"));
        normalized.put("decisionImpact", "산업부/KOTRA 등 공개 웹 문서에서 제품·인증 키워드가 확인되면 규제 Gate 판단의 근거 신뢰도만 보조합니다.");

        return PublicDataConnectorResult.builder()
                .provider(provider())
                .endpoint(limit("crawl:" + pages.stream()
                        .map(page -> safeString(page.get("url")))
                        .collect(Collectors.joining(",")), MAX_ENDPOINT_CHARS))
                .requestKey(requestKey(request))
                .hsCode(request.getHsCode())
                .targetCountry(request.getTargetCountry())
                .matchedKey(matchedKey(request))
                .usedFor(usedFor())
                .rawPayload(toJson(raw))
                .normalizedPayload(normalized)
                .scoreContribution(contribution)
                .confidence(confidence)
                .sourceType("crawl")
                .fallbackUsed(false)
                .build();
    }

    private Map<String, Object> crawlPage(String url, PublicDataRequest request) {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("url", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "GrowGateLocalEX/1.0 public-data-crawler; contact=repository-owner");
            headers.set(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,text/plain;q=0.9,*/*;q=0.1");
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String body = safeString(response.getBody());
            String text = normalizeHtml(body);
            page.put("success", true);
            page.put("status", response.getStatusCode().value());
            page.put("contentLength", body.length());
            page.put("textLength", text.length());
            page.put("text", text);
            page.put("keywordHits", keywordHits(request, text));
            page.put("documentHintCount", count(text, "(인증|고시|공고|리콜|안전|수출|지원|FTA|CE|K-?Battery|배터리)"));
        } catch (RestClientException e) {
            page.put("success", false);
            page.put("error", sanitize(e.getMessage()));
        }
        return page;
    }

    private Map<String, Object> metrics(PublicDataRequest request, List<Map<String, Object>> pages) {
        int successCount = (int) pages.stream().filter(page -> Boolean.TRUE.equals(page.get("success"))).count();
        int keywordHits = pages.stream().mapToInt(page -> asInt(page.get("keywordHits"))).sum();
        int documentHints = pages.stream().mapToInt(page -> asInt(page.get("documentHintCount"))).sum();
        int hsHits = pages.stream().map(page -> safeString(page.get("text")))
                .mapToInt(text -> safe(request.getHsCode()).isBlank() ? 0 : count(text, Pattern.quote(request.getHsCode())))
                .sum();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("pageCount", pages.size());
        metrics.put("successPageCount", successCount);
        metrics.put("keywordHits", keywordHits);
        metrics.put("documentHintCount", documentHints);
        metrics.put("hsCodeHitCount", hsHits);
        metrics.put("recordCount", successCount);
        return metrics;
    }

    private int scoreContribution(Map<String, Object> metrics) {
        int success = asInt(metrics.get("successPageCount"));
        int keywordHits = asInt(metrics.get("keywordHits"));
        int documentHints = asInt(metrics.get("documentHintCount"));
        if (success == 0) return -1;
        return Math.min(4, 1 + Math.min(2, keywordHits / 2) + Math.min(1, documentHints / 3));
    }

    private double confidence(Map<String, Object> metrics, long successCount, int totalCount) {
        if (successCount == 0 || totalCount == 0) return 0.0;
        double coverage = Math.min(1.0, successCount / (double) totalCount);
        double evidence = Math.min(0.35, asInt(metrics.get("keywordHits")) * 0.04 + asInt(metrics.get("documentHintCount")) * 0.02);
        return Math.min(0.72, 0.25 + coverage * 0.20 + evidence);
    }

    private Map<String, Object> collectionEvidence(List<Map<String, Object>> pages, Set<String> allowlist) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("collectionMode", "safe_public_web_crawl");
        evidence.put("allowlistHosts", allowlist);
        evidence.put("pageLimit", 5);
        evidence.put("maxRawChars", MAX_RAW_CHARS);
        evidence.put("maxTextChars", MAX_TEXT_CHARS);
        evidence.put("secretMasked", true);
        evidence.put("personalInfoMasked", true);
        evidence.put("robotsReviewNote", timeoutNote);
        evidence.put("successfulUrls", pages.stream()
                .filter(page -> Boolean.TRUE.equals(page.get("success")))
                .map(page -> safeString(page.get("url")))
                .toList());
        evidence.put("failedUrls", pages.stream()
                .filter(page -> !Boolean.TRUE.equals(page.get("success")))
                .map(page -> Map.of("url", safeString(page.get("url")), "error", safeString(page.get("error"))))
                .toList());
        return evidence;
    }

    private List<String> configuredUrls() {
        if (urls == null || urls.isBlank()) return List.of();
        return Arrays.stream(urls.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private Set<String> allowedHostSet() {
        return Arrays.stream(safe(allowedHosts).split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private boolean isAllowed(String url, Set<String> allowlist) {
        try {
            URI uri = new URI(url);
            String scheme = safe(uri.getScheme()).toLowerCase(Locale.ROOT);
            String host = safe(uri.getHost()).toLowerCase(Locale.ROOT);
            return "https".equals(scheme) && allowlist.contains(host);
        } catch (URISyntaxException e) {
            log.warn("crawl URL rejected: {}", sanitize(url));
            return false;
        }
    }

    private String normalizeHtml(String html) {
        String limited = safe(html);
        if (limited.length() > MAX_RAW_CHARS) limited = limited.substring(0, MAX_RAW_CHARS);
        String text = SCRIPT_STYLE.matcher(limited).replaceAll(" ");
        text = TAG.matcher(text).replaceAll(" ");
        text = text.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
        text = mask(text);
        text = SPACE.matcher(text).replaceAll(" ").trim();
        return text.length() > MAX_TEXT_CHARS ? text.substring(0, MAX_TEXT_CHARS) : text;
    }

    private int keywordHits(PublicDataRequest request, String text) {
        String lower = safe(text).toLowerCase(Locale.ROOT);
        int hits = 0;
        if (!safe(request.getHsCode()).isBlank() && lower.contains(request.getHsCode().toLowerCase(Locale.ROOT))) hits++;
        if (!safe(request.getTargetCountry()).isBlank() && lower.contains(request.getTargetCountry().toLowerCase(Locale.ROOT))) hits++;
        if (!safe(request.getProductName()).isBlank()) {
            for (String token : request.getProductName().split("\\s+")) {
                if (token.length() >= 2 && lower.contains(token.toLowerCase(Locale.ROOT))) hits++;
            }
        }
        return hits;
    }

    private String matchedKey(PublicDataRequest request) {
        return "crawl;hsCode=" + safe(request.getHsCode()) + ";country=" + safe(request.getTargetCountry());
    }

    private String fallbackReason(String reason) {
        return reason == null || reason.isBlank() ? "crawl unavailable" : sanitize(reason);
    }

    private PublicDataConnectorResult fallback(PublicDataRequest request, String reason) {
        String safeReason = fallbackReason(reason);
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("provider", provider().name());
        normalized.put("usedFor", usedFor());
        normalized.put("hsCode", request.getHsCode());
        normalized.put("targetCountry", request.getTargetCountry());
        normalized.put("productName", request.getProductName());
        normalized.put("matchedKey", matchedKey(request));
        normalized.put("fallbackUsed", true);
        normalized.put("signal", "fallback");
        normalized.put("metrics", Map.of("recordCount", 0, "pageCount", 0));
        normalized.put("rawCount", 0);
        normalized.put("normalizedCount", 0);
        normalized.put("filteredCount", 0);
        normalized.put("zeroReason", zeroReason(safeReason));
        normalized.put("requestParamsMasked", Map.of());
        normalized.put("sampleRawKeys", List.of());
        normalized.put("scoreContribution", -1);
        normalized.put("confidence", 0.0);
        normalized.put("reason", safeReason);
        normalized.put("collectionEvidence", Map.of(
                "collectionMode", "safe_public_web_crawl",
                "fallbackUsed", true,
                "reason", safeReason
        ));
        normalized.put("zeroResultDiagnostics", Map.of(
                "provider", provider().name(),
                "agent", usedFor(),
                "sourceType", "fallback",
                "rawCount", 0,
                "normalizedCount", 0,
                "filteredCount", 0,
                "zeroReason", zeroReason(safeReason),
                "sampleRawKeys", List.of(),
                "fallbackUsed", true
        ));
        normalized.put("scoreFormula", "crawl fallback contributes no positive score");
        return PublicDataConnectorResult.builder()
                .provider(provider())
                .endpoint("crawl")
                .requestKey(requestKey(request))
                .hsCode(request.getHsCode())
                .targetCountry(request.getTargetCountry())
                .matchedKey(matchedKey(request))
                .usedFor(usedFor())
                .rawPayload(toJson(Map.of("fallback", true, "reason", safeReason)))
                .normalizedPayload(normalized)
                .scoreContribution(-1)
                .confidence(0.0)
                .sourceType("fallback")
                .fallbackUsed(true)
                .errorMessage(safeReason)
                .build();
    }

    private String mask(String text) {
        String masked = SECRET.matcher(safe(text)).replaceAll("$1=[REDACTED]");
        masked = EMAIL.matcher(masked).replaceAll("[EMAIL_REDACTED]");
        return PHONE.matcher(masked).replaceAll("[PHONE_REDACTED]");
    }

    private String sanitize(String value) {
        return mask(safe(value));
    }

    private String zeroReason(String reason) {
        String lower = safe(reason).toLowerCase(Locale.ROOT);
        if (lower.contains("urls is empty")) return "CRAWL_URLS_EMPTY";
        if (lower.contains("allowlist")) return "CRAWL_URL_NOT_ALLOWLISTED";
        if (lower.contains("all crawl urls failed")) return "CRAWL_FETCH_FAILED";
        return "CRAWL_SKIPPED_OR_FAILED";
    }

    private String canonicalProduct(PublicDataRequest request) {
        return safe(request.getProductName()).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9가-힣]+", "-");
    }

    private String limit(String value, int maxLength) {
        String safeValue = safe(value);
        return safeValue.length() > maxLength ? safeValue.substring(0, maxLength) : safeValue;
    }

    private int count(String text, String regex) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(safe(text));
        int count = 0;
        while (matcher.find() && count < 200) count++;
        return count;
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String safeString(Object value) {
        return safe(value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
