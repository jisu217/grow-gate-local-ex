package com.growgate.diagnosis.report;

public class OptionalHtmlReportRenderer {

    public String render(String markdown) {
        String escaped = escape(markdown == null ? "" : markdown);
        return "<!doctype html><html lang=\"ko\"><head><meta charset=\"utf-8\">"
                + "<title>GROW-GATE Markdown Report Preview</title>"
                + "<style>body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;line-height:1.65;max-width:860px;margin:40px auto;padding:0 24px;color:#17213a}"
                + "pre{white-space:pre-wrap;background:#f6f8fb;border:1px solid #e4e8f0;border-radius:8px;padding:16px}"
                + ".note{font-size:13px;color:#61708a;margin-bottom:16px}</style></head><body>"
                + "<div class=\"note\">공식 원본은 Markdown입니다. PDF가 필요하면 브라우저 인쇄 또는 CI 변환을 사용합니다.</div>"
                + "<pre>" + escaped + "</pre></body></html>";
    }

    private String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
