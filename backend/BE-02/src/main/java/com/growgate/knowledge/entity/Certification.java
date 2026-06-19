package com.growgate.knowledge.entity;

// 인증서 엔티티
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "certifications")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certification {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "issued_at")
    private LocalDate issuedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "renewal_cycle")
    private Integer renewalCycle;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, LocalDate issuedAt, LocalDate expiresAt, Integer renewalCycle) {
        if (name != null) this.name = name;
        if (issuedAt != null) this.issuedAt = issuedAt;
        if (expiresAt != null) this.expiresAt = expiresAt;
        if (renewalCycle != null) this.renewalCycle = renewalCycle;
    }
}
