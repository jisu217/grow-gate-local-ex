package com.growgate.publicdata.enums;

public enum PublicDataProvider {
    KCS_TRADE("관세청", "기획재정부/관세청", "OTHER_GOV", "market", 24),
    KOTRA_MARKET("KOTRA", "산업통상자원부 산하", "MOTIE_AFFILIATED", "market", 24),
    SAFETY_RECALL("국가기술표준원/SafetyKorea", "산업통상자원부", "MOTIE", "recall", 24),
    KIPRIS_FTO("특허청/KIPRIS", "특허청", "OTHER_GOV", "fto", 72),
    BIZINFO_SUPPORT("기업마당/수출지원", "중소벤처기업부 등", "OTHER_GOV", "support", 24),
    MOTIE_WEB_CRAWL("산업통상자원부 공개 웹", "산업통상자원부", "MOTIE", "regulation", 24);

    private final String agency;
    private final String ministry;
    private final String dataFamily;
    private final String usedFor;
    private final int refreshCycleHours;

    PublicDataProvider(String agency, String ministry, String dataFamily, String usedFor, int refreshCycleHours) {
        this.agency = agency;
        this.ministry = ministry;
        this.dataFamily = dataFamily;
        this.usedFor = usedFor;
        this.refreshCycleHours = refreshCycleHours;
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

    public boolean motieRelated() {
        return dataFamily.startsWith("MOTIE");
    }
}
