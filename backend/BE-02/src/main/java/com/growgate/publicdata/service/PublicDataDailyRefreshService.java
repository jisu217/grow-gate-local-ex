package com.growgate.publicdata.service;

import com.growgate.common.DateTimeUtils;
import com.growgate.product.entity.Product;
import com.growgate.product.repository.ProductRepository;
import com.growgate.publicdata.dto.PublicDataDailyRefreshResponse;
import com.growgate.publicdata.entity.PublicDataCollectionRun;
import com.growgate.publicdata.entity.PublicDataRecord;
import com.growgate.publicdata.enums.PublicDataProvider;
import com.growgate.publicdata.repository.PublicDataRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDataDailyRefreshService {

    private final ProductRepository productRepository;
    private final PublicDataCollectionService collectionService;
    private final PublicDataRecordRepository recordRepository;
    private final List<PublicDataConnector> connectors;

    @Value("${public-data.daily-refresh.enabled:true}")
    private boolean enabled;

    @Value("${public-data.daily-refresh.max-products:30}")
    private int maxProducts;

    @Value("${public-data.daily-refresh.freshness-hours:20}")
    private long freshnessHours;

    @Scheduled(cron = "${public-data.daily-refresh.cron:0 0 8 * * *}",
            zone = "${public-data.daily-refresh.zone:Asia/Seoul}")
    public void scheduledDailyRefresh() {
        if (!enabled) {
            log.info("public data daily refresh skipped because public-data.daily-refresh.enabled=false");
            return;
        }

        PublicDataDailyRefreshResponse result = refreshGlobal("scheduled");
        log.info("public data daily refresh done planned={} collected={} skippedFresh={} failed={} success={} fallback={} cache={}",
                result.getPlannedCount(),
                result.getCollectedCount(),
                result.getSkippedFreshCount(),
                result.getFailedCount(),
                result.getSuccessCount(),
                result.getFallbackCount(),
                result.getCacheHitCount());
    }

    public PublicDataDailyRefreshResponse refreshGlobal(String trigger) {
        List<Product> products = productRepository.findDailyRefreshCandidates(PageRequest.of(0, Math.max(1, maxProducts)));
        return refreshProducts(trigger, products);
    }

    public PublicDataDailyRefreshResponse refreshForUser(String userId) {
        List<Product> products = productRepository.findDailyRefreshCandidatesByOwnerId(
                userId, PageRequest.of(0, Math.max(1, maxProducts)));
        return refreshProducts("manual:user:" + userId, products);
    }

    private PublicDataDailyRefreshResponse refreshProducts(String trigger, List<Product> products) {
        LocalDateTime startedAt = LocalDateTime.now();
        List<PublicDataDailyRefreshResponse.Item> items = new ArrayList<>();
        int collectedCount = 0;
        int skippedFreshCount = 0;
        int failedCount = 0;
        int successCount = 0;
        int fallbackCount = 0;
        int cacheHitCount = 0;
        int externalDataCount = 0;
        Set<String> failureReasons = new LinkedHashSet<>();

        for (Product product : products) {
            String hsCode = normalizeHsCode(product.getHsCode());
            String targetCountry = normalizeCountry(product.getTargetCountry());
            if (hsCode.length() < 6 || targetCountry.isBlank()) {
                String reason = "HS code 6자리 이상과 대상국이 필요합니다.";
                skippedFreshCount++;
                items.add(item(product, hsCode, targetCountry, "skipped", null,
                        0, reason));
                failureReasons.add(reason);
                continue;
            }

            if (hasFreshCoverage(hsCode, targetCountry)) {
                skippedFreshCount++;
                items.add(item(product, hsCode, targetCountry, "skipped_fresh", null,
                        0, "최근 " + freshnessHours + "시간 내 provider coverage가 존재합니다."));
                continue;
            }

            try {
                PublicDataCollectionResult result = collectionService.collectForDiagnosis(
                        null,
                        product.getOwner().getId(),
                        hsCode,
                        targetCountry,
                        product.getName()
                );
                PublicDataCollectionRun run = result.getRun();
                collectedCount++;
                successCount += run.getSuccessCount();
                fallbackCount += run.getFallbackCount();
                cacheHitCount += run.getCacheHitCount();
                int itemExternalDataCount = externalDataCount(result.getRecords());
                externalDataCount += itemExternalDataCount;
                if (run.getErrorSummary() != null && !run.getErrorSummary().isBlank()) {
                    failureReasons.add(run.getErrorSummary());
                }
                items.add(item(product, hsCode, targetCountry, run.getStatus().name(), run,
                        itemExternalDataCount, null));
            } catch (Exception e) {
                String reason = safeReason(e);
                failedCount++;
                failureReasons.add(reason);
                items.add(item(product, hsCode, targetCountry, "failed", null, 0, reason));
                log.warn("public data daily refresh failed productId={} hsCode={} targetCountry={}",
                        product.getId(), hsCode, targetCountry, e);
            }
        }

        return PublicDataDailyRefreshResponse.builder()
                .trigger(trigger)
                .plannedCount(products.size())
                .collectedCount(collectedCount)
                .skippedFreshCount(skippedFreshCount)
                .failedCount(failedCount)
                .successCount(successCount)
                .fallbackCount(fallbackCount)
                .cacheHitCount(cacheHitCount)
                .providerCount(connectors.size())
                .externalDataCount(externalDataCount)
                .diagnosisUsable(successCount + cacheHitCount > 0)
                .availabilityLabel(successCount + cacheHitCount > 0 ? "진단 반영 가능" : "확인 필요")
                .providers(connectors.stream().map(connector -> connector.provider().name()).sorted().toList())
                .failureReasons(new ArrayList<>(failureReasons))
                .startedAt(DateTimeUtils.formatUtc(startedAt))
                .finishedAt(DateTimeUtils.formatUtc(LocalDateTime.now()))
                .items(items)
                .build();
    }

    private boolean hasFreshCoverage(String hsCode, String targetCountry) {
        int providerCount = Math.max(1, connectors.size());
        long collectedProviders = recordRepository.countDistinctProvidersCollectedSince(
                hsCode,
                targetCountry,
                LocalDateTime.now().minusHours(Math.max(1, freshnessHours))
        );
        return collectedProviders >= providerCount;
    }

    private PublicDataDailyRefreshResponse.Item item(Product product, String hsCode, String targetCountry,
                                                    String status, PublicDataCollectionRun run,
                                                    int externalDataCount, String reason) {
        int liveOrCacheCount = run == null ? 0 : run.getSuccessCount() + run.getCacheHitCount();
        return PublicDataDailyRefreshResponse.Item.builder()
                .productId(product.getId())
                .productName(product.getName())
                .ownerId(product.getOwner().getId())
                .hsCode(hsCode)
                .targetCountry(targetCountry)
                .status(status)
                .runId(run == null ? null : run.getId())
                .successCount(run == null ? 0 : run.getSuccessCount())
                .fallbackCount(run == null ? 0 : run.getFallbackCount())
                .cacheHitCount(run == null ? 0 : run.getCacheHitCount())
                .providerCount(run == null ? 0 : run.getProviderCount())
                .externalDataCount(externalDataCount)
                .diagnosisUsable(liveOrCacheCount > 0)
                .availabilityLabel(liveOrCacheCount > 0 ? "진단 반영 가능" : "확인 필요")
                .reason(reason)
                .build();
    }

    private int externalDataCount(List<PublicDataRecord> records) {
        if (records == null) return 0;
        return (int) records.stream()
                .filter(record -> record.getProvider() == PublicDataProvider.EXPORT_EVIDENCE_FEED)
                .filter(record -> !record.isFallbackUsed())
                .count();
    }

    private String safeReason(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) return e.getClass().getSimpleName();
        return message.length() > 200 ? message.substring(0, 200) : message;
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
