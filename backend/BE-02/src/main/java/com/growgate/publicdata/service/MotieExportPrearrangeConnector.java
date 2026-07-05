package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.dto.PublicDataSignal;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MotieExportPrearrangeConnector extends AbstractPublicDataConnector {

    private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

    @Value("${public-data.motie-export-prearrange.url:https://apis.data.go.kr/1450000/exportPrearngeInfoService}")
    private String endpoint;

    @Value("${public-data.motie-export-prearrange.key:${MOTIE_EXPORT_PREARRANGE_API_KEY:${DATAGO_SERVICE_KEY:}}}")
    private String apiKey;

    @Value("${public-data.motie-export-prearrange.key-param:ServiceKey}")
    private String keyParam;

    @Value("${public-data.motie-export-prearrange.operation:getExportPrearngeStat}")
    private String operation;

    @Value("${public-data.motie-export-prearrange.region-code:11000}")
    private String regionCode;

    @Value("${public-data.motie-export-prearrange.num-of-rows:10}")
    private int numOfRows;

    @Value("${public-data.motie-export-prearrange.page-no:1}")
    private int pageNo;

    @Value("${public-data.motie-export-prearrange.lookback-months:2}")
    private int lookbackMonths;

    public MotieExportPrearrangeConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.MOTIE_EXPORT_PREARRANGE;
    }

    @Override
    public String usedFor() {
        return "market";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + safe(request.getHsCode()) + ":" + safe(regionCode);
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        YearMonth endMonth = YearMonth.now().minusMonths(1);
        YearMonth startMonth = endMonth.minusMonths(Math.max(0, lookbackMonths - 1));
        return getJsonWithKeyParam(request, operationEndpoint(), apiKey, keyParam, Map.of(
                "splOrgCd", safe(regionCode),
                "stYearMonth", startMonth.format(YYYYMM),
                "endYearMonth", endMonth.format(YYYYMM),
                "numOfRows", String.valueOf(Math.max(1, numOfRows)),
                "pageNo", String.valueOf(Math.max(1, pageNo))
        ));
    }

    @Override
    protected String endpointLabel() {
        return operationEndpoint();
    }

    @Override
    protected Map<String, String> fieldMapping() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("prearrangeRecordCount", "XML response/body/items/item rows");
        fields.put("totalCount", "response/body/totalCount");
        fields.put("exportPrearrangedAmount", "sum of amt/amount fields");
        fields.put("exportPrearrangedCount", "sum of cnt/count fields");
        fields.put("currencyCount", "distinct cur/totalForeignAmount values");
        fields.put("regionCount", "distinct byrorgnm/purchaserSidoName values");
        fields.put("recordCount", "normalized export prearrange row count");
        return fields;
    }

    @Override
    protected Map<String, Object> collectionEvidence(String endpoint, String apiKey, boolean fallbackUsed, String reason) {
        Map<String, Object> evidence = new LinkedHashMap<>(super.collectionEvidence(endpoint, apiKey, fallbackUsed, reason));
        evidence.put("collectionMode", "live_operation_api");
        evidence.put("serviceName", "산업통상부_수출예정정보서비스");
        evidence.put("operation", operation);
        evidence.put("operationNameKo", "구매확인서발급 수출 예정 통계");
        evidence.put("documentName", "IROS4_OA_DV_0401_OpenAPI활용가이드_무역정보(수출+예정정보서비스)_v1.6.docx");
        evidence.put("portalModifiedDate", "2026-03-25");
        evidence.put("portalUpdateCycle", "실시간");
        evidence.put("portalDataFormat", "XML");
        evidence.put("keyParameter", keyParam);
        evidence.put("requiredParameters", "ServiceKey,splOrgCd,stYearMonth,endYearMonth,numOfRows,pageNo");
        return evidence;
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        int recordCount = fallbackUsed ? 0 : estimateRecordCount(rawPayload);
        int totalCount = fallbackUsed ? 0 : firstIntTag(rawPayload, "totalCount");
        double amount = fallbackUsed ? 0 : sumTags(rawPayload, "amt", "amount");
        int exportCount = fallbackUsed ? 0 : (int) Math.round(sumTags(rawPayload, "cnt", "count"));
        int currencyCount = fallbackUsed ? 0 : distinctTagCount(rawPayload, "cur", "totalForeignAmount");
        int regionCount = fallbackUsed ? 0 : distinctTagCount(rawPayload, "byrorgnm", "purchaserSidoName");

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("prearrangeRecordCount", recordCount);
        metrics.put("totalCount", totalCount);
        metrics.put("exportPrearrangedAmount", Math.round(amount));
        metrics.put("exportPrearrangedCount", exportCount);
        metrics.put("currencyCount", currencyCount);
        metrics.put("regionCount", regionCount);
        metrics.put("recordCount", recordCount);

        int contribution = fallbackUsed ? -2 : recordCount == 0 ? 0
                : Math.min(8, 2 + Math.min(3, recordCount / 2)
                + (amount > 0 ? 2 : 0)
                + (exportCount > 0 ? 1 : 0));
        double confidence = fallbackUsed ? 0.0 : Math.min(0.84,
                0.34 + Math.min(0.20, recordCount * 0.03)
                        + (totalCount > 0 ? 0.12 : 0)
                        + (amount > 0 ? 0.12 : 0)
                        + (exportCount > 0 ? 0.08 : 0));
        return signal(request, metrics, contribution, confidence);
    }

    private String operationEndpoint() {
        String base = safe(endpoint);
        String op = safe(operation);
        if (base.isBlank() || op.isBlank()) return base;
        return base.endsWith("/") ? base + op : base + "/" + op;
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
                    // Ignore malformed cells while retaining other rows from the official response.
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
