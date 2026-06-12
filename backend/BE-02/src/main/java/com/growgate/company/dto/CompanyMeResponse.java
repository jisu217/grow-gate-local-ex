package com.growgate.company.dto;

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
