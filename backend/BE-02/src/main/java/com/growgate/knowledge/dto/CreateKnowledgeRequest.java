package com.growgate.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateKnowledgeRequest {

    @NotBlank(message = "제품명은 필수입니다.")
    private String name;

    private String category;

    private String description;
}
