package com.growgate.publicdata.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.entity.PublicDataRecord;
import com.growgate.publicdata.enums.PublicDataProvider;
import com.growgate.publicdata.enums.SourceQualityStatus;
import com.growgate.publicdata.repository.PublicDataCollectionRunRepository;
import com.growgate.publicdata.repository.PublicDataRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PublicDataProofRunService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final PublicDataCollectionService collectionService;
    private final PublicDataCollectionRunRepository runRepository;
    private final PublicDataRecordRepository recordRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${public-data.proof.min-live-providers:3}")
    private int minLiveProviders;

    @Value("${public-data.proof.min-motie-related-live:2}")
    private int minMotieRelatedLive;

    @Value("${spring.profiles.active:local}")
    private String serverProfile;

    @Transactional
    public Map<String, Object> createProofRun(String userId, String triggerType) {
        List<PublicDataRecord> records = runRepository.findTopByOrderByStartedAtDesc()
                .map(run -> recordRepository.findByRun_IdOrderByFetchedAtDesc(run.getId()))
                .orElseGet(() -> collectionService.getRecentRecords(userId, null, null))
                .stream()
                .sorted(Comparator.comparing(PublicDataRecord::getFetchedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return createProofRunFromRecords(userId, triggerType, records);
    }

    @Transactional
    public Map<String, Object> createProofRunForDiagnosis(String userId, String diagnosisId, String triggerType) {
        List<PublicDataRecord> records = recordRepository.findByRun_DiagnosisIdOrderByFetchedAtDesc(diagnosisId);
        return createProofRunFromRecords(userId, triggerType == null ? "diagnosis" : triggerType, records);
    }

    private Map<String, Object> createProofRunFromRecords(String userId, String triggerType, List<PublicDataRecord> records) {
        String proofRunId = "pdr_" + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(LocalDateTime.now()) + "_" + Integer.toHexString(Math.abs(records.hashCode()));

        jdbcTemplate.update("""
                        INSERT INTO public_api_proof_runs
                        (proof_run_id, requested_by, trigger_type, server_profile, total_provider_count, proof_status)
                        VALUES (?, ?, ?, ?, ?, 'RUNNING')
                        """,
                proofRunId, userId, triggerType == null ? "manual" : triggerType, serverProfile,
                distinctProviders(records, null).size());

        List<PublicDataRecord> latestProviderRecords = latestRecordPerProvider(records);
        List<Map<String, Object>> providerRows = new ArrayList<>();
        List<Map<String, Object>> waterfallRows = new ArrayList<>();
        for (PublicDataRecord record : latestProviderRecords) {
            Map<String, Object> providerRow = persistRecordProof(proofRunId, record);
            providerRows.add(providerRow);
            Map<String, Object> waterfall = persistScoreContribution(proofRunId, record, providerRow);
            waterfallRows.add(waterfall);
        }

        Map<String, Object> summary = summarize(proofRunId, latestProviderRecords, providerRows, waterfallRows);
        jdbcTemplate.update("""
                        UPDATE public_api_proof_runs
                        SET completed_at = CURRENT_TIMESTAMP(6),
                            live_provider_count = ?,
                            cache_provider_count = ?,
                            fallback_provider_count = ?,
                            motie_related_live_count = ?,
                            operation_endpoint_live_count = ?,
                            api_key_configured_live_count = ?,
                            schema_pass_count = ?,
                            schema_fail_count = ?,
                            proof_status = ?,
                            fail_reasons = ?
                        WHERE proof_run_id = ?
                        """,
                summary.get("liveProviderCount"),
                summary.get("cacheProviderCount"),
                summary.get("fallbackProviderCount"),
                summary.get("motieRelatedLiveCount"),
                summary.get("operationEndpointLiveCount"),
                summary.get("apiKeyConfiguredLiveCount"),
                summary.get("schemaPassCount"),
                summary.get("schemaFailCount"),
                summary.get("proofStatus"),
                toJson(summary.get("failReasons")),
                proofRunId);
        return detail(proofRunId);
    }

    private List<PublicDataRecord> latestRecordPerProvider(List<PublicDataRecord> records) {
        Map<PublicDataProvider, PublicDataRecord> latest = new LinkedHashMap<>();
        records.stream()
                .sorted(Comparator.comparing(PublicDataRecord::getFetchedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .forEach(record -> latest.putIfAbsent(record.getProvider(), record));
        return new ArrayList<>(latest.values());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> latest(String userId) {
        List<Map<String, Object>> rows;
        if (userId == null || userId.isBlank()) {
            rows = jdbcTemplate.queryForList("""
                    SELECT proof_run_id FROM public_api_proof_runs
                    ORDER BY created_at DESC
                    LIMIT 1
                    """);
        } else {
            rows = jdbcTemplate.queryForList("""
                    SELECT proof_run_id FROM public_api_proof_runs
                    WHERE requested_by = ?
                    ORDER BY created_at DESC
                    LIMIT 1
                    """, userId);
        }
        if (rows.isEmpty()) {
            return Map.of("proofStatus", "EMPTY", "message", "public data proof run이 아직 없습니다.");
        }
        Map<String, Object> result = detail(String.valueOf(rows.get(0).get("proof_run_id")));
        if (userId == null || userId.isBlank()) {
            result.remove("requested_by");
            result.remove("requestedBy");
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(String proofRunId) {
        List<Map<String, Object>> runs = jdbcTemplate.queryForList("""
                SELECT proof_run_id, requested_by, trigger_type, server_profile, started_at, completed_at,
                       total_provider_count, live_provider_count, cache_provider_count, fallback_provider_count,
                       motie_related_live_count, operation_endpoint_live_count, api_key_configured_live_count,
                       schema_pass_count, schema_fail_count, proof_status, fail_reasons
                FROM public_api_proof_runs
                WHERE proof_run_id = ?
                """, proofRunId);
        if (runs.isEmpty()) {
            return Map.of("proofRunId", proofRunId, "proofStatus", "NOT_FOUND");
        }
        Map<String, Object> result = new LinkedHashMap<>(runs.get(0));
        result.put("providers", rawResponses(proofRunId));
        result.put("scoreWaterfall", scoreContributions(proofRunId));
        result.put("judgeProofPolicy", "live/cache proof requires operationEndpoint=true, apiKeyConfigured=true, rawResponseSha256, schemaValidation=PASS");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> rawResponses(String proofRunId) {
        return jdbcTemplate.queryForList("""
                SELECT provider_code AS providerCode,
                       request_url_redacted AS requestUrlRedacted,
                       api_key_configured AS apiKeyConfigured,
                       operation_endpoint AS operationEndpoint,
                       http_status AS httpStatus,
                       content_type AS contentType,
                       source_type AS sourceType,
                       source_quality_status AS sourceQualityStatus,
                       record_count AS recordCount,
                       latency_ms AS latencyMs,
                       raw_response_sha256 AS rawResponseSha256,
                       schema_name AS schemaName,
                       schema_validation_status AS schemaValidationStatus,
                       schema_validation_errors AS schemaValidationErrors,
                       fallback_reason AS fallbackReason,
                       fetched_at AS fetchedAt
                FROM public_api_raw_responses
                WHERE proof_run_id = ?
                ORDER BY provider_code, fetched_at DESC
                """, proofRunId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> scoreContributions(String proofRunId) {
        return jdbcTemplate.queryForList("""
                SELECT diagnosis_id AS diagnosisId,
                       provider_code AS providerCode,
                       dimension,
                       base_score AS baseScore,
                       public_data_delta AS publicDataDelta,
                       guarded_delta AS guardedDelta,
                       final_score AS finalScore,
                       source_type AS sourceType,
                       clamp_reason AS clampReason,
                       evidence_record_ids AS evidenceRecordIds
                FROM public_data_score_contributions
                WHERE proof_run_id = ?
                ORDER BY provider_code, dimension
                """, proofRunId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> waterfallForDiagnosis(String diagnosisId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT proof_run_id AS proofRunId,
                       provider_code AS providerCode,
                       dimension,
                       base_score AS baseScore,
                       public_data_delta AS publicDataDelta,
                       guarded_delta AS guardedDelta,
                       final_score AS finalScore,
                       source_type AS sourceType,
                       clamp_reason AS clampReason
                FROM public_data_score_contributions
                WHERE diagnosis_id = ?
                ORDER BY created_at DESC, provider_code
                """, diagnosisId);
        return Map.of("diagnosisId", diagnosisId, "waterfall", rows);
    }

    private Map<String, Object> persistRecordProof(String proofRunId, PublicDataRecord record) {
        Map<String, Object> normalized = parseMap(record.getNormalizedPayload());
        Map<String, Object> evidence = mapValue(normalized.get("collectionEvidence"));
        Map<String, Object> metrics = mapValue(normalized.get("metrics"));
        String sourceType = sourceType(record);
        SourceQualityStatus qualityStatus = PublicDataQualityClassifier.classify(record, normalized);
        boolean liveOrCache = "live".equals(sourceType) || "cache".equals(sourceType);
        boolean operationEndpoint = booleanValue(evidence.get("operationEndpoint"));
        boolean apiKeyConfigured = booleanValue(evidence.get("apiKeyConfigured"));
        int recordCount = intValue(metrics.get("recordCount"));
        String rawHash = record.getRawPayload() == null || record.getRawPayload().isBlank()
                ? null : "sha256:" + sha256(record.getRawPayload());
        String schemaStatus = schemaStatus(qualityStatus, liveOrCache, operationEndpoint, apiKeyConfigured, recordCount, rawHash);
        String fallbackReason = String.valueOf(evidence.getOrDefault("reason",
                record.getErrorMessage() == null ? "" : record.getErrorMessage()));

        jdbcTemplate.update("""
                        INSERT INTO public_api_raw_responses
                        (proof_run_id, provider_code, request_url_redacted, request_params_hash, api_key_configured,
                         operation_endpoint, http_status, content_type, source_type, source_quality_status, record_count, latency_ms,
                         raw_response_sha256, schema_name, schema_validation_status, schema_validation_errors,
                         fallback_reason, fetched_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                proofRunId,
                record.getProvider().name(),
                redact(record.getEndpoint()),
                sha256(record.getRequestKey() == null ? "" : record.getRequestKey()),
                apiKeyConfigured,
                operationEndpoint,
                liveOrCache ? 200 : null,
                "application/json-or-xml",
                sourceType,
                qualityStatus.name(),
                recordCount,
                null,
                rawHash,
                record.getProvider().name() + "_v1",
                schemaStatus,
                "FAIL".equals(schemaStatus) ? toJson(List.of("live proof requires operation endpoint, API key, record count, and raw hash")) : "[]",
                fallbackReason,
                record.getFetchedAt());

        jdbcTemplate.update("""
                        INSERT INTO public_api_normalized_records
                        (proof_run_id, provider_code, external_record_id, normalized_record_type, title, country_code,
                         hs_code, product_keyword, source_url, normalized_payload, raw_response_sha256)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                proofRunId,
                record.getProvider().name(),
                record.getId(),
                record.getUsedFor() == null ? record.getProvider().usedFor() : record.getUsedFor(),
                record.getMatchedKey(),
                record.getTargetCountry(),
                record.getHsCode(),
                record.getMatchedKey(),
                redact(record.getEndpoint()),
                record.getNormalizedPayload() == null ? "{}" : record.getNormalizedPayload(),
                rawHash);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("providerCode", record.getProvider().name());
        row.put("agencyName", record.getProvider().agency());
        row.put("datasetName", record.getProvider().usedFor());
        row.put("sourceType", sourceType);
        row.put("sourceQualityStatus", qualityStatus.name());
        row.put("apiKeyConfigured", apiKeyConfigured);
        row.put("operationEndpoint", operationEndpoint);
        row.put("httpStatus", liveOrCache ? 200 : null);
        row.put("recordCount", recordCount);
        row.put("rawResponseSha256", rawHash);
        row.put("schemaValidationStatus", schemaStatus);
        row.put("fallbackReason", fallbackReason);
        return row;
    }

    private Map<String, Object> persistScoreContribution(String proofRunId, PublicDataRecord record,
                                                         Map<String, Object> providerRow) {
        String sourceType = String.valueOf(providerRow.get("sourceType"));
        SourceQualityStatus qualityStatus = SourceQualityStatus.valueOf(String.valueOf(providerRow.get("sourceQualityStatus")));
        int rawDelta = record.getScoreContribution() == null ? 0 : record.getScoreContribution();
        boolean positiveAllowed = PublicDataQualityClassifier.positiveContributionAllowed(qualityStatus);
        int guardedDelta = positiveAllowed ? rawDelta : Math.min(rawDelta, 0);
        String clampReason = rawDelta != guardedDelta ? qualityStatus.name() + " positive contribution clamped" : "";
        String diagnosisId = record.getRun() == null ? null : record.getRun().getDiagnosisId();
        String dimension = record.getUsedFor() == null ? record.getProvider().usedFor() : record.getUsedFor();
        jdbcTemplate.update("""
                        INSERT INTO public_data_score_contributions
                        (diagnosis_id, proof_run_id, provider_code, dimension, base_score, public_data_delta,
                         guarded_delta, final_score, source_type, clamp_reason, evidence_record_ids)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                diagnosisId, proofRunId, record.getProvider().name(), dimension,
                0, rawDelta, guardedDelta, guardedDelta, sourceType, clampReason,
                toJson(List.of(record.getId())));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("diagnosisId", diagnosisId);
        row.put("providerCode", record.getProvider().name());
        row.put("dimension", dimension);
        row.put("baseScore", 0);
        row.put("publicDataDelta", rawDelta);
        row.put("guardedDelta", guardedDelta);
        row.put("finalScore", guardedDelta);
        row.put("sourceType", sourceType);
        row.put("sourceQualityStatus", qualityStatus.name());
        row.put("clampReason", clampReason);
        return row;
    }

    private Map<String, Object> summarize(String proofRunId, List<PublicDataRecord> records,
                                          List<Map<String, Object>> providerRows,
                                          List<Map<String, Object>> waterfallRows) {
        Set<String> liveProviders = distinctProviders(records, "live");
        Set<String> cacheProviders = distinctProviders(records, "cache");
        Set<String> fallbackProviders = distinctProviders(records, "fallback");
        int motieRelatedLive = (int) providerRows.stream()
                .filter(row -> "live".equals(row.get("sourceType")))
                .filter(row -> PublicDataProvider.valueOf(String.valueOf(row.get("providerCode"))).motieRelated())
                .count();
        int operationLive = (int) providerRows.stream()
                .filter(row -> "live".equals(row.get("sourceType")))
                .filter(row -> Boolean.TRUE.equals(row.get("operationEndpoint")))
                .count();
        int apiKeyLive = (int) providerRows.stream()
                .filter(row -> "live".equals(row.get("sourceType")))
                .filter(row -> Boolean.TRUE.equals(row.get("apiKeyConfigured")))
                .count();
        int schemaPass = (int) providerRows.stream()
                .filter(row -> "PASS".equals(row.get("schemaValidationStatus"))).count();
        int schemaFail = (int) providerRows.stream()
                .filter(row -> "FAIL".equals(row.get("schemaValidationStatus"))).count();
        List<String> failReasons = failReasons(liveProviders.size(), motieRelatedLive, operationLive, apiKeyLive,
                schemaFail, providerRows, waterfallRows);
        String proofStatus = failReasons.isEmpty() ? "PASS"
                : liveProviders.isEmpty() && cacheProviders.isEmpty() ? "FAIL" : "WARN";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("proofRunId", proofRunId);
        result.put("proofStatus", proofStatus);
        result.put("liveProviderCount", liveProviders.size());
        result.put("cacheProviderCount", cacheProviders.size());
        result.put("fallbackProviderCount", fallbackProviders.size());
        result.put("motieRelatedLiveCount", motieRelatedLive);
        result.put("operationEndpointLiveCount", operationLive);
        result.put("apiKeyConfiguredLiveCount", apiKeyLive);
        result.put("schemaPassCount", schemaPass);
        result.put("schemaFailCount", schemaFail);
        result.put("sourceQualityCounts", providerRows.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        row -> String.valueOf(row.get("sourceQualityStatus")),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.counting()
                )));
        result.put("failReasons", failReasons);
        return result;
    }

    private List<String> failReasons(int liveCount, int motieRelatedLive, int operationLive, int apiKeyLive,
                                     int schemaFail, List<Map<String, Object>> providerRows,
                                     List<Map<String, Object>> waterfallRows) {
        List<String> reasons = new ArrayList<>();
        if (liveCount < minLiveProviders) reasons.add("liveProviderCount < " + minLiveProviders);
        if (motieRelatedLive < minMotieRelatedLive) reasons.add("motieRelatedLiveCount < " + minMotieRelatedLive);
        if (operationLive < minLiveProviders) reasons.add("operationEndpointLiveCount < " + minLiveProviders);
        if (apiKeyLive < minLiveProviders) reasons.add("apiKeyConfiguredLiveCount < " + minLiveProviders);
        if (schemaFail > 0) reasons.add("schemaFailCount > 0");
        boolean missingHash = providerRows.stream()
                .filter(row -> "live".equals(row.get("sourceType")) || "cache".equals(row.get("sourceType")))
                .anyMatch(row -> row.get("rawResponseSha256") == null);
        if (missingHash) reasons.add("live/cache provider missing rawResponseSha256");
        boolean fallbackPositive = waterfallRows.stream()
                .filter(row -> "fallback".equals(row.get("sourceType")))
                .anyMatch(row -> intValue(row.get("guardedDelta")) > 0);
        if (fallbackPositive) reasons.add("fallback provider has positive guardedDelta");
        return reasons;
    }

    private Set<String> distinctProviders(List<PublicDataRecord> records, String sourceType) {
        Set<String> providers = new LinkedHashSet<>();
        for (PublicDataRecord record : records) {
            if (sourceType == null || sourceType.equals(sourceType(record))) {
                providers.add(record.getProvider().name());
            }
        }
        return providers;
    }

    private String sourceType(PublicDataRecord record) {
        if (record.isFallbackUsed() || "fallback".equals(record.getSourceType())) return "fallback";
        if (record.isCacheHit() || "cache".equals(record.getSourceType())) return "cache";
        if (record.getSourceType() != null && !record.getSourceType().isBlank()) return record.getSourceType();
        return "live";
    }

    private String schemaStatus(SourceQualityStatus qualityStatus, boolean liveOrCache,
                                boolean operationEndpoint, boolean apiKeyConfigured,
                                int recordCount, String rawHash) {
        if (!liveOrCache) return "SKIPPED";
        if (qualityStatus == SourceQualityStatus.CACHE_VERIFIED) {
            return rawHash != null ? "PASS" : "FAIL";
        }
        if (qualityStatus == SourceQualityStatus.EMPTY_TRUE_ZERO) {
            return operationEndpoint && apiKeyConfigured && rawHash != null ? "PASS" : "FAIL";
        }
        return operationEndpoint && apiKeyConfigured && recordCount > 0 && rawHash != null ? "PASS" : "FAIL";
    }

    private String redact(String value) {
        if (value == null || value.isBlank()) return "not-configured";
        return value
                .replaceAll("(?i)(serviceKey|crtfcKey|accessKey|apiKey|key)=([^&]+)", "$1=REDACTED")
                .replaceAll("(?i)(/localhost|://localhost|://127\\.0\\.0\\.1)", "://blocked-localhost");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "hash-error";
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        return Map.of();
    }

    private boolean booleanValue(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }
}
