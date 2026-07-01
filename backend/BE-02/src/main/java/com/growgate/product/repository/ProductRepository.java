package com.growgate.product.repository;

// 상품 DB 조회
import com.growgate.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String ownerId);

    Optional<Product> findByIdAndOwnerIdAndDeletedAtIsNull(String id, String ownerId);

    List<Product> findByCompany_IdAndDeletedAtIsNullOrderByUpdatedAtDesc(String companyId);

    Optional<Product> findByIdAndCompany_IdAndDeletedAtIsNull(String id, String companyId);

    @Query("""
            select p
            from Product p
            join fetch p.owner
            where p.deletedAt is null
              and p.hsCode is not null
              and p.hsCode <> ''
              and p.targetCountry is not null
              and p.targetCountry <> ''
            order by p.updatedAt desc
            """)
    List<Product> findDailyRefreshCandidates(Pageable pageable);

    @Query("""
            select p
            from Product p
            join fetch p.owner
            where p.owner.id = :ownerId
              and p.deletedAt is null
              and p.hsCode is not null
              and p.hsCode <> ''
              and p.targetCountry is not null
              and p.targetCountry <> ''
            order by p.updatedAt desc
            """)
    List<Product> findDailyRefreshCandidatesByOwnerId(@Param("ownerId") String ownerId, Pageable pageable);
}
