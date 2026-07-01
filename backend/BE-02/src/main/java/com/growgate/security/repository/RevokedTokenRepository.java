package com.growgate.security.repository;

import com.growgate.security.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    void deleteByExpiresAtBefore(LocalDateTime now);
}
