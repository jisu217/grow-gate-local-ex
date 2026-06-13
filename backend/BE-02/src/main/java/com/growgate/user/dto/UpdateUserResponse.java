package com.growgate.user.dto;

// 회원 정보 수정 응답 DTO
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateUserResponse {

    private String userId;
    private String name;
    private String updatedAt;
}
