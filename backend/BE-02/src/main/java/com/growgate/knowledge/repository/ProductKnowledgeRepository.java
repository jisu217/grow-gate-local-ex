package com.growgate.knowledge.repository;

import com.growgate.knowledge.entity.ProductKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductKnowledgeRepository extends JpaRepository<ProductKnowledge, String> {

    Optional<ProductKnowledge> findByProductId(String productId);
}
