package com.growgate.knowledge.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class KnowledgeLogsResponse {

    private String productId;
    private List<LogEntry> logs;

    @Getter
    @Builder
    public static class LogEntry {
        private String logId;
        private String changedBy;
        private String changeType;
        private List<String> changedFields;
        private String previousValue;
        private String newValue;
        private String reason;
        private String changedAt;
    }
}
