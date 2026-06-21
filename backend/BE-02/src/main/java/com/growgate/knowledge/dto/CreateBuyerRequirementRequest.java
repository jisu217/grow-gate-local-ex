package com.growgate.knowledge.dto;

// 바이어 요구사항 등록 요청 DTO
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateBuyerRequirementRequest {

    @NotBlank(message = "바이어명은 필수입니다.")
    private String buyerName;

    private String country;

    @NotBlank(message = "요구사항 내용은 필수입니다.")
    private String requirements;

    private String reason;
}
