package com.growgate.product.controller;

// 제품 API 엔드포인트
import com.growgate.product.dto.CreateProductRequest;
import com.growgate.product.dto.CreateProductResponse;
import com.growgate.product.dto.DeleteProductResponse;
import com.growgate.product.dto.ProductListResponse;
import com.growgate.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ProductListResponse> getList(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(productService.getList(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<CreateProductResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(userDetails.getUsername(), request));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<DeleteProductResponse> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId) {
        return ResponseEntity.ok(productService.delete(userDetails.getUsername(), productId));
    }
}
