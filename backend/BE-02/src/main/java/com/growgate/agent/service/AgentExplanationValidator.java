package com.growgate.agent.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentExplanationValidator {

    private static final Map<String, String> REPLACEMENTS = replacements();

    private static Map<String, String> replacements() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("모든 데이터가 live로 수집됨", "현재 확인 가능한 데이터 상태를 기준으로 판단했으며, 일부 항목은 cache 또는 fallback일 수 있습니다.");
        values.put("입력만으로 통관 가능 확정", "입력 정보를 바탕으로 통관 전 검토해야 할 항목을 정리했습니다.");
        values.put("시장성이 높으니 바로 수출 가능", "시장 신호는 긍정적일 수 있으나 인증·가격·물류·바이어 조건 검토가 함께 필요합니다.");
        values.put("CE 인증 가능 확정", "CE 적용 가능성과 필요 문서 확인이 필요합니다.");
        values.put("리콜 없음 = 안전함", "확인된 유사 리콜 사례가 제한적이더라도 제품안전 점검은 별도로 필요합니다.");
        values.put("특허 침해 없음", "공개 특허 기준의 위험 후보 탐색 결과이며 최종 FTO 검토가 필요합니다.");
        values.put("지원사업 선정 보장", "조건이 유사한 지원사업 후보이며 최종 선정은 기관 심사에 따릅니다.");
        values.put("모든 업무 자동 완료", "AI가 업무 초안을 정리했으며 담당자가 우선순위와 완료 여부를 확인해야 합니다.");
        values.put("법률·인증 최종 보고서", "담당자 검토를 돕기 위한 수출 준비도 검토 보고서 초안입니다.");
        values.put("AI가 자율 최종 판단", "AI는 담당자 검토를 돕는 근거를 정리합니다.");
        values.put("최종 판단", "담당자 검토 기준 판단 후보");
        return values;
    }

    public String sanitizeText(String value) {
        if (value == null || value.isBlank()) return "";
        String sanitized = LlmSecretSanitizer.sanitize(value);
        for (Map.Entry<String, String> entry : REPLACEMENTS.entrySet()) {
            sanitized = sanitized.replace(entry.getKey(), entry.getValue());
        }
        return sanitized;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> validateCandidate(Map<String, Object> candidate) {
        Map<String, Object> safe = sanitizeMap(candidate == null ? Map.of() : candidate);
        List<String> sourceIds = new ArrayList<>();
        Object rawSourceIds = safe.get("sourceIds");
        if (rawSourceIds instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    sourceIds.add(String.valueOf(item));
                }
            }
        }
        if (sourceIds.isEmpty()) {
            safe.put("status", "DATA_INSUFFICIENT");
            safe.put("reviewRequired", true);
            safe.put("sourceIds", List.of());
        } else {
            safe.putIfAbsent("status", "PENDING_REVIEW");
            safe.putIfAbsent("reviewRequired", true);
            safe.put("sourceIds", sourceIds);
        }
        return safe;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> sanitizeMap(Map<String, Object> source) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (source == null) return out;
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String text) {
                out.put(entry.getKey(), sanitizeText(text));
            } else if (value instanceof Map<?, ?> map) {
                out.put(entry.getKey(), sanitizeMap((Map<String, Object>) map));
            } else if (value instanceof List<?> list) {
                out.put(entry.getKey(), sanitizeList(list));
            } else {
                out.put(entry.getKey(), value);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<Object> sanitizeList(List<?> source) {
        List<Object> out = new ArrayList<>();
        for (Object value : source) {
            if (value instanceof String text) {
                out.add(sanitizeText(text));
            } else if (value instanceof Map<?, ?> map) {
                out.add(sanitizeMap((Map<String, Object>) map));
            } else if (value instanceof List<?> list) {
                out.add(sanitizeList(list));
            } else {
                out.add(value);
            }
        }
        return out;
    }
}
