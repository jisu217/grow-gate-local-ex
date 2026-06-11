package com.growgate.user.dto;

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
