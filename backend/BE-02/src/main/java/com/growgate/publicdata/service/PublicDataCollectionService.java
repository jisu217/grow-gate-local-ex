package com.growgate.publicdata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.membership.entity.Membership;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.publicdata.entity.PublicDataCollectionRun;
import com.growgate.publicdata.entity.PublicDataRecord;
import com.growgate.publicdata.enums.PublicDataCollectionStatus;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PublicDataCollectionService {

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
        User user = userRepository.findById(userId).orElse(null);

        PublicDataCollectionRun run = PublicDataCollectionRun.builder()
                .diagnosisId(diagnosisId)
                .requestedBy(user)
                .hsCode(request.getHsCode())
                .targetCountry(request.getTargetCountry())
                .productName(productName)
                .status(PublicDataCollectionStatus.running)
                .providerCount(connectors.size())
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
                if (cacheRecord.isFallbackUsed()) fallbackCount++;
                else successCount++;
                continue;
            }

            PublicDataConnectorResult result = enabled
                    ? connector.collect(request)
                    : disabledResult(connector, request);
            PublicDataRecord newRecord = toRecord(run, result);
            records.add(recordRepository.save(newRecord));
            if (newRecord.isFallbackUsed()) {
                fallbackCount++;
                if (newRecord.getErrorMessage() != null) errors.add(connector.provider() + ": " + newRecord.getErrorMessage());
            } else {
                successCount++;
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

    public PublicDataCollectionResult collectManual(String userId, String hsCode, String targetCountry, String productName) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return collectForDiagnosis(null, userId, hsCode, targetCountry, productName);
    }

    private List<PublicDataConnector> sortedConnectors() {
        return connectors.stream()
                .sorted(Comparator.comparing(connector -> connector.provider().name()))
                .collect(Collectors.toList());
    }

    private PublicDataRecord findCache(PublicDataConnector connector, PublicDataRequest request) {
        return recordRepository
                .findTopByProviderAndRequestKeyAndExpiresAtAfterOrderByFetchedAtDesc(
                        connector.provider(), connector.requestKey(request), LocalDateTime.now())
                .orElse(null);
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
                .normalizedPayload(java.util.Map.of(
                        "provider", connector.provider().name(),
                        "usedFor", connector.usedFor(),
                        "metrics", java.util.Map.of("recordCount", 0),
                        "scoreContribution", -1,
                        "confidence", 0.0,
                        "fallbackUsed", true,
                        "reason", "PUBLIC_DATA_ENABLED=false"
                ))
                .scoreContribution(-1)
                .confidence(0.0)
                .sourceType("fallback")
                .fallbackUsed(true)
                .errorMessage("PUBLIC_DATA_ENABLED=false")
                .build();
    }

    private PublicDataRecord toRecord(PublicDataCollectionRun run, PublicDataConnectorResult result) {
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
                .normalizedPayload(toJson(result.getNormalizedPayload()))
                .scoreContribution(result.getScoreContribution())
                .confidence(result.getConfidence())
                .sourceType(result.getSourceType() == null ? result.isFallbackUsed() ? "fallback" : "live" : result.getSourceType())
                .fallbackUsed(result.isFallbackUsed())
                .cacheHit(false)
                .errorMessage(result.getErrorMessage())
                .fetchedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(Math.max(1, cacheTtlHours)))
                .build();
    }

    private PublicDataRecord cloneCacheRecord(PublicDataCollectionRun run, PublicDataRecord cached) {
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
                .normalizedPayload(cached.getNormalizedPayload())
                .scoreContribution(cached.getScoreContribution())
                .confidence(cached.getConfidence())
                .sourceType("cache")
                .fallbackUsed(cached.isFallbackUsed())
                .cacheHit(true)
                .errorMessage(cached.getErrorMessage())
                .fetchedAt(LocalDateTime.now())
                .expiresAt(cached.getExpiresAt())
                .build();
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
