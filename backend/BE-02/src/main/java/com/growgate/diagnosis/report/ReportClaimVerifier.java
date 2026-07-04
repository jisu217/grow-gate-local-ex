package com.growgate.diagnosis.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportClaimVerifier {

    public List<ClaimVerification> verify(String markdown, List<String> sourceIds) {
        List<ClaimVerification> results = new ArrayList<>();
        String text = markdown == null ? "" : markdown;
        boolean hasSource = sourceIds != null && !sourceIds.isEmpty();

        if (!hasSource) {
            results.add(new ClaimVerification(
                    "WARN",
                    "source_trace_missing",
                    "보고서에 연결 가능한 sourceId가 없어 담당자 검토가 필요합니다."
            ));
        } else {
            results.add(new ClaimVerification(
                    "PASS",
                    "source_trace_present",
                    "보고서 appendix에 sourceId가 연결되어 있습니다."
            ));
        }

        String lower = text.toLowerCase(Locale.ROOT);
        if (text.contains("FTO 문제 없음") || text.contains("완전 분석") || lower.contains("automatic pdf")) {
            results.add(new ClaimVerification(
                    "BLOCK",
                    "overclaim_detected",
                    "확정적 FTO/완전 분석/자동 PDF 주장은 공식 Markdown 보고서에서 사용할 수 없습니다."
            ));
        }

        return results;
    }

    public record ClaimVerification(String status, String code, String message) {
    }
}
