package com.growgate.knowledge.repository;

// 인증서 DB 조회
import com.growgate.knowledge.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, String> {

    List<Certification> findByProductId(String productId);

    List<Certification> findByProductIdIn(List<String> productIds);

    Optional<Certification> findByIdAndProductId(String id, String productId);
}
