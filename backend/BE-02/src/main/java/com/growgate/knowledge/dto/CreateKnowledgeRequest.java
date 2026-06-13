package com.growgate.knowledge.dto;

// 제품 지식 생성 요청 DTO
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
