package com.growgate.publicdata.repository;

import com.growgate.publicdata.entity.PublicDataCollectionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicDataCollectionRunRepository extends JpaRepository<PublicDataCollectionRun, String> {

    List<PublicDataCollectionRun> findTop20ByOrderByStartedAtDesc();

    List<PublicDataCollectionRun> findTop20ByRequestedBy_IdInOrderByStartedAtDesc(List<String> userIds);
}
