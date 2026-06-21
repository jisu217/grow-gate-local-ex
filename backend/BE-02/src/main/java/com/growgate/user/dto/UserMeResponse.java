package com.growgate.user.dto;

// 내 정보 조회 응답 DTO
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserMeResponse {

    private String userId;
    private String email;
    private String name;
    private String userType;
    private String role;
    private String companyId;
    private String companyName;
    private String createdAt;
}
