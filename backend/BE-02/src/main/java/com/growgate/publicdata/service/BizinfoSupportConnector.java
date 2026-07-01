package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.growgate.publicdata.dto.PublicDataSignal;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class BizinfoSupportConnector extends AbstractPublicDataConnector {

    @Value("${public-data.bizinfo-support.url:}")
    private String endpoint;

    @Value("${public-data.bizinfo-support.key:}")
    private String apiKey;

    public BizinfoSupportConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.BIZINFO_SUPPORT;
    }

    @Override
    public String usedFor() {
        return "support";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + safe(request.getTargetCountry()) + ":" + safe(request.getProductName());
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        return getJson(request, endpoint, apiKey, Map.of(
                "keyword", safe(request.getProductName()),
                "country", safe(request.getTargetCountry())
        ));
    }

    @Override
    protected int scoreContribution(PublicDataRequest request, boolean fallbackUsed) {
        return fallbackUsed ? -1 : 6;
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        JsonNode root = readTree(rawPayload);
        int programCount = Math.max(maxArraySize(root), Math.max(0, objectCount(root) - 1));
        int openCount = textHitCount(root, "접수|모집|공고|open|apply|신청");
        int exportMentions = textHitCount(root, "수출|export|해외|인증|바우처");

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("supportProgramCount", programCount);
        metrics.put("openProgramSignalCount", openCount);
        metrics.put("exportKeywordCount", exportMentions);
        metrics.put("recordCount", programCount);

        int contribution = fallbackUsed ? -1 : 1
                + Math.min(5, programCount)
                + Math.min(2, openCount)
                + Math.min(2, exportMentions);
        double confidence = fallbackUsed ? 0.0 : Math.min(0.9,
                0.30 + Math.min(0.30, programCount * 0.05)
                        + Math.min(0.20, (openCount + exportMentions) * 0.04));
        return signal(request, metrics, contribution, confidence);
    }
}
