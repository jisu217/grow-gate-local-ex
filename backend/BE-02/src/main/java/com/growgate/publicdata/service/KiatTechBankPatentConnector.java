package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.dto.PublicDataSignal;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class KiatTechBankPatentConnector extends AbstractPublicDataConnector {

    @Value("${public-data.kiat-tech-bank-patent.url:https://apis.data.go.kr/B552536/tech_3/techpatent}")
    private String endpoint;

    @Value("${public-data.kiat-tech-bank-patent.key:${KIAT_TECH_BANK_PATENT_API_KEY:${KIAT_TECH_BANK_API_KEY:${DATAGO_SERVICE_KEY:}}}}")
    private String apiKey;

    @Value("${public-data.kiat-tech-bank-patent.key-param:serviceKey}")
    private String keyParam;

    @Value("${public-data.kiat-tech-bank-patent.page-size:10}")
    private int pageSize;

    public KiatTechBankPatentConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KIAT_TECH_BANK_PATENT;
    }

    @Override
    public String usedFor() {
        return "fto";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + safe(request.getProductName());
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        return getJsonWithKeyParam(request, endpoint, apiKey, keyParam, Map.of(
                "pageNo", "1",
                "numOfRows", String.valueOf(Math.max(1, pageSize)),
                "type", "json"
        ));
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    @Override
    protected Map<String, String> fieldMapping() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("patentRecordCount", "JSON/XML item/data/body/items rows from KIAT tech bank patent lookup");
        fields.put("productMentions", "patent/technology title, abstract, or raw payload contains product keyword");
        fields.put("applicationNumberCount", "application/registration number-like fields");
        fields.put("technologyTitleCount", "technology/patent title-like fields");
        fields.put("recordCount", "normalized KIAT tech bank patent record count");
        return fields;
    }

    @Override
    protected Map<String, Object> collectionEvidence(String endpoint, String apiKey, boolean fallbackUsed, String reason) {
        Map<String, Object> evidence = new LinkedHashMap<>(super.collectionEvidence(endpoint, apiKey, fallbackUsed, reason));
        evidence.put("collectionMode", "live_operation_api");
        evidence.put("serviceName", "한국산업기술진흥원_기술은행 등록기술 특허 조회 서비스_GW");
        evidence.put("officialUrl", provider().officialUrl());
        evidence.put("requestShapeVerifiedWithoutKey", true);
        evidence.put("unauthenticatedSmokeResult", "Unexpected errors");
        evidence.put("keyParameter", keyParam);
        return evidence;
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        JsonNode root = readTree(rawPayload);
        int recordCount = fallbackUsed ? 0 : explicitRecordCount(root, rawPayload);
        int productMentions = fallbackUsed ? 0 : Math.max(textHitCount(root, safe(request.getProductName())),
                textHitCount(rawPayload, safe(request.getProductName())));
        int applicationNumberCount = fallbackUsed ? 0 : fieldHitCount(root,
                "applicationNumber", "applicationNo", "applNo", "patentNo", "registrationNumber", "등록번호", "출원번호", "특허번호");
        int technologyTitleCount = fallbackUsed ? 0 : fieldHitCount(root,
                "techName", "technologyName", "title", "inventionTitle", "patentTitle", "기술명", "발명의명칭", "특허명");

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("patentRecordCount", recordCount);
        metrics.put("productMentions", productMentions);
        metrics.put("applicationNumberCount", applicationNumberCount);
        metrics.put("technologyTitleCount", technologyTitleCount);
        metrics.put("recordCount", recordCount);

        int contribution = fallbackUsed ? -3 : recordCount == 0 ? 0
                : -Math.min(7, Math.max(1, recordCount / 3) + Math.min(3, productMentions));
        double confidence = fallbackUsed ? 0.0 : Math.min(0.80,
                0.34 + Math.min(0.24, recordCount * 0.03)
                        + Math.min(0.12, productMentions * 0.04)
                        + (applicationNumberCount > 0 ? 0.08 : 0));
        return signal(request, metrics, contribution, confidence);
    }

    private int fieldHitCount(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull() || node.isMissingNode()) return 0;
        int count = 0;
        if (node.isObject()) {
            for (String fieldName : fieldNames) {
                JsonNode value = node.get(fieldName);
                if (value != null && !value.asText("").isBlank()) count++;
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                count += fieldHitCount(fields.next().getValue(), fieldNames);
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) count += fieldHitCount(child, fieldNames);
        }
        return count;
    }

    private int explicitRecordCount(JsonNode root, String rawPayload) {
        int rowCount = Math.max(Math.max(arraySizeAt(root, "item"), arraySizeAt(root, "data")),
                Math.max(arraySizeAt(root, "patents"), arraySizeAt(root, "rows")));
        if (rowCount > 0) return rowCount;
        return Math.max(0, estimateRecordCount(rawPayload));
    }

    private int arraySizeAt(JsonNode node, String fieldName) {
        if (node == null || node.isNull() || node.isMissingNode()) return 0;
        if (node.isObject()) {
            JsonNode value = node.get(fieldName);
            int own = 0;
            if (value != null && value.isArray()) {
                own = value.size();
            } else if (value != null && value.isObject()) {
                own = 1;
            }

            int nested = 0;
            var fields = node.fields();
            while (fields.hasNext()) {
                nested = Math.max(nested, arraySizeAt(fields.next().getValue(), fieldName));
            }
            return Math.max(own, nested);
        }
        if (node.isArray()) {
            int nested = 0;
            for (JsonNode child : node) {
                nested = Math.max(nested, arraySizeAt(child, fieldName));
            }
            return nested;
        }
        return 0;
    }

    private int textHitCount(String rawPayload, String keyword) {
        if (rawPayload == null || rawPayload.isBlank() || keyword == null || keyword.isBlank()) return 0;
        String lowerBody = rawPayload.toLowerCase(java.util.Locale.ROOT);
        String lowerKeyword = keyword.toLowerCase(java.util.Locale.ROOT);
        int count = 0;
        int index = lowerBody.indexOf(lowerKeyword);
        while (index >= 0 && count < 50) {
            count++;
            index = lowerBody.indexOf(lowerKeyword, index + lowerKeyword.length());
        }
        return count;
    }
}
