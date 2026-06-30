package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
}
