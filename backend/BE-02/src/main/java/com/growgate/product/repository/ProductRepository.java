package com.growgate.product.repository;

import com.growgate.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByOwnerIdOrderByUpdatedAtDesc(String ownerId);

    Optional<Product> findByIdAndOwnerId(String id, String ownerId);

    List<Product> findByOwner_Company_IdOrderByUpdatedAtDesc(String companyId);

    Optional<Product> findByIdAndOwner_Company_Id(String id, String companyId);
}
