package com.growgate.publicdata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import lombok.RequiredArgsConstructor;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "public-data.export-evidence", name = "enabled", havingValue = "true")
public class ExportEvidenceFeedConnector implements PublicDataConnector {

    private static final int MAX_FEEDS = 8;
    private static final int MAX_ITEMS_PER_FEED = 20;
    private static final int MAX_RAW_CHARS = 180_000;
    private static final Pattern ITEM = Pattern.compile("(?is)<item\\b[^>]*>(.*?)</item>|<entry\\b[^>]*>(.*?)</entry>");
    private static final Pattern TITLE = Pattern.compile("(?is)<title\\b[^>]*>(.*?)</title>");
    private static final Pattern LINK = Pattern.compile("(?is)<link\\b[^>]*>(.*?)</link>|<link\\b[^>]*href=[\"']([^\"']+)[\"'][^>]*/?>");
    private static final Pattern SUMMARY = Pattern.compile("(?is)<(?:description|summary|content)\\b[^>]*>(.*?)</(?:description|summary|content)>");
    private static final Pattern DATE = Pattern.compile("(?is)<(?:pubDate|published|updated|dc:date)\\b[^>]*>(.*?)</(?:pubDate|published|updated|dc:date)>|\\b20\\d{2}[-./]\\d{1,2}[-./]\\d{1,2}\\b");
    private static final Pattern TAG = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern SPACE = Pattern.compile("\\s+");
    private static final Pattern SECRET = Pattern.compile("(?i)(serviceKey|apiKey|apikey|token|secret|password|authorization)\\s*[=:]\\s*[^\\s&]+");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${public-data.export-evidence.urls:}")
    private String urls;

