package com.growgate.knowledge.entity;

import com.growgate.knowledge.enums.KnowledgeLoopStatus;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "knowledge_loop_items")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeLoopItem {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "diagnosis_id", columnDefinition = "VARCHAR(36)")
    private String diagnosisId;

    @Column(name = "type", length = 60, nullable = false)
    private String type;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "rationale", columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "impact", columnDefinition = "TEXT")
    private String impact;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private KnowledgeLoopStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private User decidedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @PrePersist
    private void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.status == null) this.status = KnowledgeLoopStatus.pending;
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    public void accept(User user) {
        this.status = KnowledgeLoopStatus.accepted;
        this.decidedBy = user;
        this.decidedAt = LocalDateTime.now();
    }

    public void dismiss(User user) {
        this.status = KnowledgeLoopStatus.dismissed;
        this.decidedBy = user;
        this.decidedAt = LocalDateTime.now();
    }

    public void archive(User user) {
        this.status = KnowledgeLoopStatus.archived;
        this.decidedBy = user;
        this.decidedAt = LocalDateTime.now();
    }
}
