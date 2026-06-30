package com.growgate.publicdata.entity;

import com.growgate.publicdata.enums.PublicDataCollectionStatus;
import com.growgate.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "public_data_collection_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicDataCollectionRun {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "diagnosis_id", length = 36)
    private String diagnosisId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @Column(name = "hs_code", length = 20)
    private String hsCode;

    @Column(name = "target_country", length = 40)
    private String targetCountry;

    @Column(name = "product_name", length = 160)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PublicDataCollectionStatus status;

    @Column(name = "provider_count", nullable = false)
    private int providerCount;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "fallback_count", nullable = false)
    private int fallbackCount;

    @Column(name = "cache_hit_count", nullable = false)
    private int cacheHitCount;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PrePersist
    private void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.status == null) this.status = PublicDataCollectionStatus.requested;
        if (this.startedAt == null) this.startedAt = LocalDateTime.now();
    }
}
