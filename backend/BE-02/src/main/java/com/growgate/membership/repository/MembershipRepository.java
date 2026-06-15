package com.growgate.membership.repository;

import com.growgate.membership.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, String> {
    Optional<Membership> findFirstByUserId(String userId);
    List<Membership> findByUserId(String userId);
}
