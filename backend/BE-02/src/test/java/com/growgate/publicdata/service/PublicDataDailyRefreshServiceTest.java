package com.growgate.publicdata.service;

import com.growgate.product.entity.Product;
import com.growgate.product.repository.ProductRepository;
import com.growgate.publicdata.entity.PublicDataCollectionRun;
import com.growgate.publicdata.entity.PublicDataRecord;
import com.growgate.publicdata.enums.PublicDataCollectionStatus;
import com.growgate.publicdata.enums.PublicDataProvider;
import com.growgate.publicdata.repository.PublicDataRecordRepository;
import com.growgate.user.entity.User;
import com.growgate.user.enums.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicDataDailyRefreshServiceTest {

    @Test
    void refreshForUserSkipsProductWhenFreshProviderCoverageExists() {
        ProductRepository productRepository = mock(ProductRepository.class);
        PublicDataCollectionService collectionService = mock(PublicDataCollectionService.class);
        PublicDataRecordRepository recordRepository = mock(PublicDataRecordRepository.class);
        PublicDataConnector connector = connector(PublicDataProvider.KCS_TRADE);

        when(productRepository.findDailyRefreshCandidatesByOwnerId(eq("user-1"), any(Pageable.class)))
                .thenReturn(List.of(product("product-1", user("user-1"), "850760", "독일")));
        when(recordRepository.countDistinctProvidersCollectedSince(eq("850760"), eq("DE"), any(LocalDateTime.class)))
                .thenReturn(1L);

        PublicDataDailyRefreshService service = service(
                productRepository, collectionService, recordRepository, List.of(connector));

        var response = service.refreshForUser("user-1");

        assertThat(response.getPlannedCount()).isEqualTo(1);
        assertThat(response.getProviderCount()).isEqualTo(1);
        assertThat(response.getProviders()).containsExactly("KCS_TRADE");
        assertThat(response.getCollectedCount()).isZero();
        assertThat(response.getSkippedFreshCount()).isEqualTo(1);
        assertThat(response.getItems().get(0).getStatus()).isEqualTo("skipped_fresh");
        verify(collectionService, never()).collectForDiagnosis(any(), any(), any(), any(), any());
    }

    @Test
    void refreshForUserCollectsWhenCoverageIsStale() {
        ProductRepository productRepository = mock(ProductRepository.class);
        PublicDataCollectionService collectionService = mock(PublicDataCollectionService.class);
        PublicDataRecordRepository recordRepository = mock(PublicDataRecordRepository.class);
        PublicDataConnector connector = connector(PublicDataProvider.KCS_TRADE);
        PublicDataConnector externalConnector = connector(PublicDataProvider.EXPORT_EVIDENCE_FEED);
        User user = user("user-1");
        Product product = product("product-1", user, "850760", "DE");
        PublicDataCollectionRun run = PublicDataCollectionRun.builder()
                .id("run-1")
                .requestedBy(user)
                .hsCode("850760")
                .targetCountry("DE")
                .productName("배터리팩")
                .status(PublicDataCollectionStatus.partial_failed)
                .providerCount(2)
                .successCount(1)
                .fallbackCount(1)
                .cacheHitCount(0)
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .build();
        PublicDataRecord externalRecord = PublicDataRecord.builder()
                .run(run)
                .provider(PublicDataProvider.EXPORT_EVIDENCE_FEED)
                .sourceType("external_feed")
                .fallbackUsed(false)
                .cacheHit(false)
                .fetchedAt(LocalDateTime.now())
                .build();

        when(productRepository.findDailyRefreshCandidatesByOwnerId(eq("user-1"), any(Pageable.class)))
                .thenReturn(List.of(product));
        when(recordRepository.countDistinctProvidersCollectedSince(eq("850760"), eq("DE"), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(collectionService.collectForDiagnosis(null, "user-1", "850760", "DE", "배터리팩"))
                .thenReturn(new PublicDataCollectionResult(run, List.of(externalRecord)));

        PublicDataDailyRefreshService service = service(
                productRepository, collectionService, recordRepository, List.of(connector, externalConnector));

        var response = service.refreshForUser("user-1");

        assertThat(response.getPlannedCount()).isEqualTo(1);
        assertThat(response.getCollectedCount()).isEqualTo(1);
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFallbackCount()).isEqualTo(1);
        assertThat(response.getExternalDataCount()).isEqualTo(1);
        assertThat(response.isDiagnosisUsable()).isTrue();
        assertThat(response.getAvailabilityLabel()).isEqualTo("진단 반영 가능");
        assertThat(response.getItems().get(0).getRunId()).isEqualTo("run-1");
        assertThat(response.getItems().get(0).getExternalDataCount()).isEqualTo(1);
        assertThat(response.getItems().get(0).getAvailabilityLabel()).isEqualTo("진단 반영 가능");
        verify(collectionService).collectForDiagnosis(null, "user-1", "850760", "DE", "배터리팩");
    }

    private PublicDataDailyRefreshService service(ProductRepository productRepository,
                                                  PublicDataCollectionService collectionService,
                                                  PublicDataRecordRepository recordRepository,
                                                  List<PublicDataConnector> connectors) {
        PublicDataDailyRefreshService service = new PublicDataDailyRefreshService(
                productRepository,
                collectionService,
                recordRepository,
                connectors
        );
        ReflectionTestUtils.setField(service, "maxProducts", 30);
        ReflectionTestUtils.setField(service, "freshnessHours", 20L);
        return service;
    }

    private PublicDataConnector connector(PublicDataProvider provider) {
        PublicDataConnector connector = mock(PublicDataConnector.class);
        when(connector.provider()).thenReturn(provider);
        return connector;
    }

    private Product product(String id, User owner, String hsCode, String targetCountry) {
        return Product.builder()
                .id(id)
                .owner(owner)
                .name("배터리팩")
                .hsCode(hsCode)
                .targetCountry(targetCountry)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private User user(String id) {
        return User.builder()
                .id(id)
                .email(id + "@growgate.local")
                .passwordHash("encoded")
                .name("테스트 사용자")
                .userType(UserType.personal)
                .build();
    }
}
