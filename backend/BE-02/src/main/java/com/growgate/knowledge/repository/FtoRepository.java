package com.growgate.knowledge.repository;

import com.growgate.knowledge.entity.Fto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FtoRepository extends JpaRepository<Fto, String> {

    List<Fto> findByProductId(String productId);
}
