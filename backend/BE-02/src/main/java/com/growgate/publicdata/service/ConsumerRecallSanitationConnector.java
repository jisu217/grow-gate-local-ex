package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ConsumerRecallSanitationConnector extends AbstractConsumerRecallConnector {

    @Value("${public-data.consumer-recall.sanitation-key:${CONSUMER_RECALL_SANITATION_API_KEY:}}")
    private String apiKey;

    public ConsumerRecallSanitationConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.CONSUMER_RECALL_SANITATION;
    }

    @Override
    protected String apiKey() {
        return apiKey;
    }

    @Override
    protected String menuId() {
        return "0208";
    }

    @Override
    protected String menuName() {
        return "위생용품";
    }
}
