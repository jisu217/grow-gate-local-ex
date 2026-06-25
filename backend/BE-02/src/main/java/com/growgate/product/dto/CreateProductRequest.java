package com.growgate.product.dto;

// 제품 생성 요청 DTO
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "제품명은 필수입니다.")
    private String name;

    private String category;

    private String hsCode;

    private String targetCountry;

    private String description;
}
