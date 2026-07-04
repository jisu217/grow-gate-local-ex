package com.growgate.regional.service;

import com.growgate.regional.entity.RegionalIndustryProfile;
import com.growgate.regional.repository.RegionalIndustryProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegionalIndustryProfileService {

    private final RegionalIndustryProfileRepository repository;

    public Map<String, Object> resolve(String region, String industry, String hsCode, String productName) {
        String safeRegion = firstNonBlank(region, "지역 미입력");
        String safeIndustry = firstNonBlank(industry, "제조");
        String searchText = normalize(safeRegion + " " + safeIndustry + " " + hsCode + " " + productName);

        RegionalIndustryProfile best = null;
        int bestScore = 0;
        for (RegionalIndustryProfile candidate : repository.findAllByOrderByRegionNameAscIndustryAsc()) {
            int score = matchScore(candidate, searchText);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }

        if (best == null || bestScore < 3) {
            return fallbackProfile(safeRegion, safeIndustry);
        }
        return toProfileMap(best, safeRegion, safeIndustry, bestScore);
    }

    private int matchScore(RegionalIndustryProfile candidate, String text) {
        int score = 0;
        if (contains(text, candidate.getRegionName())) score += 5;
        if (contains(text, candidate.getRegionCode())) score += 3;
        if (contains(text, candidate.getIndustry())) score += 4;
        for (String keyword : split(candidate.getClusterKeywords())) {
            if (contains(text, keyword)) score += 1;
        }
        return score;
    }

    private Map<String, Object> toProfileMap(RegionalIndustryProfile profile, String inputRegion,
                                             String inputIndustry, int matchScore) {
        List<String> supportOrgs = split(profile.getSupportOrgs());
        List<String> keywords = split(profile.getClusterKeywords());
        int contribution = Math.min(8, Math.max(3, 3 + Math.min(5, matchScore / 2)));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("region", inputRegion);
        result.put("industry", inputIndustry);
        result.put("regionCode", profile.getRegionCode());
        result.put("regionName", profile.getRegionName());
        result.put("cluster", String.join(", ", keywords));
        result.put("leadingIndustry", profile.getIndustry());
        result.put("supportOrgs", supportOrgs);
        result.put("matchedSignals", List.of("regional_industry_profiles matchScore=" + matchScore));
        result.put("scoreContribution", contribution);
        result.put("confidence", Boolean.TRUE.equals(profile.getSynthetic()) ? 0.54 : 0.76);
        result.put("publicSourceName", profile.getPublicSourceName());
        result.put("sourcePeriod", profile.getSourcePeriod());
        result.put("refreshCycleHours", profile.getRefreshCycleHours());
        result.put("synthetic", Boolean.TRUE.equals(profile.getSynthetic()));
        result.put("warning", Boolean.TRUE.equals(profile.getSynthetic())
                ? "synthetic_demo_not_real_customer: 공식 지역산업 공공데이터로 교체 필요"
                : "");
        result.put("evidencePolicy", "지역/업종/HS 입력을 regional_industry_profiles dataset과 매칭합니다.");
        return result;
    }

    private Map<String, Object> fallbackProfile(String region, String industry) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("region", region);
        result.put("industry", industry);
        result.put("regionCode", "UNMATCHED");
        result.put("regionName", region);
        result.put("cluster", "지역 산업 매칭 미확인");
        result.put("leadingIndustry", "제조 일반");
        result.put("supportOrgs", List.of("지역테크노파크", "KOTRA 지방지원단", "중소벤처기업진흥공단 지역본부"));
        result.put("matchedSignals", List.of("controlled fallback: regional_industry_profiles match 없음"));
        result.put("scoreContribution", "지역 미입력".equals(region) ? -2 : 2);
        result.put("confidence", "지역 미입력".equals(region) ? 0.22 : 0.38);
        result.put("publicSourceName", "regional profile fallback");
        result.put("sourcePeriod", "not collected");
        result.put("refreshCycleHours", 720);
        result.put("synthetic", false);
        result.put("warning", "지역/업종 공공데이터 매칭 실패 - 담당자 확인 필요");
        result.put("evidencePolicy", "dataset miss는 live proof로 계산하지 않습니다.");
        return result;
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        String[] parts = value.split(",");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String cleaned = part.trim();
            if (!cleaned.isBlank()) result.add(cleaned);
        }
        return result;
    }

    private boolean contains(String text, String value) {
        if (value == null || value.isBlank()) return false;
        return text.contains(normalize(value));
    }

    private String normalize(String value) {
        return Normalizer.normalize(String.valueOf(value == null ? "" : value), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
