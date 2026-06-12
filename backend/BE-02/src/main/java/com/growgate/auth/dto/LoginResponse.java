package com.growgate.auth.dto;

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
