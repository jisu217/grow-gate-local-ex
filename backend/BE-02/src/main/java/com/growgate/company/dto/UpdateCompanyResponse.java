package com.growgate.company.dto;

// 회사 정보 수정 응답 DTO
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateCompanyResponse {

    private String companyId;
    private String industry;
    private String updatedAt;
}
