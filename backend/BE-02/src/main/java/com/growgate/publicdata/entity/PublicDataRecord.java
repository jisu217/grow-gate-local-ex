package com.growgate.publicdata.entity;

import com.growgate.publicdata.enums.PublicDataProvider;
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
@Table(name = "public_data_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicDataRecord {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id")
    private PublicDataCollectionRun run;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PublicDataProvider provider;

    @Column(name = "endpoint", length = 500)
    private String endpoint;

    @Column(name = "request_key", nullable = false, length = 240)
    private String requestKey;

    @Column(name = "hs_code", length = 20)
    private String hsCode;

    @Column(name = "target_country", length = 40)
    private String targetCountry;

    @Column(name = "matched_key", length = 500)
    private String matchedKey;

    @Column(name = "used_for", length = 60)
    private String usedFor;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "normalized_payload", columnDefinition = "TEXT")
    private String normalizedPayload;

    @Column(name = "score_contribution")
    private Integer scoreContribution;

    @Column(name = "fallback_used", nullable = false)
    private boolean fallbackUsed;

    @Column(name = "cache_hit", nullable = false)
    private boolean cacheHit;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    private void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.fetchedAt == null) this.fetchedAt = LocalDateTime.now();
    }
}
