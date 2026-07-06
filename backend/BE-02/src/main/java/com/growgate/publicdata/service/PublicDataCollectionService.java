package com.growgate.publicdata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.common.DateTimeUtils;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.publicdata.dto.PublicDataDashboardResponse;
import com.growgate.publicdata.dto.PublicDataDashboardResponse.ProviderStatus;
import com.growgate.membership.entity.Membership;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.publicdata.entity.PublicDataCollectionRun;
import com.growgate.publicdata.entity.PublicDataRecord;
import com.growgate.publicdata.enums.PublicDataCollectionStatus;
import com.growgate.publicdata.enums.PublicDataProvider;
import com.growgate.publicdata.enums.SourceQualityStatus;
import com.growgate.publicdata.repository.PublicDataCollectionRunRepository;
import com.growgate.publicdata.repository.PublicDataRecordRepository;
import com.growgate.user.entity.User;
import com.growgate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PublicDataCollectionService {

    private static final Set<PublicDataProvider> INACTIVE_PROVIDERS = EnumSet.of(
            PublicDataProvider.BIZINFO_SUPPORT,
            PublicDataProvider.MOLIT_WAREHOUSE_REGISTRATION
    );

    private final List<PublicDataConnector> connectors;
    private final PublicDataCollectionRunRepository runRepository;
    private final PublicDataRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ObjectMapper objectMapper;

    @Value("${public-data.enabled:false}")
    private boolean enabled;

    @Value("${public-data.cache-ttl-hours:24}")
    private long cacheTtlHours;

    public PublicDataCollectionResult collectForDiagnosis(String diagnosisId, String userId, String hsCode,
                                                          String targetCountry, String productName) {
        PublicDataRequest request = new PublicDataRequest(diagnosisId, userId, normalizeHsCode(hsCode),
                normalizeCountry(targetCountry), productName);
        return collect(request);
    }

    @Transactional(readOnly = true)
    public List<PublicDataCollectionRun> getRecentRuns(String userId) {
        return runRepository.findTop20ByRequestedBy_IdInOrderByStartedAtDesc(scopeUserIds(userId));
    }

    @Transactional(readOnly = true)
    public List<PublicDataRecord> getRecentRecords(String userId, String hsCode, String targetCountry) {
        List<String> scopeUserIds = scopeUserIds(userId);
        if (hsCode != null && !hsCode.isBlank() && targetCountry != null && !targetCountry.isBlank()) {
            return recordRepository.findTop50ByRun_RequestedBy_IdInAndHsCodeAndTargetCountryOrderByFetchedAtDesc(
                    scopeUserIds, normalizeHsCode(hsCode), normalizeCountry(targetCountry));
        }
        return recordRepository.findTop50ByRun_RequestedBy_IdInOrderByFetchedAtDesc(scopeUserIds);
    }

    @Transactional(readOnly = true)
    public PublicDataDashboardResponse getDashboard(String userId) {
        List<String> scopeUserIds = scopeUserIds(userId);
        List<PublicDataCollectionRun> runs = runRepository.findTop20ByRequestedBy_IdInOrderByStartedAtDesc(scopeUserIds);
        List<PublicDataRecord> records = recordRepository.findTop50ByRun_RequestedBy_IdInOrderByFetchedAtDesc(scopeUserIds);

        int successCount = runs.stream().mapToInt(PublicDataCollectionRun::getSuccessCount).sum();
        int fallbackCount = runs.stream().mapToInt(PublicDataCollectionRun::getFallbackCount).sum();
        int cacheHitCount = runs.stream().mapToInt(PublicDataCollectionRun::getCacheHitCount).sum();
        int providerCount = runs.stream().mapToInt(PublicDataCollectionRun::getProviderCount).sum();
        double averageConfidence = records.stream()
                .map(PublicDataRecord::getConfidence)
                .filter(confidence -> confidence != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return PublicDataDashboardResponse.builder()
                .runCount(runs.size())
                .recordCount(records.size())
                .successCount(successCount)
                .fallbackCount(fallbackCount)
                .cacheHitCount(cacheHitCount)
                .fallbackRatio(ratio(fallbackCount, providerCount))
                .liveRatio(ratio(successCount, providerCount))
                .cacheRatio(ratio(cacheHitCount, providerCount))
                .averageConfidence(round4(averageConfidence))
                .motieRelatedRecordCount((int) records.stream().filter(record -> record.getProvider().motieRelated()).count())
                .otherGovRecordCount((int) records.stream().filter(record -> !record.getProvider().motieRelated()).count())
                .newestFetchedAt(records.stream().map(PublicDataRecord::getFetchedAt)
                        .filter(value -> value != null).max(LocalDateTime::compareTo).map(DateTimeUtils::formatUtc).orElse(null))
                .oldestFetchedAt(records.stream().map(PublicDataRecord::getFetchedAt)
                        .filter(value -> value != null).min(LocalDateTime::compareTo).map(DateTimeUtils::formatUtc).orElse(null))
                .providers(providerStatuses(records))
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getHealth(String userId) {
        List<String> scopeUserIds = scopeUserIds(userId);
        List<PublicDataCollectionRun> runs = runRepository.findTop20ByRequestedBy_IdInOrderByStartedAtDesc(scopeUserIds);
        List<PublicDataRecord> records = recordRepository.findTop50ByRun_RequestedBy_IdInOrderByFetchedAtDesc(scopeUserIds);
        int totalRecords = records.size();
        int liveCount = (int) records.stream().filter(record -> "live".equals(sourceType(record))).count();
        int cacheCount = (int) records.stream().filter(record -> "cache".equals(sourceType(record))).count();
        int fallbackCount = (int) records.stream().filter(record -> "fallback".equals(sourceType(record))).count();
        int motieRelatedRecords = (int) records.stream().filter(record -> record.getProvider().motieRelated()).count();
        int motieLiveOrCache = (int) records.stream()
                .filter(record -> record.getProvider().motieRelated())
                .filter(record -> "live".equals(sourceType(record)) || "cache".equals(sourceType(record)))
                .count();
        int motieVerifiedOperationLive = (int) records.stream()
                .filter(record -> record.getProvider().motieRelated())
                .filter(record -> "live".equals(sourceType(record)) || "cache".equals(sourceType(record)))
                .filter(record -> evidenceBoolean(record, "operationEndpoint"))
                .count();
        int motieFallbackCount = (int) records.stream()
                .filter(record -> record.getProvider().motieRelated())
                .filter(record -> "fallback".equals(sourceType(record)))
                .count();
        int motieScoreContribution = records.stream()
                .filter(record -> record.getProvider().motieRelated())
                .filter(record -> "live".equals(sourceType(record)) || "cache".equals(sourceType(record)))
                .map(PublicDataRecord::getScoreContribution)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("scope", "current-user-or-company");
        health.put("runCount", runs.size());
        health.put("recordCount", totalRecords);
        health.put("liveCount", liveCount);
        health.put("cacheCount", cacheCount);
        health.put("fallbackCount", fallbackCount);
        health.put("liveOrCacheRatio", ratio(liveCount + cacheCount, Math.max(1, totalRecords)));
        health.put("motieRelatedRecordCount", motieRelatedRecords);
        health.put("motieRelatedLiveOrCacheCount", motieLiveOrCache);
        health.put("motieVerifiedOperationLiveCount", motieVerifiedOperationLive);
        health.put("motieRelatedFallbackCount", motieFallbackCount);
        health.put("motieRelatedScoreContribution", motieScoreContribution);
        health.put("motieRelatedRatio", ratio(motieRelatedRecords, Math.max(1, totalRecords)));
        health.put("motieLiveEvidenceReady", motieVerifiedOperationLive >= 2);
        health.put("latestRunId", runs.isEmpty() ? null : runs.get(0).getId());
        health.put("newestFetchedAt", records.stream().map(PublicDataRecord::getFetchedAt)
                .filter(value -> value != null).max(LocalDateTime::compareTo).map(DateTimeUtils::formatUtc).orElse(null));
        health.put("providers", providerStatuses(records));
        health.put("presentationAnswer", "MOTIE-related live/cache records="
                + motieLiveOrCache + ", verified operation endpoint records=" + motieVerifiedOperationLive
                + ", score contribution=" + motieScoreContribution);
        return health;
    }

    public PublicDataCollectionResult collectManual(String userId, String hsCode, String targetCountry, String productName) {
        return collectManual(userId, hsCode, targetCountry, productName, null);
    }

    public PublicDataCollectionResult collectManual(String userId, String hsCode, String targetCountry,
                                                    String productName, String lawSearchQuery) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        PublicDataRequest request = new PublicDataRequest(null, userId, normalizeHsCode(hsCode),
                normalizeCountry(targetCountry), productName, lawSearchQuery);
        return collect(request);
    }

    private PublicDataCollectionResult collect(PublicDataRequest request) {
        User user = userRepository.findById(request.getUserId()).orElse(null);

        PublicDataCollectionRun run = PublicDataCollectionRun.builder()
                .diagnosisId(request.getDiagnosisId())
                .requestedBy(user)
                .hsCode(request.getHsCode())
                .targetCountry(request.getTargetCountry())
                .productName(request.getProductName())
                .status(PublicDataCollectionStatus.running)
                .providerCount(sortedConnectors().size())
                .startedAt(LocalDateTime.now())
                .build();
        runRepository.save(run);

        List<PublicDataRecord> records = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int fallbackCount = 0;
        int cacheHitCount = 0;

        for (PublicDataConnector connector : sortedConnectors()) {
            PublicDataRecord record = findCache(connector, request);
            if (record != null) {
                PublicDataRecord cacheRecord = cloneCacheRecord(run, record);
                records.add(recordRepository.save(cacheRecord));
                cacheHitCount++;
                if (isFallbackRecord(cacheRecord)) fallbackCount++;
                else if (isSuccessRecord(cacheRecord)) successCount++;
                continue;
            }

            PublicDataConnectorResult result = enabled
                    ? connector.collect(request)
                    : disabledResult(connector, request);
            PublicDataRecord newRecord = toRecord(run, result);
            records.add(recordRepository.save(newRecord));
            if (isFallbackRecord(newRecord)) {
                fallbackCount++;
                if (newRecord.getErrorMessage() != null) errors.add(connector.provider() + ": " + newRecord.getErrorMessage());
            } else if (isSuccessRecord(newRecord)) {
                successCount++;
            } else if (newRecord.getErrorMessage() != null) {
                errors.add(connector.provider() + ": " + newRecord.getErrorMessage());
            }
        }

        run.setSuccessCount(successCount);
        run.setFallbackCount(fallbackCount);
        run.setCacheHitCount(cacheHitCount);
        run.setErrorSummary(errors.isEmpty() ? null : String.join("; ", errors));
        run.setStatus(resolveStatus(successCount, fallbackCount));
        run.setFinishedAt(LocalDateTime.now());
        runRepository.save(run);

        return new PublicDataCollectionResult(run, records);
    }

    private List<PublicDataConnector> sortedConnectors() {
        return connectors.stream()
                .filter(connector -> !INACTIVE_PROVIDERS.contains(connector.provider()))
                .sorted(Comparator.comparing(connector -> connector.provider().name()))
                .collect(Collectors.toList());
    }

    private PublicDataRecord findCache(PublicDataConnector connector, PublicDataRequest request) {
        return recordRepository
                .findTopByProviderAndRequestKeyAndExpiresAtAfterOrderByFetchedAtDesc(
                        connector.provider(), connector.requestKey(request), LocalDateTime.now())
                .filter(this::isReusableCacheRecord)
                .orElse(null);
    }

    private boolean isReusableCacheRecord(PublicDataRecord record) {
        String sourceType = sourceType(record);
        return !record.isFallbackUsed()
                && ("live".equals(sourceType) || "cache".equals(sourceType))
                && !"FALLBACK_ERROR".equals(record.getSourceQualityStatus())
                && !"EMPTY_NOT_CONFIGURED".equals(record.getSourceQualityStatus())
                && !hasConfigurationBlocker(record);
    }

    private boolean hasConfigurationBlocker(PublicDataRecord record) {
        Map<String, Object> normalized = parseNormalized(record);
        Object evidenceValue = normalized.get("collectionEvidence");
        if (evidenceValue instanceof Map<?, ?> evidence
                && Boolean.TRUE.equals(evidence.get("operationEndpoint"))
                && Boolean.FALSE.equals(evidence.get("apiKeyConfigured"))) {
            return true;
        }
        return containsConfigurationBlocker(record.getErrorMessage())
                || containsConfigurationBlocker(record.getSourceQualityStatus())
                || containsConfigurationBlocker(normalized);
    }

    private boolean containsConfigurationBlocker(Object value) {
        if (value == null) return false;
        if (value instanceof String text) {
            String normalized = text.toUpperCase();
            return normalized.contains("API_KEY_NOT_CONFIGURED")
                    || normalized.contains("EMPTY_NOT_CONFIGURED")
                    || normalized.contains("PUBLIC_DATA_DISABLED")
                    || normalized.contains("ENDPOINT_NOT_CONFIGURED");
        }
        if (value instanceof Map<?, ?> map) {
            return map.values().stream().anyMatch(this::containsConfigurationBlocker);
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (containsConfigurationBlocker(item)) return true;
            }
        }
        return false;
    }

    private PublicDataConnectorResult disabledResult(PublicDataConnector connector, PublicDataRequest request) {
        return PublicDataConnectorResult.builder()
                .provider(connector.provider())
                .endpoint("disabled")
                .requestKey(connector.requestKey(request))
                .hsCode(request.getHsCode())
                .targetCountry(request.getTargetCountry())
                .matchedKey("hsCode=" + request.getHsCode() + ";country=" + request.getTargetCountry())
                .usedFor(connector.usedFor())
                .rawPayload("{\"fallback\":true,\"reason\":\"PUBLIC_DATA_ENABLED=false\"}")
                .normalizedPayload(disabledNormalizedPayload(connector))
                .scoreContribution(-1)
                .confidence(0.0)
                .sourceType("fallback")
                .fallbackUsed(true)
                .errorMessage("PUBLIC_DATA_ENABLED=false")
                .build();
    }

    private Map<String, Object> disabledNormalizedPayload(PublicDataConnector connector) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("provider", connector.provider().name());
        payload.put("usedFor", connector.usedFor());
        payload.put("agency", connector.provider().agency());
        payload.put("ministry", connector.provider().ministry());
        payload.put("dataFamily", connector.provider().dataFamily());
        payload.put("motieRelated", connector.provider().motieRelated());
        payload.put("refreshCycleHours", connector.provider().refreshCycleHours());
        payload.put("metrics", java.util.Map.of("recordCount", 0));
        payload.put("metricSchema", List.of("recordCount"));
        payload.put("rawCount", 0);
        payload.put("normalizedCount", 0);
        payload.put("filteredCount", 0);
        payload.put("zeroReason", "PUBLIC_DATA_DISABLED");
        payload.put("requestParamsMasked", java.util.Map.of());
        payload.put("sampleRawKeys", List.of());
        payload.put("scoreContribution", -1);
        payload.put("confidence", 0.0);
        payload.put("fallbackUsed", true);
        payload.put("collectionEvidence", java.util.Map.of(
                "endpointConfigured", false,
                "operationEndpoint", false,
                "endpointType", "disabled",
                "apiKeyConfigured", false,
                "apiKeyStored", false,
                "fallbackUsed", true,
                "reason", "PUBLIC_DATA_ENABLED=false"
        ));
        payload.put("zeroResultDiagnostics", java.util.Map.of(
                "provider", connector.provider().name(),
                "agent", connector.usedFor(),
                "sourceType", "fallback",
                "rawCount", 0,
                "normalizedCount", 0,
                "filteredCount", 0,
                "zeroReason", "PUBLIC_DATA_DISABLED",
                "sampleRawKeys", List.of(),
                "fallbackUsed", true
        ));
        payload.put("scoreFormula", "disabled public data contributes fallback penalty and zero confidence");
        payload.put("reason", "PUBLIC_DATA_ENABLED=false");
        return payload;
    }

    private PublicDataRecord toRecord(PublicDataCollectionRun run, PublicDataConnectorResult result) {
        Map<String, Object> normalizedPayload = normalizePayload(result.getNormalizedPayload());
        String sourceType = result.getSourceType() == null ? result.isFallbackUsed() ? "fallback" : "live" : result.getSourceType();
        SourceQualityStatus qualityStatus = PublicDataQualityClassifier.classify(
                sourceType, result.isFallbackUsed(), normalizedPayload, result.getErrorMessage());
        normalizedPayload.put("sourceQualityStatus", qualityStatus.name());
        normalizedPayload.putIfAbsent("schemaValidation", schemaValidation(qualityStatus, normalizedPayload));
        normalizedPayload.put("scoreContributionLedger", scoreContributionLedger(
                result.getScoreContribution(), result.getConfidence(), qualityStatus));
        return PublicDataRecord.builder()
                .run(run)
                .provider(result.getProvider())
                .endpoint(result.getEndpoint())
                .requestKey(result.getRequestKey())
                .hsCode(result.getHsCode())
                .targetCountry(result.getTargetCountry())
                .matchedKey(result.getMatchedKey())
                .usedFor(result.getUsedFor())
                .rawPayload(result.getRawPayload())
                .normalizedPayload(toJson(normalizedPayload))
                .scoreContribution(guardedScoreContribution(result.getScoreContribution(), result.isFallbackUsed(), sourceType, qualityStatus))
                .confidence(result.getConfidence())
                .sourceType(sourceType)
                .sourceQualityStatus(qualityStatus.name())
                .fallbackUsed(result.isFallbackUsed())
                .cacheHit(false)
                .errorMessage(result.getErrorMessage())
                .fetchedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(Math.max(1, cacheTtlHours)))
                .build();
    }

    private PublicDataRecord cloneCacheRecord(PublicDataCollectionRun run, PublicDataRecord cached) {
        Map<String, Object> normalizedPayload = parseNormalized(cached);
        SourceQualityStatus qualityStatus = SourceQualityStatus.CACHE_VERIFIED;
        normalizedPayload.put("sourceQualityStatus", qualityStatus.name());
        normalizedPayload.putIfAbsent("schemaValidation", schemaValidation(qualityStatus, normalizedPayload));
        normalizedPayload.put("scoreContributionLedger", scoreContributionLedger(
                cached.getScoreContribution(), cached.getConfidence(), qualityStatus));
        return PublicDataRecord.builder()
                .run(run)
                .provider(cached.getProvider())
                .endpoint(cached.getEndpoint())
                .requestKey(cached.getRequestKey())
                .hsCode(cached.getHsCode())
                .targetCountry(cached.getTargetCountry())
                .matchedKey(cached.getMatchedKey())
                .usedFor(cached.getUsedFor())
                .rawPayload(cached.getRawPayload())
                .normalizedPayload(toJson(normalizedPayload))
                .scoreContribution(guardedScoreContribution(cached.getScoreContribution(), false, "cache", qualityStatus))
                .confidence(cached.getConfidence())
                .sourceType("cache")
                .sourceQualityStatus(qualityStatus.name())
                .fallbackUsed(false)
                .cacheHit(true)
                .errorMessage(null)
                .fetchedAt(LocalDateTime.now())
                .expiresAt(cached.getExpiresAt())
                .build();
    }

    private int guardedScoreContribution(Integer scoreContribution, boolean fallbackUsed, String sourceType,
                                         SourceQualityStatus sourceQualityStatus) {
        int value = scoreContribution == null ? 0 : scoreContribution;
        if (fallbackUsed || "fallback".equals(sourceType)
                || !PublicDataQualityClassifier.positiveContributionAllowed(sourceQualityStatus)) {
            return Math.min(value, 0);
        }
        return value;
    }

    private String sourceType(PublicDataRecord record) {
        return PublicDataQualityClassifier.sourceType(record);
    }

    private Map<String, Object> normalizePayload(Map<String, Object> payload) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (payload != null) normalized.putAll(payload);
        return normalized;
    }

    private Map<String, Object> schemaValidation(SourceQualityStatus qualityStatus,
                                                 Map<String, Object> normalizedPayload) {
        if (qualityStatus == SourceQualityStatus.LIVE_VERIFIED || qualityStatus == SourceQualityStatus.CACHE_VERIFIED) {
            return Map.of("status", "PASS", "reason", qualityStatus.name());
        }
        if (qualityStatus == SourceQualityStatus.EMPTY_TRUE_ZERO) {
            return Map.of("status", "PASS", "reason", "verified zero-result response");
        }
        return Map.of("status", "SKIPPED", "reason", String.valueOf(normalizedPayload.getOrDefault("zeroReason", "")));
    }

    private Map<String, Object> scoreContributionLedger(Integer rawContribution, Double confidence,
                                                        SourceQualityStatus qualityStatus) {
        int raw = rawContribution == null ? 0 : rawContribution;
        double conf = confidence == null ? 0.0 : confidence;
        boolean positiveAllowed = PublicDataQualityClassifier.positiveContributionAllowed(qualityStatus);
        int guarded = positiveAllowed ? raw : Math.min(raw, 0);
        return Map.of(
                "rawContribution", raw,
                "confidence", conf,
                "guardedContribution", guarded,
                "positiveContributionAllowed", positiveAllowed,
                "sourceQualityStatus", qualityStatus.name()
        );
    }

    private boolean isSuccessRecord(PublicDataRecord record) {
        String sourceType = sourceType(record);
        return "live".equals(sourceType) || "cache".equals(sourceType);
    }

    private boolean isFallbackRecord(PublicDataRecord record) {
        return "fallback".equals(sourceType(record));
    }

    private PublicDataCollectionStatus resolveStatus(int successCount, int fallbackCount) {
        if (successCount > 0 && fallbackCount == 0) return PublicDataCollectionStatus.done;
        if (successCount > 0) return PublicDataCollectionStatus.partial_failed;
        return PublicDataCollectionStatus.failed;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private List<ProviderStatus> providerStatuses(List<PublicDataRecord> records) {
        return java.util.Arrays.stream(PublicDataProvider.values())
                .map(provider -> {
                    List<PublicDataRecord> providerRecords = records.stream()
                            .filter(record -> record.getProvider() == provider)
                            .toList();
                    int recordCount = providerRecords.size();
                    int fallbackCount = (int) providerRecords.stream().filter(this::isFallbackRecord).count();
                    int cacheCount = (int) providerRecords.stream()
                            .filter(record -> "cache".equals(sourceType(record)))
                            .count();
                    int liveCount = (int) providerRecords.stream()
                            .filter(record -> "live".equals(sourceType(record)))
                            .count();
                    double avgConfidence = providerRecords.stream()
                            .map(PublicDataRecord::getConfidence)
                            .filter(confidence -> confidence != null)
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0.0);
                    return ProviderStatus.builder()
                            .provider(provider.name())
                            .agency(provider.agency())
                            .ministry(provider.ministry())
                            .dataFamily(provider.dataFamily())
                            .motieRelated(provider.motieRelated())
                            .usedFor(provider.usedFor())
                            .refreshCycleHours(provider.refreshCycleHours())
                            .recordCount(recordCount)
                            .liveCount(liveCount)
                            .cacheCount(cacheCount)
                            .fallbackCount(fallbackCount)
                            .fallbackRatio(ratio(fallbackCount, recordCount))
                            .averageConfidence(round4(avgConfidence))
                            .newestFetchedAt(providerRecords.stream().map(PublicDataRecord::getFetchedAt)
                                    .filter(value -> value != null).max(LocalDateTime::compareTo).map(DateTimeUtils::formatUtc).orElse(null))
                            .newestExpiresAt(providerRecords.stream().map(PublicDataRecord::getExpiresAt)
                                    .filter(value -> value != null).max(LocalDateTime::compareTo).map(DateTimeUtils::formatUtc).orElse(null))
                            .operationEndpointSeen(providerRecords.stream().anyMatch(record -> evidenceBoolean(record, "operationEndpoint")))
                            .apiKeyConfiguredSeen(providerRecords.stream().anyMatch(record -> evidenceBoolean(record, "apiKeyConfigured")))
                            .build();
                })
                .toList();
    }

    private boolean evidenceBoolean(PublicDataRecord record, String key) {
        Object value = parseNormalized(record).get("collectionEvidence");
        if (!(value instanceof Map<?, ?> evidence)) return false;
        Object raw = evidence.get(key);
        return raw instanceof Boolean bool && bool;
    }

    private Map<String, Object> parseNormalized(PublicDataRecord record) {
        if (record.getNormalizedPayload() == null || record.getNormalizedPayload().isBlank()) return Map.of();
        try {
            return objectMapper.readValue(record.getNormalizedPayload(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private double ratio(int value, int total) {
        if (total <= 0) return 0.0;
        return round4(value / (double) total);
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private List<String> scopeUserIds(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        if (user.getUserType().name().equals("company")) {
            return membershipRepository.findFirstByUserId(userId)
                    .map(Membership::getCompany)
                    .map(company -> membershipRepository.findByCompanyId(company.getId()).stream()
                            .map(membership -> membership.getUser().getId())
                            .toList())
                    .filter(ids -> !ids.isEmpty())
                    .orElse(List.of(userId));
        }
        return List.of(userId);
    }

    private String normalizeHsCode(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private String normalizeCountry(String value) {
        if (value == null) return "";
        String v = value.trim().toLowerCase().replace(" ", "");
        if (v.equals("독일") || v.equals("germany") || v.equals("de")) return "DE";
        if (v.equals("미국") || v.equals("usa") || v.equals("us")) return "US";
        if (v.equals("일본") || v.equals("japan") || v.equals("jp")) return "JP";
        if (v.equals("중국") || v.equals("china") || v.equals("cn")) return "CN";
        if (v.equals("유럽") || v.equals("eu")) return "EU";
        return value.trim().toUpperCase();
    }
}
