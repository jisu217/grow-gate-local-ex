package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ConsumerRecallChemicalConnector extends AbstractConsumerRecallConnector {

    @Value("${public-data.consumer-recall.chemical-key:${CONSUMER_RECALL_CHEMICAL_API_KEY:}}")
    private String apiKey;

    public ConsumerRecallChemicalConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.CONSUMER_RECALL_CHEMICAL;
    }

    @Override
    protected String apiKey() {
        return apiKey;
    }

    @Override
    protected String menuId() {
        return "0401";
    }

    @Override
    protected String menuName() {
        return "생활화학제품";
    }
}
