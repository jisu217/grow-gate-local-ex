package com.growgate.knowledge.repository;

// 바이어 요구사항 DB 조회
import com.growgate.knowledge.entity.BuyerRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuyerRequirementRepository extends JpaRepository<BuyerRequirement, String> {

    List<BuyerRequirement> findByProductId(String productId);

    Optional<BuyerRequirement> findByIdAndProductId(String id, String productId);
}
