package com.growgate.knowledge.dto;

import com.growgate.knowledge.entity.KnowledgeLoopItem;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class KnowledgeLoopItemsResponse {
    private String productId;
    private List<Item> items;

    @Getter
    @Builder
    public static class Item {
        private String id;
        private String diagnosisId;
        private String type;
        private String title;
        private String evidence;
        private String rationale;
        private String impact;
        private String status;
        private String createdAt;
        private String decidedAt;
    }

    public static Item from(KnowledgeLoopItem item) {
        return Item.builder()
                .id(item.getId())
                .diagnosisId(item.getDiagnosisId())
                .type(item.getType())
                .title(item.getTitle())
                .evidence(item.getEvidence())
                .rationale(item.getRationale())
                .impact(item.getImpact())
                .status(item.getStatus().name())
                .createdAt(item.getCreatedAt() != null ? item.getCreatedAt().toString() : null)
                .decidedAt(item.getDecidedAt() != null ? item.getDecidedAt().toString() : null)
                .build();
    }
}
