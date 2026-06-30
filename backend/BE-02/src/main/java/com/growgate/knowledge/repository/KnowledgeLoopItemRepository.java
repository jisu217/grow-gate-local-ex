package com.growgate.knowledge.repository;

import com.growgate.knowledge.entity.KnowledgeLoopItem;
import com.growgate.knowledge.enums.KnowledgeLoopStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeLoopItemRepository extends JpaRepository<KnowledgeLoopItem, String> {
    List<KnowledgeLoopItem> findTop20ByProductIdOrderByCreatedAtDesc(String productId);

    List<KnowledgeLoopItem> findTop20ByProductIdAndStatusOrderByCreatedAtDesc(String productId, KnowledgeLoopStatus status);

    Optional<KnowledgeLoopItem> findByIdAndProductId(String id, String productId);
}
