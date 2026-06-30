package com.growgate.knowledge.repository;

// FTO DB 조회
import com.growgate.knowledge.entity.Fto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FtoRepository extends JpaRepository<Fto, String> {

    List<Fto> findByProductId(String productId);

    List<Fto> findByProductIdIn(List<String> productIds);

    Optional<Fto> findByIdAndProductId(String id, String productId);
}
