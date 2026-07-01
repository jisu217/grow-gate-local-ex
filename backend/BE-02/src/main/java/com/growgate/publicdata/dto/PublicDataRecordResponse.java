package com.growgate.publicdata.dto;

import com.growgate.publicdata.entity.PublicDataRecord;
import com.growgate.common.DateTimeUtils;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicDataRecordResponse {

    private String recordId;
    private String runId;
    private String provider;
    private String agency;
    private String ministry;
    private String dataFamily;
    private boolean motieRelated;
    private String endpoint;
    private Boolean endpointConfigured;
    private Boolean operationEndpoint;
    private String endpointType;
    private Boolean apiKeyConfigured;
    private String requestKey;
    private String hsCode;
    private String targetCountry;
    private String matchedKey;
    private String usedFor;
    private Integer scoreContribution;
    private Double confidence;
    private String sourceType;
    private boolean fallbackUsed;
    private boolean cacheHit;
    private String errorMessage;
    private String fetchedAt;
    private String expiresAt;
    private String normalizedPayload;
    private String metricSchema;
    private String scoreFormula;

    public static PublicDataRecordResponse from(PublicDataRecord record) {
        RecordEvidence evidence = RecordEvidence.from(record.getNormalizedPayload());
        return PublicDataRecordResponse.builder()
                .recordId(record.getId())
                .runId(record.getRun() != null ? record.getRun().getId() : null)
                .provider(record.getProvider().name())
                .agency(record.getProvider().agency())
                .ministry(record.getProvider().ministry())
                .dataFamily(record.getProvider().dataFamily())
                .motieRelated(record.getProvider().motieRelated())
                .endpoint(record.getEndpoint())
                .endpointConfigured(evidence.endpointConfigured())
                .operationEndpoint(evidence.operationEndpoint())
                .endpointType(evidence.endpointType())
                .apiKeyConfigured(evidence.apiKeyConfigured())
                .requestKey(record.getRequestKey())
                .hsCode(record.getHsCode())
                .targetCountry(record.getTargetCountry())
                .matchedKey(record.getMatchedKey())
                .usedFor(record.getUsedFor())
                .scoreContribution(record.getScoreContribution())
                .confidence(record.getConfidence())
                .sourceType(record.getSourceType())
                .fallbackUsed(record.isFallbackUsed())
                .cacheHit(record.isCacheHit())
                .errorMessage(record.getErrorMessage())
                .fetchedAt(DateTimeUtils.formatUtc(record.getFetchedAt()))
                .expiresAt(DateTimeUtils.formatUtc(record.getExpiresAt()))
                .normalizedPayload(record.getNormalizedPayload())
                .metricSchema(evidence.metricSchema())
                .scoreFormula(evidence.scoreFormula())
                .build();
    }

    private record RecordEvidence(Boolean endpointConfigured,
                                  Boolean operationEndpoint,
                                  String endpointType,
                                  Boolean apiKeyConfigured,
                                  String metricSchema,
                                  String scoreFormula) {
        private static RecordEvidence from(String normalizedPayload) {
            if (normalizedPayload == null || normalizedPayload.isBlank()) {
                return new RecordEvidence(null, null, null, null, null, null);
            }
            try {
                com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(normalizedPayload);
                com.fasterxml.jackson.databind.JsonNode evidence = root.path("collectionEvidence");
                return new RecordEvidence(
                        evidence.has("endpointConfigured") ? evidence.path("endpointConfigured").asBoolean() : null,
                        evidence.has("operationEndpoint") ? evidence.path("operationEndpoint").asBoolean() : null,
                        evidence.path("endpointType").asText(null),
                        evidence.has("apiKeyConfigured") ? evidence.path("apiKeyConfigured").asBoolean() : null,
                        root.path("metricSchema").isMissingNode() ? null : root.path("metricSchema").toString(),
                        root.path("scoreFormula").asText(null)
                );
            } catch (Exception e) {
                return new RecordEvidence(null, null, null, null, null, null);
            }
        }
    }
}
