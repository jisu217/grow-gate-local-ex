package com.growgate.product.dto;

// 제품 삭제 응답 DTO
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeleteProductResponse {

    private String message;
    private String productId;
}
