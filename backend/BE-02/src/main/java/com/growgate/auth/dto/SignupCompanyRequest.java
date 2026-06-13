package com.growgate.auth.dto;

// 기업 회원가입 요청 DTO
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupCompanyRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @NotBlank(message = "담당자명은 필수입니다.")
    private String name;

    @NotBlank(message = "기업명은 필수입니다.")
    private String companyName;

    @NotBlank(message = "사업자번호는 필수입니다.")
    private String businessNumber;

    private String industry;
}
