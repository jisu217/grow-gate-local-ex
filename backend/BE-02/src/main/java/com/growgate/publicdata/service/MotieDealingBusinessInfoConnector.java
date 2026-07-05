package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.dto.PublicDataSignal;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MotieDealingBusinessInfoConnector extends AbstractPublicDataConnector {

    @Value("${public-data.motie-dealing-business-info.url:https://apis.data.go.kr/1450000/delngBsshInfoService}")
    private String endpoint;

    @Value("${public-data.motie-dealing-business-info.key:${MOTIE_DEALING_BUSINESS_INFO_API_KEY:${DATAGO_SERVICE_KEY:}}}")
    private String apiKey;

    @Value("${public-data.motie-dealing-business-info.key-param:ServiceKey}")
    private String keyParam;

    @Value("${public-data.motie-dealing-business-info.operation:getDelngSttusInqire}")
    private String operation;

    public MotieDealingBusinessInfoConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.MOTIE_DEALING_BUSINESS_INFO;
    }

    @Override
    public String usedFor() {
        return "market";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + hscdSix(request);
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        String hscdSix = hscdSix(request);
        if (hscdSix.isBlank()) {
            return success(request, operationEndpoint(), apiKey, keyParam,
                    Map.of("hscdSix", ""), "", 200, 0);
        }
        return getJsonWithKeyParam(request, operationEndpoint(), apiKey, keyParam, Map.of(
                "hscdSix", hscdSix
        ));
    }

    @Override
    protected String endpointLabel() {
        return operationEndpoint();
    }

    @Override
    protected Map<String, String> fieldMapping() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("dealingRecordCount", "XML response/body/items/item rows");
        fields.put("totalCount", "response/body/totalCount when present");
        fields.put("tradeCount", "sum of count/dealEnterpriseCount fields");
        fields.put("regionCount", "distinct purchaserSidoName/supplierSidoName values");
        fields.put("yearCount", "distinct year values");
        fields.put("itemNameCount", "distinct itemName values when item operation is configured");
        fields.put("recordCount", "normalized dealing business info row count");
        return fields;
    }

    @Override
    protected Map<String, Object> collectionEvidence(String endpoint, String apiKey, boolean fallbackUsed, String reason) {
        Map<String, Object> evidence = new LinkedHashMap<>(super.collectionEvidence(endpoint, apiKey, fallbackUsed, reason));
        evidence.put("collectionMode", "live_operation_api");
        evidence.put("serviceName", "산업통상부_거래업체정보서비스");
        evidence.put("operation", operation);
        evidence.put("operationNameKo", "거래 현황 조회");
        evidence.put("documentName", "IROS4_OA_DV_0401_OpenAPI활용가이드_무역정보(거래업체정보서비스)_v1.7.docx");
        evidence.put("portalModifiedDate", "2026-03-25");
        evidence.put("portalUpdateCycle", "실시간");
        evidence.put("portalDataFormat", "XML");
        evidence.put("keyParameter", keyParam);
        evidence.put("requiredParameters", "ServiceKey,hscdSix");
        return evidence;
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        int recordCount = fallbackUsed ? 0 : estimateRecordCount(rawPayload);
        int totalCount = fallbackUsed ? 0 : firstIntTag(rawPayload, "totalCount");
        long tradeCount = fallbackUsed ? 0 : Math.round(sumTags(rawPayload, "count", "dealEnterpriseCount"));
        int regionCount = fallbackUsed ? 0 : distinctTagCount(rawPayload, "purchaserSidoName", "supplierSidoName");
        int yearCount = fallbackUsed ? 0 : distinctTagCount(rawPayload, "year");
        int itemNameCount = fallbackUsed ? 0 : distinctTagCount(rawPayload, "itemName");

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("dealingRecordCount", recordCount);
        metrics.put("totalCount", totalCount);
        metrics.put("tradeCount", tradeCount);
        metrics.put("regionCount", regionCount);
        metrics.put("yearCount", yearCount);
        metrics.put("itemNameCount", itemNameCount);
        metrics.put("recordCount", recordCount);

        int contribution = fallbackUsed ? -2 : recordCount == 0 ? 0
                : Math.min(7, 2 + Math.min(3, recordCount / 2)
                + (tradeCount > 0 ? 1 : 0)
                + (regionCount > 1 ? 1 : 0));
        double confidence = fallbackUsed ? 0.0 : Math.min(0.82,
                0.32 + Math.min(0.20, recordCount * 0.03)
                        + (tradeCount > 0 ? 0.14 : 0)
                        + (regionCount > 0 ? 0.10 : 0)
                        + (yearCount > 0 ? 0.06 : 0));
        return signal(request, metrics, contribution, confidence);
    }

    private String operationEndpoint() {
        String base = safe(endpoint);
        String op = safe(operation);
        if (base.isBlank() || op.isBlank()) return base;
        return base.endsWith("/") ? base + op : base + "/" + op;
    }

    private String hscdSix(PublicDataRequest request) {
        String digits = safe(request.getHsCode()).replaceAll("\\D", "");
        if (digits.length() <= 6) return digits;
        return digits.substring(0, 6);
    }

    private double sumTags(String rawPayload, String... tagNames) {
        double sum = 0;
        for (String tagName : tagNames) {
            Matcher matcher = tagPattern(tagName).matcher(safe(rawPayload));
            while (matcher.find()) {
                String value = matcher.group(1).replace(",", "").trim();
                try {
                    sum += Double.parseDouble(value);
                } catch (NumberFormatException ignored) {
                    // Keep parsing the rest of the official XML response.
                }
            }
        }
        return sum;
    }

    private int firstIntTag(String rawPayload, String tagName) {
        Matcher matcher = tagPattern(tagName).matcher(safe(rawPayload));
        if (!matcher.find()) return 0;
        try {
            return Integer.parseInt(matcher.group(1).replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int distinctTagCount(String rawPayload, String... tagNames) {
        Set<String> values = new LinkedHashSet<>();
        for (String tagName : tagNames) {
            Matcher matcher = tagPattern(tagName).matcher(safe(rawPayload));
            while (matcher.find()) {
                String value = matcher.group(1).trim();
                if (!value.isBlank()) values.add(value);
            }
        }
        return values.size();
    }

    private Pattern tagPattern(String tagName) {
        return Pattern.compile("<" + Pattern.quote(tagName) + "\\b[^>]*>(.*?)</" + Pattern.quote(tagName) + ">",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }
}
