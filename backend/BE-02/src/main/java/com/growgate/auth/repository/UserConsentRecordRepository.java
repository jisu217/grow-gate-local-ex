package com.growgate.auth.repository;

import com.growgate.auth.entity.UserConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserConsentRecordRepository extends JpaRepository<UserConsentRecord, String> {
}
