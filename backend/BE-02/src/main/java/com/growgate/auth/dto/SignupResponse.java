package com.growgate.auth.dto;

// 회원가입 응답 DTO
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupResponse {

    private String userId;
    private String email;
    private String name;
    private String userType;
    private String companyId;
}
