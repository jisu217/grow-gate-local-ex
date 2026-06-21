package com.growgate.product.dto;

// 제품 생성 응답 DTO
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateProductResponse {

    private String productId;
    private String name;
    private String createdAt;
}
