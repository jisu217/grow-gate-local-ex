package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ConsumerRecallOverseasConnector extends AbstractConsumerRecallConnector {

    @Value("${public-data.consumer-recall.overseas-key:${CONSUMER_RECALL_OVERSEAS_API_KEY:}}")
    private String apiKey;

    public ConsumerRecallOverseasConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.CONSUMER_RECALL_OVERSEAS;
    }

    @Override
    protected String apiKey() {
        return apiKey;
    }

    @Override
    protected String menuId() {
        return "0501";
    }

    @Override
    protected String menuName() {
        return "해외리콜";
    }
}
