package com.growgate.regional.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "regional_industry_profiles")
public class RegionalIndustryProfile {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "region_code", nullable = false, length = 60)
    private String regionCode;

    @Column(name = "region_name", nullable = false, length = 100)
    private String regionName;

    @Column(nullable = false, length = 120)
    private String industry;

    @Column(name = "cluster_keywords", columnDefinition = "TEXT")
    private String clusterKeywords;

    @Column(name = "support_orgs", columnDefinition = "TEXT")
    private String supportOrgs;

    @Column(name = "public_source_name", length = 200)
    private String publicSourceName;

    @Column(name = "source_period", length = 80)
    private String sourcePeriod;

    @Column(name = "refresh_cycle_hours", nullable = false)
    private Integer refreshCycleHours;

    @Column(nullable = false)
    private Boolean synthetic;
}
