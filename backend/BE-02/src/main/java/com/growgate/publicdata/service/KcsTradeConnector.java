package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.growgate.publicdata.dto.PublicDataSignal;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class KcsTradeConnector extends AbstractPublicDataConnector {

    private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

    @Value("${public-data.kcs-trade.url:}")
    private String endpoint;

    @Value("${public-data.kcs-trade.key:}")
    private String apiKey;

    public KcsTradeConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KCS_TRADE;
    }

    @Override
    public String usedFor() {
        return "market";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + safe(request.getHsCode()) + ":" + safe(request.getTargetCountry());
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        YearMonth endMonth = YearMonth.now().minusMonths(1);
        YearMonth startMonth = endMonth.minusMonths(11);
        return getJson(request, endpoint, apiKey, Map.of(
                "hsCode", safe(request.getHsCode()),
                "cntyCd", safe(request.getTargetCountry()),
                "strtYymm", startMonth.format(YYYYMM),
                "endYymm", endMonth.format(YYYYMM)
        ));
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        JsonNode root = readTree(rawPayload);
        double importAmountUsd = sumNumericFields(root,
                "importAmountUsd", "import_amount_usd", "impDlr", "importDlr", "importAmount", "수입금액");
        double exportAmountUsd = sumNumericFields(root,
                "exportAmountUsd", "export_amount_usd", "expDlr", "exportDlr", "exportAmount", "수출금액");
        double yoyGrowthPct = firstNumericField(root,
                "yoyGrowthPct", "yoy_growth_pct", "growthPct", "growth", "증가율").orElse(0);
        int recordCount = Math.max(maxArraySize(root), Math.max(0, objectCount(root) - 1));

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("importAmountUsd", Math.round(importAmountUsd));
        metrics.put("exportAmountUsd", Math.round(exportAmountUsd));
        metrics.put("yoyGrowthPct", Math.round(yoyGrowthPct * 10.0) / 10.0);
        metrics.put("recordCount", recordCount);

        int contribution = fallbackUsed ? -2 : 3
                + Math.min(4, recordCount / 2)
                + (importAmountUsd + exportAmountUsd > 0 ? 2 : 0)
                + (yoyGrowthPct >= 10 ? 2 : yoyGrowthPct > 0 ? 1 : yoyGrowthPct < -10 ? -2 : 0);
        double confidence = fallbackUsed ? 0.0 : Math.min(0.95,
                0.35 + Math.min(0.25, recordCount * 0.03)
                        + (importAmountUsd + exportAmountUsd > 0 ? 0.25 : 0)
                        + (yoyGrowthPct != 0 ? 0.10 : 0));
        return signal(request, metrics, contribution, confidence);
    }
}
