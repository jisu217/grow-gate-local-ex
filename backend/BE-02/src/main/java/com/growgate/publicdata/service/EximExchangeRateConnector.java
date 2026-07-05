package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.dto.PublicDataSignal;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class EximExchangeRateConnector extends AbstractPublicDataConnector {

    @Value("${public-data.exim-exchange-rate.url:https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON}")
    private String endpoint;

    @Value("${public-data.exim-exchange-rate.key:${EXIM_EXCHANGE_RATE_API_KEY:}}")
    private String apiKey;

    @Value("${public-data.exim-exchange-rate.data-code:AP01}")
    private String dataCode;

    @Value("${public-data.exim-exchange-rate.search-date:}")
    private String searchDate;

    public EximExchangeRateConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.EXIM_EXCHANGE_RATE;
    }

    @Override
    public String usedFor() {
        return "market";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + targetCurrency(request.getTargetCountry()) + ":" + effectiveSearchDate();
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        return getJsonWithKeyParam(request, endpoint, apiKey, "authkey", Map.of(
                "searchdate", effectiveSearchDate(),
                "data", dataCode
        ));
    }

    @Override
    protected Map<String, String> fieldMapping() {
        return Map.of(
                "recordCount", "exchangeJSON currency rows",
                "targetCurrency", "currency code inferred from targetCountry",
                "targetExchangeRate", "deal_bas_r for targetCurrency when present",
                "usdExchangeRate", "deal_bas_r for USD when present",
                "eurExchangeRate", "deal_bas_r for EUR when present"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        JsonNode root = readTree(rawPayload);
        Map<String, Object> metrics = new LinkedHashMap<>();
        String targetCurrency = targetCurrency(request.getTargetCountry());
        double targetRate = rateFor(root, targetCurrency);
        metrics.put("recordCount", fallbackUsed ? 0 : Math.max(root.isArray() ? root.size() : 0, maxArraySize(root)));
        metrics.put("targetCurrency", targetCurrency);
        metrics.put("targetExchangeRate", targetRate);
        metrics.put("usdExchangeRate", rateFor(root, "USD"));
        metrics.put("eurExchangeRate", rateFor(root, "EUR"));
        metrics.put("exchangeRateDate", effectiveSearchDate());
        int contribution = fallbackUsed ? -1 : targetRate > 0 ? 1 : 0;
        double confidence = fallbackUsed ? 0.0 : targetRate > 0 ? 0.70 : 0.42;
        return signal(request, metrics, contribution, confidence);
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    private String effectiveSearchDate() {
        if (searchDate != null && !searchDate.isBlank()) return searchDate;
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private double rateFor(JsonNode root, String currency) {
        if (root == null || !root.isArray()) return 0.0;
        for (JsonNode row : root) {
            String unit = row.path("cur_unit").asText("").toUpperCase(Locale.ROOT);
            if (unit.equals(currency) || unit.startsWith(currency + "(")) {
                return parseRate(row.path("deal_bas_r").asText(""));
            }
        }
        return 0.0;
    }

    private double parseRate(String value) {
        try {
            return Double.parseDouble(value.replace(",", "").trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String targetCurrency(String country) {
        String value = safe(country).toUpperCase(Locale.ROOT);
        if (value.contains("EU") || value.contains("DE") || value.contains("FR")
                || value.contains("IT") || value.contains("ES") || value.contains("NL")) return "EUR";
        if (value.contains("JP") || value.contains("일본")) return "JPY";
        if (value.contains("CN") || value.contains("중국")) return "CNH";
        return "USD";
    }
}
