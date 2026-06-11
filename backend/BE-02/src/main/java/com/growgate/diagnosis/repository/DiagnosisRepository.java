package com.growgate.diagnosis.repository;

import com.growgate.diagnosis.entity.DiagnosisRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosisRepository extends JpaRepository<DiagnosisRun, String> {

    List<DiagnosisRun> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<DiagnosisRun> findByIdAndUserId(String id, String userId);

    List<DiagnosisRun> findByCompanyIdOrderByCreatedAtDesc(String companyId);

    Optional<DiagnosisRun> findByIdAndCompanyId(String id, String companyId);
}
