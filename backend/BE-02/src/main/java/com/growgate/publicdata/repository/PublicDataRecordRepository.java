package com.growgate.publicdata.repository;

import com.growgate.publicdata.entity.PublicDataRecord;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicDataRecordRepository extends JpaRepository<PublicDataRecord, String> {

    Optional<PublicDataRecord> findTopByProviderAndRequestKeyAndExpiresAtAfterOrderByFetchedAtDesc(
            PublicDataProvider provider,
            String requestKey,
            LocalDateTime now
    );

    List<PublicDataRecord> findTop50ByHsCodeAndTargetCountryOrderByFetchedAtDesc(String hsCode, String targetCountry);

    List<PublicDataRecord> findTop50ByOrderByFetchedAtDesc();

    List<PublicDataRecord> findTop50ByRun_RequestedBy_IdInOrderByFetchedAtDesc(List<String> userIds);

    List<PublicDataRecord> findTop50ByRun_RequestedBy_IdInAndHsCodeAndTargetCountryOrderByFetchedAtDesc(
            List<String> userIds,
            String hsCode,
            String targetCountry
    );
}
