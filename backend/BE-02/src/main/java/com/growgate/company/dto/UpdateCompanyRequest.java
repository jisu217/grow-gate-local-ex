package com.growgate.company.dto;

// 회사 정보 수정 요청 DTO
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateCompanyRequest {

    private String industry;
}
