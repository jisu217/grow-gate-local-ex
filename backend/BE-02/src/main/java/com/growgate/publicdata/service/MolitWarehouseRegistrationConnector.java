package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.dto.PublicDataSignal;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

public class MolitWarehouseRegistrationConnector extends AbstractPublicDataConnector {

    private static final String TECHNICAL_DOCUMENT_NAME = "국토교통부_물류창고업등록정보기술문서";
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    @Value("${public-data.molit-warehouse-registration.url:https://apis.data.go.kr/1611000/whsinfoview2}")
    private String endpoint;

    @Value("${public-data.molit-warehouse-registration.key:${DATAGO_SERVICE_KEY:}}")
    private String apiKey;

    @Value("${public-data.molit-warehouse-registration.operation:WhsInfoDetail}")
    private String operation;

    @Value("${public-data.molit-warehouse-registration.num-of-rows:50}")
    private int numOfRows;

    @Value("${public-data.molit-warehouse-registration.page-no:1}")
    private int pageNo;

    @Value("${public-data.molit-warehouse-registration.region:}")
    private String region;

    @Value("${public-data.molit-warehouse-registration.item:}")
    private String configuredItem;

    @Value("${public-data.molit-warehouse-registration.start-date:}")
    private String startDate;

    @Value("${public-data.molit-warehouse-registration.end-date:}")
    private String endDate;

    public MolitWarehouseRegistrationConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.MOLIT_WAREHOUSE_REGISTRATION;
    }

    @Override
    public String usedFor() {
        return "support";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + safe(request.getTargetCountry()) + ":" + safe(itemFilter(request));
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        String operationEndpoint = endpointForOperation();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", "json");
        params.put("numOfRows", String.valueOf(Math.max(1, Math.min(500, numOfRows))));
        params.put("pageNo", String.valueOf(Math.max(1, pageNo)));
        putIfPresent(params, "sday", normalizedDate(startDate));
        putIfPresent(params, "eday", normalizedDate(endDate));
        putIfPresent(params, "Region", region);
        putIfPresent(params, "item", itemFilter(request));
        return getJson(request, operationEndpoint, apiKey, params);
    }

    @Override
    protected String endpointLabel() {
        return endpointForOperation();
    }

    @Override
    protected Map<String, String> fieldMapping() {
        return Map.of(
                "recordCount", "items.item[] / response.body.items.item[] warehouse records",
                "totalCount", "header.totalCount / body.totalCount / TotalCount",
                "generalAreaSquareMeters", "sum GENERAL_AREA",
                "frozenAreaSquareMeters", "sum FROZEN_AREA",
                "storageAreaSquareMeters", "sum STORAGE_AREA",
                "generalWingCount", "sum GENERAL_WING_COUNT",
                "frozenWingCount", "sum FROZEN_WING_COUNT",
                "itemKeywordMentions", "STORAGE_ITEM contains requested/configured item text",
                "sampleWarehouseNumbers", "WARE_NO sample values"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        JsonNode root = readTree(rawPayload);
        List<JsonNode> rows = warehouseRows(root);
        String item = itemFilter(request);
        int totalCount = firstInteger(root, "totalCount", "TotalCount", "totalcount").orElse(rows.size());
        double generalArea = sumNumericFields(root, "GENERAL_AREA");
        double frozenArea = sumNumericFields(root, "FROZEN_AREA");
        double storageArea = sumNumericFields(root, "STORAGE_AREA");
        int generalWings = (int) Math.round(sumNumericFields(root, "GENERAL_WING_COUNT"));
        int frozenWings = (int) Math.round(sumNumericFields(root, "FROZEN_WING_COUNT"));
        int itemMentions = countItemMentions(rows, item);
        List<String> warehouseNumbers = rows.stream()
                .map(row -> text(row, "WARE_NO"))
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(5)
                .toList();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("totalCount", totalCount);
        metrics.put("operation", safe(operation).isBlank() ? "WhsInfoDetail" : operation);
        metrics.put("regionFilter", safe(region));
        metrics.put("itemFilter", item);
        metrics.put("itemKeywordMentions", itemMentions);
        metrics.put("generalAreaSquareMeters", roundOne(generalArea));
        metrics.put("frozenAreaSquareMeters", roundOne(frozenArea));
        metrics.put("storageAreaSquareMeters", roundOne(storageArea));
        metrics.put("generalWingCount", generalWings);
        metrics.put("frozenWingCount", frozenWings);
        metrics.put("sampleWarehouseNumbers", warehouseNumbers);

        int contribution = fallbackUsed ? -1 : rows.isEmpty() ? 0 : 1 + Math.min(2, itemMentions);
        double confidence = fallbackUsed ? 0.0 : Math.min(0.70,
                0.32 + Math.min(0.18, rows.size() * 0.02)
                        + (itemMentions > 0 ? 0.12 : 0)
                        + (generalArea + frozenArea + storageArea > 0 ? 0.08 : 0));
        return signal(request, metrics, contribution, confidence);
    }

    @Override
    protected Map<String, Object> collectionEvidence(String endpoint, String apiKey, boolean fallbackUsed, String reason) {
        Map<String, Object> evidence = new LinkedHashMap<>(super.collectionEvidence(endpoint, apiKey, fallbackUsed, reason));
        evidence.put("technicalDocumentName", TECHNICAL_DOCUMENT_NAME);
        evidence.put("apiName", "whsinfoview");
        evidence.put("operation", safe(operation).isBlank() ? "WhsInfoDetail" : operation);
        evidence.put("updateCycle", "daily");
        evidence.put("dataProvider", "국토교통부 물류시설정보과");
        evidence.put("apiKeyStored", false);
        return evidence;
    }

    @Override
    protected Map<String, Object> dataUsagePolicy() {
        Map<String, Object> policy = new LinkedHashMap<>(super.dataUsagePolicy());
        policy.put("credentialHandling", "serviceKey is read only from MOLIT_WAREHOUSE_REGISTRATION_API_KEY or DATAGO_SERVICE_KEY and is masked in diagnostics.");
        return policy;
    }

    private String endpointForOperation() {
        String base = safe(endpoint);
        if (base.isBlank()) return "";
        if (base.endsWith("/WhsInfoList") || base.endsWith("/WhsInfoDetail")) return base;
        String op = safe(operation).isBlank() ? "WhsInfoDetail" : operation;
        return base.endsWith("/") ? base + op : base + "/" + op;
    }

    private void putIfPresent(Map<String, String> params, String key, String value) {
        if (value != null && !value.isBlank()) params.put(key, value);
    }

    private String itemFilter(PublicDataRequest request) {
        if (configuredItem != null && !configuredItem.isBlank()) return configuredItem.trim();
        String productName = safe(request.getProductName()).trim();
        if (productName.isBlank() || productName.length() > 100) return "";
        return productName;
    }

    private String normalizedDate(String value) {
        String date = safe(value).trim();
        if (date.isBlank()) return "";
        if (date.matches("\\d{8}")) return date;
        try {
            return LocalDate.parse(date).format(BASIC_DATE);
        } catch (Exception ignored) {
            return "";
        }
    }

    private List<JsonNode> warehouseRows(JsonNode root) {
        List<JsonNode> rows = new ArrayList<>();
        collectWarehouseRows(root, rows);
        return rows;
    }

    private void collectWarehouseRows(JsonNode node, List<JsonNode> rows) {
        if (node == null || node.isNull() || node.isMissingNode()) return;
        if (node.isObject() && (node.has("WARE_NO") || node.has("COMPANY_NAME") || node.has("STORAGE_ITEM"))) {
            rows.add(node);
            return;
        }
        if (node.isContainerNode()) {
            for (JsonNode child : node) collectWarehouseRows(child, rows);
        }
    }

    private int countItemMentions(List<JsonNode> rows, String item) {
        String needle = safe(item).toLowerCase(Locale.ROOT);
        if (needle.isBlank()) return 0;
        int count = 0;
        for (JsonNode row : rows) {
            if (text(row, "STORAGE_ITEM").toLowerCase(Locale.ROOT).contains(needle)) count++;
        }
        return count;
    }

    private OptionalInt firstInteger(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull() || node.isMissingNode()) return OptionalInt.empty();
        if (node.isObject()) {
            for (String fieldName : fieldNames) {
                JsonNode value = node.get(fieldName);
                if (value != null && !value.isNull()) {
                    if (value.isNumber()) return OptionalInt.of(value.asInt());
                    try {
                        return OptionalInt.of(Integer.parseInt(value.asText("").replace(",", "").trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            for (JsonNode child : node) {
                OptionalInt found = firstInteger(child, fieldNames);
                if (found.isPresent()) return found;
            }
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                OptionalInt found = firstInteger(child, fieldNames);
                if (found.isPresent()) return found;
            }
        }
        return OptionalInt.empty();
    }

    private String text(JsonNode row, String field) {
        JsonNode value = row == null ? null : row.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
