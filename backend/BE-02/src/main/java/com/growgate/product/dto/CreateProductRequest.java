package com.growgate.product.dto;

// 제품 생성 요청 DTO
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "제품명은 필수입니다.")
    @Size(max = 200, message = "제품명은 200자 이하여야 합니다.")
    private String name;

    @Size(max = 100, message = "카테고리는 100자 이하여야 합니다.")
    private String category;

    @Pattern(regexp = "^$|^\\d{6,10}$", message = "HS Code는 숫자 6~10자리여야 합니다.")
    private String hsCode;

    @Size(max = 100, message = "대상 국가는 100자 이하여야 합니다.")
    private String targetCountry;

    @Size(max = 2000, message = "제품 설명은 2000자 이하여야 합니다.")
    private String description;
}
