package com.growgate.knowledge.repository;

// 지식 출처 DB 조회
import com.growgate.knowledge.entity.KnowledgeSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSource, String> {

    List<KnowledgeSource> findByKnowledgeId(String knowledgeId);

    Optional<KnowledgeSource> findByIdAndKnowledgeId(String id, String knowledgeId);
}
