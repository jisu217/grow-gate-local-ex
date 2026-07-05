package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.dto.PublicDataSignal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractConsumerRecallConnector extends AbstractPublicDataConnector {

    @Value("${public-data.consumer-recall.url:https://www.consumer.go.kr/openapi/recall/contents/index.do}")
    private String endpoint;

    @Value("${public-data.consumer-recall.count-per-page:20}")
    private int countPerPage;

    protected AbstractConsumerRecallConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    protected abstract String apiKey();

    protected abstract String menuId();

    protected abstract String menuName();

    @Override
    public String usedFor() {
        return "recall";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + menuId() + ":" + normalizeToken(request.getProductName());
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("pageNo", "1");
        params.put("cntPerPage", String.valueOf(Math.max(1, countPerPage)));
        params.put("cntntsId", menuId());
        if (request.getProductName() != null && !request.getProductName().isBlank()) {
            params.put("productNm", request.getProductName());
        }
        return getJsonWithKeyParam(request, endpoint, apiKey(), "serviceKey", params);
    }

    @Override
    protected Map<String, String> fieldMapping() {
        return Map.of(
                "recordCount", "Consumer24 recall item count",
                "allCount", "allCnt response field",
                "normalResponse", "code=00 NORMAL SERVICE",
                "noDataResponse", "code=30 NODATA_ERROR",
                "menuId", "Consumer24 recall cntntsId"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        int allCount = intTag(rawPayload, "allCnt");
        int itemCount = countMatches(rawPayload, "<(?:item|contents)\\b");
        String code = tag(rawPayload, "code");
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", fallbackUsed ? 0 : Math.max(allCount, itemCount));
        metrics.put("allCount", fallbackUsed ? 0 : allCount);
        metrics.put("menuId", menuId());
        metrics.put("menuName", menuName());
        metrics.put("normalResponse", "00".equals(code));
        metrics.put("noDataResponse", "30".equals(code));
        metrics.put("productNameHits", productNameHits(rawPayload, request.getProductName()));
        int contribution = fallbackUsed ? -1 : Math.max(allCount, itemCount) > 0 ? -2 : 0;
        double confidence = fallbackUsed ? 0.0 : "00".equals(code) ? 0.70 : "30".equals(code) ? 0.50 : 0.35;
        return signal(request, metrics, contribution, confidence);
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    private int productNameHits(String rawPayload, String productName) {
        String body = rawPayload == null ? "" : rawPayload;
        int hits = 0;
        for (String token : safe(productName).split("[\\s,/()\\-]+")) {
            if (token.length() >= 2 && body.contains(token)) hits++;
        }
        return hits;
    }

    private int intTag(String rawPayload, String tagName) {
        try {
            return Integer.parseInt(tag(rawPayload, tagName).replaceAll("[^0-9-]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private String tag(String rawPayload, String tagName) {
        if (rawPayload == null || rawPayload.isBlank()) return "";
        Matcher matcher = Pattern.compile("<" + Pattern.quote(tagName) + ">(.*?)</" + Pattern.quote(tagName) + ">",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(rawPayload);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private int countMatches(String body, String regex) {
        if (body == null || body.isBlank()) return 0;
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(body);
        int count = 0;
        while (matcher.find() && count < 500) count++;
        return count;
    }

    private String normalizeToken(String value) {
        return safe(value).toLowerCase().replaceAll("\\s+", "");
    }
}
