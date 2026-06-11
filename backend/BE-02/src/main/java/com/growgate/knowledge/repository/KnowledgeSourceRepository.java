package com.growgate.knowledge.repository;

import com.growgate.knowledge.entity.KnowledgeSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSource, String> {

    List<KnowledgeSource> findByKnowledgeId(String knowledgeId);
}