    @Value("${public-data.export-evidence.allowed-hosts:www.motie.go.kr,motie.go.kr,dream.kotra.or.kr,www.kotra.or.kr,eiec.kdi.re.kr,www.kiet.re.kr,www.kita.net}")
    private String allowedHosts;

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.EXPORT_EVIDENCE_FEED;
    }

    @Override
    public String usedFor() {
        return provider().usedFor();
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider().name() + ":" + safe(request.getTargetCountry()) + ":" + safe(request.getProductName());
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        List<String> targets = configuredUrls();
        if (targets.isEmpty()) return fallback(request, "PUBLIC_DATA_EXPORT_EVIDENCE_URLS is empty");

        Set<String> allowlist = allowedHostSet();
        List<Map<String, Object>> feedResults = targets.stream()
                .filter(url -> isAllowed(url, allowlist))
                .limit(MAX_FEEDS)
                .map(url -> fetchFeed(url, request))
                .toList();
        if (feedResults.isEmpty()) return fallback(request, "no export evidence feed URL passed allowlist validation");

        List<Map<String, Object>> items = feedResults.stream()
                .filter(feed -> Boolean.TRUE.equals(feed.get("success")))
                .flatMap(feed -> itemList(feed.get("items")).stream())
                .toList();
        if (items.isEmpty()) return fallback(request, "all export evidence feeds failed or returned no items");

        Map<String, Object> metrics = metrics(request, items);
        int contribution = scoreContribution(metrics);
        double confidence = confidence(metrics, items.size(), feedResults.size());
        Map<String, Object> normalized = normalizedPayload(request, feedResults, items, metrics, contribution, confidence, allowlist);
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("provider", provider().name());
        raw.put("collectedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        raw.put("feeds", feedResults);

        return PublicDataConnectorResult.builder()
                .provider(provider())
                .endpoint(limit("feed:" + targets.stream().limit(MAX_FEEDS).collect(Collectors.joining(",")), 480))
                .requestKey(requestKey(request))
                .hsCode(request.getHsCode())
                .targetCountry(request.getTargetCountry())
                .matchedKey(matchedKey(request))
                .usedFor(usedFor())
                .rawPayload(toJson(raw))
                .normalizedPayload(normalized)
                .scoreContribution(contribution)
                .confidence(confidence)
                .sourceType("external_feed")
                .fallbackUsed(false)
                .build();
    }

    private Map<String, Object> fetchFeed(String url, PublicDataRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "GrowGateLocalEX/1.0 export-evidence-feed; contact=repository-owner");
            headers.set(HttpHeaders.ACCEPT, "application/rss+xml,application/atom+xml,application/json,text/xml;q=0.9,*/*;q=0.1");
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String body = limit(safe(response.getBody()), MAX_RAW_CHARS);
            List<Map<String, Object>> parsed = parseItems(body, request);
            result.put("success", true);
            result.put("status", response.getStatusCode().value());
            result.put("itemCount", parsed.size());
            result.put("items", parsed);
        } catch (RestClientException e) {
            result.put("success", false);
            result.put("error", sanitize(e.getMessage()));
        }
        return result;
    }

    private List<Map<String, Object>> parseItems(String body, PublicDataRequest request) {
        if (body == null || body.isBlank()) return List.of();
        String trimmed = body.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) return parseJsonItems(trimmed, request);
        return parseXmlItems(trimmed, request);
    }

    private List<Map<String, Object>> parseJsonItems(String body, PublicDataRequest request) {
        try {
            JsonNode root = objectMapper.readTree(body);
            List<JsonNode> nodes = new ArrayList<>();
            collectJsonItems(root, nodes);
            return nodes.stream()
                    .limit(MAX_ITEMS_PER_FEED)
                    .map(node -> item(
                            firstText(node, "title", "subject", "name"),
                            firstText(node, "link", "url", "originUrl"),
                            firstText(node, "summary", "description", "content", "body"),
                            firstText(node, "publishedAt", "pubDate", "createdAt", "date"),
                            request
                    ))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private void collectJsonItems(JsonNode node, List<JsonNode> items) {
        if (node == null || node.isNull() || items.size() >= MAX_ITEMS_PER_FEED * MAX_FEEDS) return;
        if (node.isArray()) {
            node.forEach(child -> collectJsonItems(child, items));
            return;
        }
        if (node.isObject()) {
            if (node.has("title") || node.has("subject") || node.has("name")) items.add(node);
            node.fields().forEachRemaining(entry -> collectJsonItems(entry.getValue(), items));
        }
    }

    private List<Map<String, Object>> parseXmlItems(String body, PublicDataRequest request) {
        List<Map<String, Object>> items = new ArrayList<>();
        Matcher matcher = ITEM.matcher(body);
        while (matcher.find() && items.size() < MAX_ITEMS_PER_FEED) {
            String block = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            items.add(item(firstMatch(TITLE, block), firstMatch(LINK, block),
                    firstMatch(SUMMARY, block), firstDate(block), request));
        }
        return items;
    }

    private Map<String, Object> item(String title, String link, String summary, String publishedAt, PublicDataRequest request) {
        String text = normalizeText(title + " " + summary);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", limit(normalizeText(title), 220));
        item.put("url", limit(normalizeText(link), 480));
        item.put("summary", limit(text, 900));
        item.put("publishedAt", normalizeDate(publishedAt));
        item.put("targetCountryMention", hit(text, request.getTargetCountry()));
        item.put("productMention", hit(text, request.getProductName()));
        item.put("exportKeywordHit", count(text, "(수출|통상|무역|관세|인증|리콜|FTA|export|trade|customs|certification)"));
        return item;
    }

    private Map<String, Object> metrics(PublicDataRequest request, List<Map<String, Object>> items) {
        int countryMentions = items.stream().mapToInt(item -> bool(item.get("targetCountryMention")) ? 1 : 0).sum();
        int productMentions = items.stream().mapToInt(item -> bool(item.get("productMention")) ? 1 : 0).sum();
        int keywordHits = items.stream().mapToInt(item -> asInt(item.get("exportKeywordHit"))).sum();
        int recentCount = (int) items.stream().filter(item -> !safe(item.get("publishedAt")).isBlank()).count();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("documentCount", items.size());
        metrics.put("recentDocumentCount", recentCount);
        metrics.put("targetCountryMentions", countryMentions);
        metrics.put("productMentions", productMentions);
        metrics.put("exportKeywordHits", keywordHits);
        metrics.put("recordCount", items.size());
        return metrics;
    }

    private int scoreContribution(Map<String, Object> metrics) {
        return Math.min(3, Math.min(1, asInt(metrics.get("documentCount")))
                + Math.min(1, asInt(metrics.get("targetCountryMentions")))
                + Math.min(1, asInt(metrics.get("exportKeywordHits")) / 2));
    }

    private double confidence(Map<String, Object> metrics, int itemCount, int feedCount) {
        if (itemCount == 0 || feedCount == 0) return 0.0;
        return Math.min(0.65, 0.25
                + Math.min(0.20, itemCount * 0.02)
                + Math.min(0.10, asInt(metrics.get("recentDocumentCount")) * 0.02)
                + Math.min(0.10, (asInt(metrics.get("targetCountryMentions")) + asInt(metrics.get("productMentions"))) * 0.03));
    }

    private Map<String, Object> normalizedPayload(PublicDataRequest request, List<Map<String, Object>> feeds,
                                                  List<Map<String, Object>> items, Map<String, Object> metrics,
                                                  int contribution, double confidence, Set<String> allowlist) {
        List<String> dates = items.stream().map(item -> safe(item.get("publishedAt")))
                .filter(value -> !value.isBlank())
                .sorted(Comparator.naturalOrder())
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("provider", provider().name());
        payload.put("usedFor", usedFor());
        payload.put("hsCode", request.getHsCode());
        payload.put("targetCountry", request.getTargetCountry());
        payload.put("productName", request.getProductName());
        payload.put("matchedKey", matchedKey(request));
        payload.put("fallbackUsed", false);
        payload.put("signal", "external_feed");
        payload.put("agency", provider().agency());
        payload.put("ministry", provider().ministry());
        payload.put("dataFamily", provider().dataFamily());
        payload.put("motieRelated", provider().motieRelated());
        payload.put("refreshCycleHours", provider().refreshCycleHours());
        payload.put("metrics", metrics);
        payload.put("metricSchema", metrics.keySet());
        payload.put("fieldMapping", Map.of(
                "documentCount", "RSS/Atom/API item count",
                "recentDocumentCount", "publishedAt/pubDate/date present count",
                "targetCountryMentions", "title/summary target country mention",
                "productMentions", "title/summary product mention",
                "exportKeywordHits", "export/trade/customs/certification keyword hits"
        ));
        payload.put("scoreContribution", contribution);
        payload.put("confidence", confidence);
        payload.put("collectionEvidence", Map.of(
                "collectionMode", "allowlisted_rss_or_json_feed",
                "allowlistHosts", allowlist,
                "feedLimit", MAX_FEEDS,
                "itemLimitPerFeed", MAX_ITEMS_PER_FEED,
                "successfulFeeds", feeds.stream().filter(feed -> Boolean.TRUE.equals(feed.get("success"))).count(),
                "secretMasked", true,
                "fallbackUsed", false
        ));
        payload.put("sampleItems", items.stream().limit(5).toList());
        if (!dates.isEmpty()) {
            payload.put("sourcePeriodStart", dates.get(0));
            payload.put("sourcePeriodEnd", dates.get(dates.size() - 1));
            payload.put("sourcePeriod", dates.get(0) + " ~ " + dates.get(dates.size() - 1));
            payload.put("lastPublishedAt", dates.get(dates.size() - 1));
        }
        payload.put("metricSummary", "documents=" + metrics.get("documentCount")
                + ", countryMentions=" + metrics.get("targetCountryMentions")
                + ", exportKeywordHits=" + metrics.get("exportKeywordHits"));
        payload.put("scoreFormula", "external feed contribution is capped at +3 and only supports market evidence freshness; it never replaces official API records");
        return payload;
    }

    private PublicDataConnectorResult fallback(PublicDataRequest request, String reason) {
        String safeReason = sanitize(reason);
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("provider", provider().name());
        normalized.put("usedFor", usedFor());
        normalized.put("hsCode", request.getHsCode());
        normalized.put("targetCountry", request.getTargetCountry());
        normalized.put("productName", request.getProductName());
        normalized.put("matchedKey", matchedKey(request));
        normalized.put("fallbackUsed", true);
        normalized.put("signal", "fallback");
        normalized.put("agency", provider().agency());
        normalized.put("ministry", provider().ministry());
        normalized.put("dataFamily", provider().dataFamily());
        normalized.put("motieRelated", provider().motieRelated());
        normalized.put("refreshCycleHours", provider().refreshCycleHours());
        normalized.put("metrics", Map.of("recordCount", 0));
        normalized.put("metricSchema", List.of("recordCount"));
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
                "collectionMode", "allowlisted_rss_or_json_feed",
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
        normalized.put("scoreFormula", "failed external feed collection contributes fallback penalty and zero confidence");
        return PublicDataConnectorResult.builder()
                .provider(provider())
                .endpoint("export-evidence-feed")
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

    private List<String> configuredUrls() {
        if (urls == null || urls.isBlank()) return List.of();
        return java.util.Arrays.stream(urls.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private Set<String> allowedHostSet() {
        return java.util.Arrays.stream(safe(allowedHosts).split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private boolean isAllowed(String url, Set<String> allowlist) {
        try {
            String host = new URI(url).getHost();
            return host != null && allowlist.contains(host.toLowerCase(Locale.ROOT));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> itemList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private String firstMatch(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body == null ? "" : body);
        if (!matcher.find()) return "";
        for (int i = 1; i <= matcher.groupCount(); i++) {
            if (matcher.group(i) != null) return normalizeText(matcher.group(i));
        }
        return "";
    }

    private String firstDate(String body) {
        Matcher matcher = DATE.matcher(body == null ? "" : body);
        if (!matcher.find()) return "";
        for (int i = 1; i <= matcher.groupCount(); i++) {
            if (matcher.group(i) != null) return matcher.group(i);
        }
        return matcher.group();
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = findField(node, name);
            if (value != null && value.isValueNode()) return value.asText("");
        }
        return "";
    }

    private JsonNode findField(JsonNode node, String name) {
        if (node == null || node.isNull()) return null;
        if (node.has(name)) return node.get(name);
        if (node.isContainerNode()) {
            for (JsonNode child : node) {
                JsonNode found = findField(child, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String normalizeText(String value) {
        String text = TAG.matcher(safe(value)).replaceAll(" ");
        return SPACE.matcher(text.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")).replaceAll(" ").trim();
    }

    private String normalizeDate(String value) {
        if (value == null || value.isBlank()) return "";
        Matcher matcher = Pattern.compile("\\b20\\d{2}[-./]\\d{1,2}[-./]\\d{1,2}\\b").matcher(value);
        if (matcher.find()) return matcher.group().replace('.', '-').replace('/', '-');
        return limit(normalizeText(value), 40);
    }

    private String matchedKey(PublicDataRequest request) {
        return "feed=" + safe(request.getTargetCountry()) + ";product=" + safe(request.getProductName());
    }

    private boolean hit(String text, String query) {
        return !safe(query).isBlank() && safe(text).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private int count(String text, String regex) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(safe(text));
        int count = 0;
        while (matcher.find() && count < 100) count++;
        return count;
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private boolean bool(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String limit(String value, int max) {
        String text = safe(value);
        return text.length() <= max ? text : text.substring(0, max);
    }

    private String sanitize(String value) {
        return SECRET.matcher(safe(value)).replaceAll("$1=[REDACTED]");
    }

    private String zeroReason(String reason) {
        String lower = safe(reason).toLowerCase(Locale.ROOT);
        if (lower.contains("urls is empty")) return "FEED_URLS_EMPTY";
        if (lower.contains("allowlist")) return "FEED_URL_NOT_ALLOWLISTED";
        if (lower.contains("no items")) return "FEED_RAW_COUNT_ZERO";
        return "FEED_FETCH_FAILED";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
