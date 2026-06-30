package com.growgate.knowledge.repository;

// 제품 지식 DB 조회
import com.growgate.knowledge.entity.ProductKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductKnowledgeRepository extends JpaRepository<ProductKnowledge, String> {

    Optional<ProductKnowledge> findByProductId(String productId);

    List<ProductKnowledge> findByProductIdIn(List<String> productIds);
}
