package com.growgate.knowledge.entity;

// 바이어 요구사항 엔티티
import com.growgate.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "buyer_requirements")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyerRequirement {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "buyer_name", nullable = false, length = 200)
    private String buyerName;

    @Column(length = 100)
    private String country;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public void update(String buyerName, String country, String requirements) {
        if (buyerName != null) this.buyerName = buyerName;
        if (country != null) this.country = country;
        if (requirements != null) this.requirements = requirements;
    }
}
