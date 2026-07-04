package com.growgate.publicdata.enums;

public enum PublicDataProvider {
    KCS_TRADE("관세청", "기획재정부/관세청", "OTHER_GOV", "market", 24,
            "수출입무역통계",
            "https://www.data.go.kr/en/data/15102108/openapi.do",
            true, true,
            "공공데이터포털 개별 데이터셋 이용허락범위 확인 필요",
            "https://www.data.go.kr/ugs/selectPortalPolicyView.do",
            "unknown", "unknown", "unknown",
            "unknown",
            "공식 이용조건 확인 전 원문 링크 중심으로 표시하고 재배포하지 않습니다.",
            "2026-07-04"),
    KOTRA_MARKET("KOTRA", "산업통상자원부 산하", "MOTIE_AFFILIATED", "market", 24,
            "대한무역투자진흥공사_글로벌 공급망 인사이트",
            "https://www.data.go.kr/data/15122648/openapi.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15122648/openapi.do",
            "yes", "yes", "yes",
            "unknown",
            "출처와 sourceType을 함께 표시하고, operation endpoint/key 증거가 없으면 live 공공 API로 표시하지 않습니다.",
            "2026-07-04"),
    SAFETY_RECALL("국가기술표준원/SafetyKorea", "산업통상자원부", "MOTIE", "recall", 24,
            "산업통상부 국가기술표준원_제품 안전인증 및 리콜 정보",
            "https://www.data.go.kr/data/15116894/openapi.do",
            true, false,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15116894/openapi.do",
            "yes", "yes", "yes",
            "unknown",
            "SafetyKorea 원문 링크와 조회일을 유지하고 인증/리콜 최종 판단은 담당자 확인으로 제한합니다.",
            "2026-07-04"),
    KIPRIS_FTO("특허청/KIPRIS", "특허청", "OTHER_GOV", "fto", 72,
            "KIPRIS Plus Open API",
            "https://plus.kipris.or.kr/eng/main/contents.do?menuNo=300024",
            true, true,
            "KIPRIS Plus Terms of use 확인 필요",
            "https://plus.kipris.or.kr/eng/main/contents.do?menuNo=300030",
            "conditional", "conditional", "unknown",
            "unknown",
            "특허/상표 공개자료 기반 사전 위험 신호로만 표시하고, FTO 법률 판단 또는 침해 판단으로 표현하지 않습니다.",
            "2026-07-04"),
    BIZINFO_SUPPORT("기업마당/수출지원", "중소벤처기업부 등", "OTHER_GOV", "support", 24,
            "기업마당 지원사업 공고",
            "https://www.bizinfo.go.kr/web/index.do",
            true, true,
            "기업마당 저작권 정책/이용약관 확인 필요",
            "https://www.bizinfo.go.kr/biz/bizd/selectBIZD120View.do",
            "conditional", "conditional", "conditional",
            "yes",
            "원문 링크 제공을 우선하고, 공식 이용조건 확인 전 지원사업 콘텐츠의 재가공/재배포를 제한합니다.",
            "2026-07-04"),
    MOTIE_WEB_CRAWL("산업통상자원부 공개 웹", "산업통상자원부", "MOTIE", "regulation", 24,
            "산업통상자원부 공개 웹 보조근거",
            "https://www.motie.go.kr/",
            false, false,
            "사이트별 이용조건 확인 필요",
            "",
            "unknown", "unknown", "unknown",
            "unknown",
            "공식 operation API proof가 아닌 crawl 보조근거입니다. 원문 링크 확인 전 재배포하지 않습니다.",
            "2026-07-04"),
    EXPORT_EVIDENCE_FEED("수출 뉴스·보고서 feed", "공개 RSS/API", "EXTERNAL_EVIDENCE", "market", 12,
            "수출 뉴스·보고서 feed",
            "",
            false, false,
            "feed 제공자별 이용조건 확인 필요",
            "",
            "unknown", "unknown", "unknown",
            "unknown",
            "공식 공공 API proof가 아닌 external_feed 보조근거입니다.",
            "2026-07-04");

    private final String agency;
    private final String ministry;
    private final String dataFamily;
    private final String usedFor;
    private final int refreshCycleHours;
    private final String sourceName;
    private final String officialUrl;
    private final boolean operationEndpoint;
    private final boolean apiKeyRequired;
    private final String licenseName;
    private final String termsUrl;
    private final String redistributionAllowed;
    private final String storageAllowed;
    private final String reprocessingAllowed;
    private final String attributionRequired;
    private final String usageWarning;
    private final String lastVerifiedAt;

    PublicDataProvider(String agency, String ministry, String dataFamily, String usedFor, int refreshCycleHours,
                       String sourceName, String officialUrl, boolean operationEndpoint, boolean apiKeyRequired,
                       String licenseName, String termsUrl, String redistributionAllowed, String storageAllowed,
                       String reprocessingAllowed, String attributionRequired, String usageWarning,
                       String lastVerifiedAt) {
        this.agency = agency;
        this.ministry = ministry;
        this.dataFamily = dataFamily;
        this.usedFor = usedFor;
        this.refreshCycleHours = refreshCycleHours;
        this.sourceName = sourceName;
        this.officialUrl = officialUrl;
        this.operationEndpoint = operationEndpoint;
        this.apiKeyRequired = apiKeyRequired;
        this.licenseName = licenseName;
        this.termsUrl = termsUrl;
        this.redistributionAllowed = redistributionAllowed;
        this.storageAllowed = storageAllowed;
        this.reprocessingAllowed = reprocessingAllowed;
        this.attributionRequired = attributionRequired;
        this.usageWarning = usageWarning;
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public String agency() {
        return agency;
    }

    public String ministry() {
        return ministry;
    }

    public String dataFamily() {
        return dataFamily;
    }

    public String usedFor() {
        return usedFor;
    }

    public int refreshCycleHours() {
        return refreshCycleHours;
    }

    public String sourceName() {
        return sourceName;
    }

    public String officialUrl() {
        return officialUrl;
    }

    public boolean operationEndpoint() {
        return operationEndpoint;
    }

    public boolean apiKeyRequired() {
        return apiKeyRequired;
    }

    public String licenseName() {
        return licenseName;
    }

    public String termsUrl() {
        return termsUrl;
    }

    public String redistributionAllowed() {
        return redistributionAllowed;
    }

    public String storageAllowed() {
        return storageAllowed;
    }

    public String reprocessingAllowed() {
        return reprocessingAllowed;
    }

    public String attributionRequired() {
        return attributionRequired;
    }

    public String usageWarning() {
        return usageWarning;
    }

    public String lastVerifiedAt() {
        return lastVerifiedAt;
    }

    public boolean motieRelated() {
        return dataFamily.startsWith("MOTIE");
    }
}
