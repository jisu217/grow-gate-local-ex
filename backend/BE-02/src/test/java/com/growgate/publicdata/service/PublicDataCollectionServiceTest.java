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
        assertThat(result.getRecords().get(0).getSourceQualityStatus()).isEqualTo("FALLBACK_ERROR");
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
        assertThat(result.getRecords().get(0).getSourceQualityStatus()).isEqualTo("CACHE_VERIFIED");
        verify(connector, never()).collect(any(PublicDataRequest.class));
    }

    @Test
    void collectForDiagnosisDoesNotReuseApiKeyNotConfiguredCache() {
        PublicDataConnector connector = mock(PublicDataConnector.class);
        when(connector.provider()).thenReturn(PublicDataProvider.KIPRIS_FTO);
        when(connector.usedFor()).thenReturn("fto");
        when(connector.requestKey(any(PublicDataRequest.class))).thenReturn("KIPRIS_FTO:DE:Battery pack");
        when(connector.collect(any(PublicDataRequest.class))).thenReturn(PublicDataConnectorResult.builder()
                .provider(PublicDataProvider.KIPRIS_FTO)
                .endpoint("https://api.example.test/kipris")
                .requestKey("KIPRIS_FTO:DE:Battery pack")
                .hsCode("850760")
                .targetCountry("DE")
                .matchedKey("hsCode=850760;country=DE")
                .usedFor("fto")
                .rawPayload("{\"items\":[{\"title\":\"sensor patent\"}]}")
                .normalizedPayload(Map.of(
                        "collectionEvidence", Map.of("operationEndpoint", true, "apiKeyConfigured", true),
                        "metrics", Map.of("recordCount", 1)))
                .scoreContribution(2)
                .confidence(0.75)
                .sourceType("live")
                .fallbackUsed(false)
                .build());

        PublicDataCollectionRunRepository runRepository = mock(PublicDataCollectionRunRepository.class);
        PublicDataRecordRepository recordRepository = mock(PublicDataRecordRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        MembershipRepository membershipRepository = mock(MembershipRepository.class);
        User user = user("user-1");
        PublicDataRecord staleMissingKeyCache = PublicDataRecord.builder()
                .provider(PublicDataProvider.KIPRIS_FTO)
                .endpoint("https://api.example.test/kipris")
                .requestKey("KIPRIS_FTO:DE:Battery pack")
                .hsCode("850760")
                .targetCountry("DE")
                .matchedKey("hsCode=850760;country=DE")
                .usedFor("fto")
                .rawPayload("{\"items\":[]}")
                .normalizedPayload("""
                        {"zeroReason":"API_KEY_NOT_CONFIGURED",
                         "collectionEvidence":{"operationEndpoint":true,"apiKeyConfigured":false}}
                        """)
                .scoreContribution(0)
                .confidence(0.0)
                .sourceType("cache")
                .sourceQualityStatus("CACHE_VERIFIED")
                .fallbackUsed(false)
                .cacheHit(true)
                .errorMessage(null)
                .fetchedAt(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().plusHours(2))
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(runRepository.save(any(PublicDataCollectionRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recordRepository.save(any(PublicDataRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recordRepository.findTopByProviderAndRequestKeyAndExpiresAtAfterOrderByFetchedAtDesc(
                eq(PublicDataProvider.KIPRIS_FTO), eq("KIPRIS_FTO:DE:Battery pack"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(staleMissingKeyCache));

        PublicDataCollectionService service = service(
                List.of(connector), runRepository, recordRepository, userRepository, membershipRepository);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "cacheTtlHours", 24L);

        PublicDataCollectionResult result = service.collectForDiagnosis(
                "diagnosis-1", "user-1", "850760", "DE", "Battery pack");

        assertThat(result.getRun().getStatus()).isEqualTo(PublicDataCollectionStatus.done);
        assertThat(result.getRun().getCacheHitCount()).isZero();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getSourceType()).isEqualTo("live");
        assertThat(result.getRecords().get(0).isCacheHit()).isFalse();
        assertThat(result.getRecords().get(0).getSourceQualityStatus()).isEqualTo("LIVE_VERIFIED");
        verify(connector).collect(any(PublicDataRequest.class));
    }

    @Test
    void collectForDiagnosisDoesNotReuseFallbackRecordAsCache() {
        PublicDataConnector connector = mock(PublicDataConnector.class);
        when(connector.provider()).thenReturn(PublicDataProvider.SAFETY_RECALL);
        when(connector.usedFor()).thenReturn("recall");
        when(connector.requestKey(any(PublicDataRequest.class))).thenReturn("SAFETY_RECALL:배터리팩");
        when(connector.collect(any(PublicDataRequest.class))).thenReturn(PublicDataConnectorResult.builder()
                .provider(PublicDataProvider.SAFETY_RECALL)
                .endpoint("csv:classpath:publicdata/kats-domestic-recall-20230809.csv")
                .requestKey("SAFETY_RECALL:배터리팩")
                .hsCode("850760")
                .targetCountry("DE")
                .matchedKey("product=배터리팩")
                .usedFor("recall")
                .rawPayload("{\"source\":\"csv\"}")
                .normalizedPayload(Map.of(
                        "collectionEvidence", Map.of(
                                "operationEndpoint", false,
                                "apiKeyConfigured", false,
                                "reason", "API_KEY_NOT_CONFIGURED; CSV cache available"),
                        "metrics", Map.of("recordCount", 881)))
                .scoreContribution(-2)
                .confidence(0.62)
                .sourceType("cache")
                .fallbackUsed(false)
                .build());

        PublicDataCollectionRunRepository runRepository = mock(PublicDataCollectionRunRepository.class);
        PublicDataRecordRepository recordRepository = mock(PublicDataRecordRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        MembershipRepository membershipRepository = mock(MembershipRepository.class);
        User user = user("user-1");
        PublicDataRecord staleFallback = PublicDataRecord.builder()
                .provider(PublicDataProvider.SAFETY_RECALL)
                .endpoint("https://www.safetykorea.kr/release/openapi")
                .requestKey("SAFETY_RECALL:배터리팩")
                .hsCode("850760")
                .targetCountry("DE")
                .matchedKey("product=배터리팩")
                .usedFor("recall")
                .rawPayload("{\"fallback\":true}")
                .normalizedPayload("{\"sourceQualityStatus\":\"FALLBACK_ERROR\"}")
                .scoreContribution(-2)
                .confidence(0.0)
                .sourceType("fallback")
                .sourceQualityStatus("FALLBACK_ERROR")
                .fallbackUsed(true)
                .cacheHit(false)
                .errorMessage("SAFETY_RECALL API key is not configured")
                .fetchedAt(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().plusHours(2))
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(runRepository.save(any(PublicDataCollectionRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recordRepository.save(any(PublicDataRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recordRepository.findTopByProviderAndRequestKeyAndExpiresAtAfterOrderByFetchedAtDesc(
                eq(PublicDataProvider.SAFETY_RECALL), eq("SAFETY_RECALL:배터리팩"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(staleFallback));

        PublicDataCollectionService service = service(
                List.of(connector), runRepository, recordRepository, userRepository, membershipRepository);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "cacheTtlHours", 24L);

        PublicDataCollectionResult result = service.collectForDiagnosis(
                "diagnosis-1", "user-1", "850760", "DE", "배터리팩");

        assertThat(result.getRun().getStatus()).isEqualTo(PublicDataCollectionStatus.done);
        assertThat(result.getRun().getSuccessCount()).isEqualTo(1);
        assertThat(result.getRun().getFallbackCount()).isZero();
        assertThat(result.getRun().getCacheHitCount()).isZero();
        assertThat(result.getRecords().get(0).getSourceType()).isEqualTo("cache");
        assertThat(result.getRecords().get(0).isFallbackUsed()).isFalse();
        assertThat(result.getRecords().get(0).getErrorMessage()).isNull();
        verify(connector).collect(any(PublicDataRequest.class));
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
        assertThat(result.getRecords().get(0).getSourceQualityStatus()).isEqualTo("FALLBACK_ERROR");
    }

    @Test
    void collectForDiagnosisClampsEmptyNotConfiguredPositiveContributionBeforePersisting() {
        PublicDataConnector connector = mock(PublicDataConnector.class);
        when(connector.provider()).thenReturn(PublicDataProvider.KIPRIS_FTO);
        when(connector.usedFor()).thenReturn("fto");
        when(connector.requestKey(any(PublicDataRequest.class))).thenReturn("KIPRIS_FTO:DE:Battery pack");
        when(connector.collect(any(PublicDataRequest.class))).thenReturn(PublicDataConnectorResult.builder()
                .provider(PublicDataProvider.KIPRIS_FTO)
                .endpoint("https://api.example.test/kipris")
                .requestKey("KIPRIS_FTO:DE:Battery pack")
                .hsCode("850760")
                .targetCountry("DE")
                .matchedKey("hsCode=850760;country=DE")
                .usedFor("fto")
                .rawPayload("{\"items\":[]}")
                .normalizedPayload(Map.of(
                        "rawCount", 0,
                        "normalizedCount", 0,
                        "zeroReason", "API_KEY_NOT_CONFIGURED",
                        "collectionEvidence", Map.of(
                                "operationEndpoint", true,
                                "apiKeyConfigured", false,
                                "endpointType", "operation")))
                .scoreContribution(7)
                .confidence(0.0)
                .sourceType("empty")
                .fallbackUsed(false)
                .errorMessage("API_KEY_NOT_CONFIGURED")
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
                eq(PublicDataProvider.KIPRIS_FTO), eq("KIPRIS_FTO:DE:Battery pack"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        PublicDataCollectionService service = service(
                List.of(connector), runRepository, recordRepository, userRepository, membershipRepository);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "cacheTtlHours", 24L);

        PublicDataCollectionResult result = service.collectForDiagnosis(
                "diagnosis-1", "user-1", "850760", "DE", "Battery pack");

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getSourceQualityStatus()).isEqualTo("EMPTY_NOT_CONFIGURED");
        assertThat(result.getRecords().get(0).getScoreContribution()).isLessThanOrEqualTo(0);
    }

    @Test
    void collectForDiagnosisCountsCsvFileRecordAsSuccessfulPublicDataRecord() {
        PublicDataConnector connector = mock(PublicDataConnector.class);
        when(connector.provider()).thenReturn(PublicDataProvider.MOTIE_IMPORT_REGULATION);
        when(connector.usedFor()).thenReturn("regulation");
        when(connector.requestKey(any(PublicDataRequest.class))).thenReturn("MOTIE_IMPORT_REGULATION:미국:강관");
        when(connector.collect(any(PublicDataRequest.class))).thenReturn(PublicDataConnectorResult.builder()
                .provider(PublicDataProvider.MOTIE_IMPORT_REGULATION)
                .endpoint("csv:classpath:publicdata/motie-import-regulations-20260508.csv")
                .requestKey("MOTIE_IMPORT_REGULATION:미국:강관")
                .hsCode("730630")
                .targetCountry("US")
                .matchedKey("hsCode=730630;country=US")
                .usedFor("regulation")
                .rawPayload("{\"source\":\"csv\"}")
                .normalizedPayload(Map.of(
                        "collectionEvidence", Map.of("operationEndpoint", false),
                        "metrics", Map.of("recordCount", 235)))
                .scoreContribution(-3)
                .confidence(0.72)
                .sourceType("cache")
                .fallbackUsed(false)
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
                eq(PublicDataProvider.MOTIE_IMPORT_REGULATION), eq("MOTIE_IMPORT_REGULATION:미국:강관"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        PublicDataCollectionService service = service(
                List.of(connector), runRepository, recordRepository, userRepository, membershipRepository);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "cacheTtlHours", 24L);

        PublicDataCollectionResult result = service.collectForDiagnosis(
                "diagnosis-1", "user-1", "730630", "US", "강관");

        assertThat(result.getRun().getStatus()).isEqualTo(PublicDataCollectionStatus.done);
        assertThat(result.getRun().getSuccessCount()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getSourceType()).isEqualTo("cache");
        assertThat(result.getRecords().get(0).getSourceQualityStatus()).isEqualTo("CACHE_VERIFIED");
    }

    @Test
    void collectForDiagnosisSkipsInactiveProviders() {
        PublicDataConnector inactive = mock(PublicDataConnector.class);
        when(inactive.provider()).thenReturn(PublicDataProvider.BIZINFO_SUPPORT);

        PublicDataConnector active = mock(PublicDataConnector.class);
        when(active.provider()).thenReturn(PublicDataProvider.KCS_TRADE);
        when(active.usedFor()).thenReturn("market");
        when(active.requestKey(any(PublicDataRequest.class))).thenReturn("KCS_TRADE:850760:DE");
        when(active.collect(any(PublicDataRequest.class))).thenReturn(PublicDataConnectorResult.builder()
                .provider(PublicDataProvider.KCS_TRADE)
                .endpoint("https://api.example.test/kcs")
                .requestKey("KCS_TRADE:850760:DE")
                .hsCode("850760")
                .targetCountry("DE")
                .matchedKey("hsCode=850760;country=DE")
                .usedFor("market")
                .rawPayload("{\"items\":[{\"hsCode\":\"850760\"}]}")
                .normalizedPayload(Map.of(
                        "collectionEvidence", Map.of("operationEndpoint", true, "apiKeyConfigured", true),
                        "metrics", Map.of("recordCount", 1)))
                .scoreContribution(3)
                .confidence(0.70)
                .sourceType("live")
                .fallbackUsed(false)
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
                eq(PublicDataProvider.KCS_TRADE), eq("KCS_TRADE:850760:DE"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        PublicDataCollectionService service = service(
                List.of(inactive, active), runRepository, recordRepository, userRepository, membershipRepository);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "cacheTtlHours", 24L);

        PublicDataCollectionResult result = service.collectForDiagnosis(
                "diagnosis-1", "user-1", "850760", "DE", "Battery pack");

        assertThat(result.getRun().getProviderCount()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getProvider()).isEqualTo(PublicDataProvider.KCS_TRADE);
        verify(inactive, never()).requestKey(any(PublicDataRequest.class));
        verify(inactive, never()).collect(any(PublicDataRequest.class));
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
