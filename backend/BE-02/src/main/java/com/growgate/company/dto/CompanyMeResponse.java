package com.growgate.company.dto;

// 내 회사 정보 조회 응답 DTO
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CompanyMeResponse {

    private String companyId;
    private String name;
    private String businessNumber;
    private String industry;
    private String createdAt;
}
