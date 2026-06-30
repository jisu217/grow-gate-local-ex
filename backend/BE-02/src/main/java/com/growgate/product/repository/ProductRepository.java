package com.growgate.product.repository;

// 상품 DB 조회
import com.growgate.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String ownerId);

    Optional<Product> findByIdAndOwnerIdAndDeletedAtIsNull(String id, String ownerId);

    List<Product> findByCompany_IdAndDeletedAtIsNullOrderByUpdatedAtDesc(String companyId);

    Optional<Product> findByIdAndCompany_IdAndDeletedAtIsNull(String id, String companyId);
}
