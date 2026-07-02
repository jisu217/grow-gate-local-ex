package com.growgate.auth.dto;

// 회원가입 요청 DTO (개인/기업 통합)
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "가입 유형은 필수입니다.")
    @Pattern(regexp = "personal|company", message = "가입 유형은 personal 또는 company만 허용됩니다.")
    private String type; // "personal" | "company"

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    // 기업 가입 시 필수
    private String companyName;
    private String businessNumber;
    private String industry;

    @AssertTrue(message = "서비스 이용약관 동의는 필수입니다.")
    private boolean termsAccepted;

    @AssertTrue(message = "개인정보 수집·이용 동의는 필수입니다.")
    private boolean privacyAccepted;

    private boolean marketingAccepted;

    private String consentPolicyVersion;
}
