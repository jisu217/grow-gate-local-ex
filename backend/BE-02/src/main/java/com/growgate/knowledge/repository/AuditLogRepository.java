package com.growgate.knowledge.repository;

// 감사 로그 DB 조회
import com.growgate.knowledge.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByProductIdOrderByChangedAtDesc(String productId);

    Page<AuditLog> findByProductId(String productId, Pageable pageable);
}
