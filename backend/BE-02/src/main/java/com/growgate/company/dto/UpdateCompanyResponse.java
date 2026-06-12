package com.growgate.company.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateCompanyResponse {

    private String companyId;
    private String industry;
    private String updatedAt;
}
