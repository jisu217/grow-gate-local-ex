package com.growgate.publicdata.enums;

public enum SourceQualityStatus {
    LIVE_VERIFIED,
    CACHE_VERIFIED,
    EMPTY_TRUE_ZERO,
    EMPTY_NOT_CONFIGURED,
    FALLBACK_ERROR,
    SEED_SYNTHETIC,
    HEURISTIC_ONLY
}
