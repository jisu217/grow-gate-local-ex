package com.growgate.regional.repository;

import com.growgate.regional.entity.RegionalIndustryProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionalIndustryProfileRepository extends JpaRepository<RegionalIndustryProfile, String> {
    List<RegionalIndustryProfile> findAllByOrderByRegionNameAscIndustryAsc();
}
