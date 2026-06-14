package com.growgate.company.repository;

// 회사 DB 조회
import com.growgate.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, String> {

    Optional<Company> findByBusinessNumber(String businessNumber);
}
