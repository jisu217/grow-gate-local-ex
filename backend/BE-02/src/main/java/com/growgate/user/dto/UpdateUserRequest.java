package com.growgate.user.dto;

// 회원 정보 수정 요청 DTO
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateUserRequest {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;
}
