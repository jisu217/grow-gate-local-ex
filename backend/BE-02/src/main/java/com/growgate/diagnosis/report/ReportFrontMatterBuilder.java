package com.growgate.diagnosis.report;

import java.util.List;

public class ReportFrontMatterBuilder {

    public String build(MarkdownReportRenderer.ReportContext context) {
        return "---\n"
                + "reportId: rpt_" + safe(context.getDiagnosisId(), "unknown") + "\n"
                + "diagnosisId: " + safe(context.getDiagnosisId(), "unknown") + "\n"
                + "publicDataProofRunId: " + safe(context.getPublicDataProofRunId(), "not-available") + "\n"
                + "modelVersion: " + safe(context.getModelVersion(), MarkdownReportRenderer.DEFAULT_MODEL_VERSION) + "\n"
                + "reportFormat: markdown\n"
                + "pdfStatus: optional_browser_print_or_ci_conversion\n"
                + "limitation:\n"
                + yamlList(List.of(
                        "본 보고서는 법률·인증 담당자 의사결정을 대신하지 않는 견적 전 risk screening 자료입니다.",
                        "LLM 보조 문장은 source trace와 claim verification을 통과한 설명에 한정합니다.",
                        "synthetic persona validation은 실제 고객 인터뷰가 아닙니다."
                ))
                + "---\n\n";
    }

    private String yamlList(List<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            out.append("  - ").append(value.replace("\n", " ")).append("\n");
        }
        return out.toString();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.replace("\n", " ");
    }
}
