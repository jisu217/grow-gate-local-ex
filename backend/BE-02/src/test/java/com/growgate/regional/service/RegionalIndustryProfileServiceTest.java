package com.growgate.regional.service;

import com.growgate.regional.entity.RegionalIndustryProfile;
import com.growgate.regional.repository.RegionalIndustryProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegionalIndustryProfileServiceTest {

    @Test
    void resolvesSyntheticRegionalProfilesForThreeJudgeDemoCases() {
        RegionalIndustryProfileRepository repository = mock(RegionalIndustryProfileRepository.class);
        when(repository.findAllByOrderByRegionNameAscIndustryAsc()).thenReturn(List.of(
                profile("GN-CHANGWON", "경남 창원", "전기전자·이차전지 부품 제조",
                        "이차전지, 배터리팩, 전장부품, UN38.3, CE, RoHS, REACH",
                        "경남테크노파크, 한국전기연구원, KOTRA 경남지원단, 창원산업진흥원"),
                profile("BS-GANGSEO", "부산 강서", "조선기자재·스마트 밸브 액추에이터",
                        "조선기자재, 스마트밸브, 액추에이터, IP67, 선급, UL, FCC",
                        "부산테크노파크, 한국조선해양기자재연구원, KOTRA 부산지원단, 부산경제진흥원"),
                profile("DG-SUSEONG", "대구 수성", "디지털헬스·웨어러블 센서 제조",
                        "디지털헬스, 웨어러블, 의료기기, ISO13485, PMDA, BLE, 개인정보",
                        "대구테크노파크, 대구경북첨단의료산업진흥재단, KOTRA 대구경북지원단, 한국의료기기안전정보원")
        ));
        RegionalIndustryProfileService service = new RegionalIndustryProfileService(repository);

        var battery = service.resolve("경남 창원", "전기전자·이차전지 부품 제조", "850760", "산업용 리튬이온 배터리팩");
        var marine = service.resolve("부산 강서", "조선기자재·스마트 밸브 액추에이터", "848180", "선박용 스마트 전동밸브 액추에이터");
        var medical = service.resolve("대구 수성", "디지털헬스·웨어러블 센서 제조", "902519", "웨어러블 체온 모니터");

        assertThat(battery.get("regionCode")).isEqualTo("GN-CHANGWON");
        assertThat(supportOrgs(battery)).contains("경남테크노파크", "KOTRA 경남지원단");
        assertThat(marine.get("regionCode")).isEqualTo("BS-GANGSEO");
        assertThat(supportOrgs(marine)).contains("한국조선해양기자재연구원");
        assertThat(medical.get("regionCode")).isEqualTo("DG-SUSEONG");
        assertThat(supportOrgs(medical)).contains("대구경북첨단의료산업진흥재단");
        assertThat(battery.get("warning")).asString().contains("synthetic_demo_not_real_customer");
    }

    @SuppressWarnings("unchecked")
    private List<String> supportOrgs(Map<String, Object> profile) {
        return (List<String>) profile.get("supportOrgs");
    }

    private RegionalIndustryProfile profile(String regionCode, String regionName, String industry,
                                            String keywords, String supportOrgs) {
        RegionalIndustryProfile profile = new RegionalIndustryProfile();
        ReflectionTestUtils.setField(profile, "id", regionCode);
        ReflectionTestUtils.setField(profile, "regionCode", regionCode);
        ReflectionTestUtils.setField(profile, "regionName", regionName);
        ReflectionTestUtils.setField(profile, "industry", industry);
        ReflectionTestUtils.setField(profile, "clusterKeywords", keywords);
        ReflectionTestUtils.setField(profile, "supportOrgs", supportOrgs);
        ReflectionTestUtils.setField(profile, "publicSourceName",
                "synthetic regional profile seed - replace with official regional/industrial public dataset");
        ReflectionTestUtils.setField(profile, "sourcePeriod", "2026 synthetic seed");
        ReflectionTestUtils.setField(profile, "refreshCycleHours", 720);
        ReflectionTestUtils.setField(profile, "synthetic", true);
        return profile;
    }
}
