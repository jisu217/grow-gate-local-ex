package com.growgate.knowledge.repository;

import com.growgate.knowledge.entity.BuyerRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuyerRequirementRepository extends JpaRepository<BuyerRequirement, String> {

    List<BuyerRequirement> findByProductId(String productId);
}
