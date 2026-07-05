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
    private String sourceName;
    private String officialUrl;
    private Boolean providerOperationEndpoint;
    private Boolean providerApiKeyRequired;
    private String licenseName;
    private String termsUrl;
    private String redistributionAllowed;
    private String storageAllowed;
    private String reprocessingAllowed;
    private String attributionRequired;
    private String usageWarning;
    private String licenseLastVerifiedAt;
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
    private Integer rawCount;
    private Integer normalizedCount;
    private Integer filteredCount;
    private String zeroReason;
    private String requestParamsMasked;
    private String sampleRawKeys;

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
                .sourceName(record.getProvider().sourceName())
                .officialUrl(record.getProvider().officialUrl())
                .providerOperationEndpoint(record.getProvider().operationEndpoint())
                .providerApiKeyRequired(record.getProvider().apiKeyRequired())
                .licenseName(record.getProvider().licenseName())
                .termsUrl(record.getProvider().termsUrl())
                .redistributionAllowed(record.getProvider().redistributionAllowed())
                .storageAllowed(record.getProvider().storageAllowed())
                .reprocessingAllowed(record.getProvider().reprocessingAllowed())
                .attributionRequired(record.getProvider().attributionRequired())
                .usageWarning(record.getProvider().usageWarning())
                .licenseLastVerifiedAt(record.getProvider().lastVerifiedAt())
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
                .rawCount(evidence.rawCount())
                .normalizedCount(evidence.normalizedCount())
                .filteredCount(evidence.filteredCount())
                .zeroReason(evidence.zeroReason())
                .requestParamsMasked(evidence.requestParamsMasked())
                .sampleRawKeys(evidence.sampleRawKeys())
                .build();
    }

    private record RecordEvidence(Boolean endpointConfigured,
                                  Boolean operationEndpoint,
                                  String endpointType,
                                  Boolean apiKeyConfigured,
                                  String metricSchema,
                                  String scoreFormula,
                                  Integer rawCount,
                                  Integer normalizedCount,
                                  Integer filteredCount,
                                  String zeroReason,
                                  String requestParamsMasked,
                                  String sampleRawKeys) {
        private static RecordEvidence from(String normalizedPayload) {
            if (normalizedPayload == null || normalizedPayload.isBlank()) {
                return new RecordEvidence(null, null, null, null, null, null, null, null, null, null, null, null);
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
                        root.path("scoreFormula").asText(null),
                        root.has("rawCount") ? root.path("rawCount").asInt() : null,
                        root.has("normalizedCount") ? root.path("normalizedCount").asInt() : null,
                        root.has("filteredCount") ? root.path("filteredCount").asInt() : null,
                        root.path("zeroReason").asText(null),
                        root.path("requestParamsMasked").isMissingNode() ? null : root.path("requestParamsMasked").toString(),
                        root.path("sampleRawKeys").isMissingNode() ? null : root.path("sampleRawKeys").toString()
                );
            } catch (Exception e) {
                return new RecordEvidence(null, null, null, null, null, null, null, null, null, null, null, null);
            }
        }
    }
}
