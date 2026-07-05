package com.growgate.diagnosis.service;

import com.growgate.diagnosis.entity.DiagnosisRun;

public final class SyntheticDemoAccess {

    private static final String DEMO_WATERMARK = "synthetic_demo_not_real_customer";

    private SyntheticDemoAccess() {
    }

    public static boolean isSyntheticDemoRun(DiagnosisRun run) {
        if (run == null) {
            return false;
        }
        return containsWatermark(run.getResult())
                || containsWatermark(run.getInputData())
                || containsWatermark(run.getKnowledgeSnapshot());
    }

    private static boolean containsWatermark(String value) {
        return value != null && value.contains(DEMO_WATERMARK);
    }
}
