package com.growgate.auth.dto;

// 로그인 응답 DTO (accessToken, userId, role 등)
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String userId;
    private String userType;
    private String role;
    private String companyId;
}
