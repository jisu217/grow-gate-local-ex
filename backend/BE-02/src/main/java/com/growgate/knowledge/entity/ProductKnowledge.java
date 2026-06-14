package com.growgate.knowledge.entity;

// 제품 지식 통합 엔티티
import com.growgate.knowledge.enums.TrustGrade;
import com.growgate.product.entity.Product;
import com.growgate.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_knowledge")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductKnowledge {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", unique = true, nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_grade", length = 10)
    private TrustGrade trustGrade;

    @Column(name = "source_type", length = 100)
    private String sourceType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(TrustGrade trustGrade, String sourceType, String notes, User updatedBy) {
        if (trustGrade != null) this.trustGrade = trustGrade;
        if (sourceType != null) this.sourceType = sourceType;
        if (notes != null) this.notes = notes;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}
