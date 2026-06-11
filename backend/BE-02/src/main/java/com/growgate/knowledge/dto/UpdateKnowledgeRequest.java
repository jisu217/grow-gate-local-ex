package com.growgate.knowledge.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateKnowledgeRequest {

    private String name;
    private String category;
    private String description;
    private KnowledgeUpdateData knowledge;
    private String reason;

    @Getter
    @NoArgsConstructor
    public static class KnowledgeUpdateData {
        private String trustGrade;
        private String sourceType;
        private String notes;
    }
}
