package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.entity.PublicDataCollectionRun;
import com.growgate.publicdata.entity.PublicDataRecord;
import com.growgate.publicdata.enums.PublicDataCollectionStatus;
import com.growgate.publicdata.enums.PublicDataProvider;
import com.growgate.publicdata.repository.PublicDataCollectionRunRepository;
import com.growgate.publicdata.repository.PublicDataRecordRepository;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.user.entity.User;
import com.growgate.user.enums.UserType;
import com.growgate.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicDataCollectionServiceTest {

    @Test
    void collectForDiagnosisPersistsFallbackRecordsWhenLiveCollectionDisabled() {
        PublicDataConnector connector = mock(PublicDataConnector.class);
        when(connector.provider()).thenReturn(PublicDataProvider.KCS_TRADE);
        when(connector.usedFor()).thenReturn("market");
        when(connector.requestKey(any(PublicDataRequest.class))).thenReturn("KCS_TRADE:850760:DE");

        PublicDataCollectionRunRepository runRepository = mock(PublicDataCollectionRunRepository.class);
        PublicDataRecordRepository recordRepository = mock(PublicDataRecordRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        MembershipRepository membershipRepository = mock(MembershipRepository.class);
        User user = user("user-1");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(runRepository.save(any(PublicDataCollectionRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recordRepository.save(any(PublicDataRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recordRepository.findTopByProviderAndRequestKeyAndExpiresAtAfterOrderByFetchedAtDesc(
                eq(PublicDataProvider.KCS_TRADE), eq("KCS_TRADE:850760:DE"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        PublicDataCollectionService service = service(
                List.of(connector), runRepository, recordRepository, userRepository, membershipRepository);
        ReflectionTestUtils.setField(service, "enabled", false);
        ReflectionTestUtils.setField(service, "cacheTtlHours", 24L);

        PublicDataCollectionResult result = service.collectForDiagnosis(
                "diagnosis-1", "user-1", "850760", "독일", "배터리팩");

        assertThat(result.getRun().getStatus()).isEqualTo(PublicDataCollectionStatus.failed);
        assertThat(result.getRun().getFallbackCount()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).isFallbackUsed()).isTrue();
        assertThat(result.getRecords().get(0).getErrorMessage()).isEqualTo("PUBLIC_DATA_ENABLED=false");
        verify(connector, never()).collect(any(PublicDataRequest.class));
    }

    @Test
    void collectForDiagnosisReusesValidCacheBeforeCallingConnector() {
        PublicDataConnector connector = mock(PublicDataConnector.class);
        when(connector.provider()).thenReturn(PublicDataProvider.KCS_TRADE);
        when(connector.usedFor()).thenReturn("market");
        when(connector.requestKey(any(PublicDataRequest.class))).thenReturn("KCS_TRADE:850760:DE");

        PublicDataCollectionRunRepository runRepository = mock(PublicDataCollectionRunRepository.class);
        PublicDataRecordRepository recordRepository = mock(PublicDataRecordRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        MembershipRepository membershipRepository = mock(MembershipRepository.class);
        User user = user("user-1");
        PublicDataRecord cached = PublicDataRecord.builder()
                .provider(PublicDataProvider.KCS_TRADE)
                .endpoint("https://api.example.test/kcs")
                .requestKey("KCS_TRADE:850760:DE")
                .hsCode("850760")
                .targetCountry("DE")
                .matchedKey("hsCode=850760;country=DE")
                .usedFor("market")
                .rawPayload("{\"ok\":true}")
                .normalizedPayload("{\"signal\":\"live\"}")
                .scoreContribution(8)
                .confidence(0.82)
                .sourceType("live")
                .fallbackUsed(false)
                .cacheHit(false)
                .fetchedAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().plusHours(2))
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(runRepository.save(any(PublicDataCollectionRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recordRepository.save(any(PublicDataRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recordRepository.findTopByProviderAndRequestKeyAndExpiresAtAfterOrderByFetchedAtDesc(
                eq(PublicDataProvider.KCS_TRADE), eq("KCS_TRADE:850760:DE"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(cached));

        PublicDataCollectionService service = service(
                List.of(connector), runRepository, recordRepository, userRepository, membershipRepository);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "cacheTtlHours", 24L);

        PublicDataCollectionResult result = service.collectForDiagnosis(
                "diagnosis-1", "user-1", "850760", "DE", "배터리팩");

        assertThat(result.getRun().getStatus()).isEqualTo(PublicDataCollectionStatus.done);
        assertThat(result.getRun().getCacheHitCount()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).isCacheHit()).isTrue();
        assertThat(result.getRecords().get(0).getScoreContribution()).isEqualTo(8);
        verify(connector, never()).collect(any(PublicDataRequest.class));
    }

    @Test
    void collectForDiagnosisClampsFallbackPositiveContributionBeforePersisting() {
        PublicDataConnector connector = mock(PublicDataConnector.class);
        when(connector.provider()).thenReturn(PublicDataProvider.KOTRA_MARKET);
        when(connector.usedFor()).thenReturn("market");
        when(connector.requestKey(any(PublicDataRequest.class))).thenReturn("KOTRA_MARKET:DE:Battery pack");
        when(connector.collect(any(PublicDataRequest.class))).thenReturn(PublicDataConnectorResult.builder()
                .provider(PublicDataProvider.KOTRA_MARKET)
                .endpoint("https://api.example.test/kotra")
                .requestKey("KOTRA_MARKET:DE:Battery pack")
                .hsCode("850760")
                .targetCountry("DE")
                .matchedKey("hsCode=850760;country=DE")
                .usedFor("market")
                .rawPayload("{\"fallback\":true}")
                .normalizedPayload(Map.of("metrics", Map.of("recordCount", 0), "scoreContribution", 9))
                .scoreContribution(9)
                .confidence(0.0)
                .sourceType("fallback")
                .fallbackUsed(true)
                .errorMessage("forced fallback")
                .build());

        PublicDataCollectionRunRepository runRepository = mock(PublicDataCollectionRunRepository.class);
        PublicDataRecordRepository recordRepository = mock(PublicDataRecordRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        MembershipRepository membershipRepository = mock(MembershipRepository.class);
        User user = user("user-1");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(runRepository.save(any(PublicDataCollectionRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recordRepository.save(any(PublicDataRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recordRepository.findTopByProviderAndRequestKeyAndExpiresAtAfterOrderByFetchedAtDesc(
                eq(PublicDataProvider.KOTRA_MARKET), eq("KOTRA_MARKET:DE:Battery pack"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        PublicDataCollectionService service = service(
                List.of(connector), runRepository, recordRepository, userRepository, membershipRepository);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "cacheTtlHours", 24L);

        PublicDataCollectionResult result = service.collectForDiagnosis(
                "diagnosis-1", "user-1", "850760", "DE", "Battery pack");

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).isFallbackUsed()).isTrue();
        assertThat(result.getRecords().get(0).getScoreContribution()).isLessThanOrEqualTo(0);
    }

    @Test
    void getRecentRunsScopesPersonalUsersToOwnPublicDataRuns() {
        PublicDataCollectionRunRepository runRepository = mock(PublicDataCollectionRunRepository.class);
        PublicDataRecordRepository recordRepository = mock(PublicDataRecordRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        MembershipRepository membershipRepository = mock(MembershipRepository.class);
        User user = user("user-1");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(runRepository.findTop20ByRequestedBy_IdInOrderByStartedAtDesc(List.of("user-1")))
                .thenReturn(List.of());

        PublicDataCollectionService service = service(
                List.of(), runRepository, recordRepository, userRepository, membershipRepository);

        List<PublicDataCollectionRun> runs = service.getRecentRuns("user-1");

        assertThat(runs).isEmpty();
        verify(runRepository).findTop20ByRequestedBy_IdInOrderByStartedAtDesc(List.of("user-1"));
    }

    @Test
    void getHealthSeparatesMotieLiveOperationEvidenceFromOtherGovRecords() {
        PublicDataCollectionRunRepository runRepository = mock(PublicDataCollectionRunRepository.class);
        PublicDataRecordRepository recordRepository = mock(PublicDataRecordRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        MembershipRepository membershipRepository = mock(MembershipRepository.class);
        User user = user("user-1");
        PublicDataCollectionRun run = PublicDataCollectionRun.builder()
                .id("run-1")
                .requestedBy(user)
                .startedAt(LocalDateTime.now())
                .build();

        PublicDataRecord kotra = liveRecord(PublicDataProvider.KOTRA_MARKET, "market", 5);
        PublicDataRecord safety = liveRecord(PublicDataProvider.SAFETY_RECALL, "recall", -3);
        PublicDataRecord kcs = liveRecord(PublicDataProvider.KCS_TRADE, "market", 8);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(runRepository.findTop20ByRequestedBy_IdInOrderByStartedAtDesc(List.of("user-1")))
                .thenReturn(List.of(run));
        when(recordRepository.findTop50ByRun_RequestedBy_IdInOrderByFetchedAtDesc(List.of("user-1")))
                .thenReturn(List.of(kotra, safety, kcs));

        PublicDataCollectionService service = service(
                List.of(), runRepository, recordRepository, userRepository, membershipRepository);

        Map<String, Object> health = service.getHealth("user-1");

        assertThat(health.get("motieRelatedRecordCount")).isEqualTo(2);
        assertThat(health.get("motieRelatedLiveOrCacheCount")).isEqualTo(2);
        assertThat(health.get("motieVerifiedOperationLiveCount")).isEqualTo(2);
        assertThat(health.get("motieRelatedScoreContribution")).isEqualTo(2);
        assertThat(health.get("motieLiveEvidenceReady")).isEqualTo(true);
    }

    private PublicDataCollectionService service(List<PublicDataConnector> connectors,
                                                PublicDataCollectionRunRepository runRepository,
                                                PublicDataRecordRepository recordRepository,
                                                UserRepository userRepository,
                                                MembershipRepository membershipRepository) {
        return new PublicDataCollectionService(
                connectors,
                runRepository,
                recordRepository,
                userRepository,
                membershipRepository,
                new ObjectMapper()
        );
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

    private PublicDataRecord liveRecord(PublicDataProvider provider, String usedFor, int contribution) {
        return PublicDataRecord.builder()
                .provider(provider)
                .endpoint("https://api.example.test/" + provider.name())
                .requestKey(provider.name() + ":request")
                .hsCode("850760")
                .targetCountry("DE")
                .matchedKey("hsCode=850760;country=DE")
                .usedFor(usedFor)
                .rawPayload("{\"items\":[]}")
                .normalizedPayload("{\"collectionEvidence\":{\"operationEndpoint\":true,\"apiKeyConfigured\":true}}")
                .scoreContribution(contribution)
                .confidence(0.8)
                .sourceType("live")
                .fallbackUsed(false)
                .cacheHit(false)
                .fetchedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }
}
