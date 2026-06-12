package com.growgate.auth.service;

import com.growgate.auth.dto.LoginRequest;
import com.growgate.auth.dto.LoginResponse;
import com.growgate.auth.dto.SignupCompanyRequest;
import com.growgate.auth.dto.SignupPersonalRequest;
import com.growgate.auth.dto.SignupResponse;
import com.growgate.company.entity.Company;
import com.growgate.company.repository.CompanyRepository;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.security.JwtTokenProvider;
import com.growgate.user.entity.User;
import com.growgate.user.enums.UserRole;
import com.growgate.user.enums.UserType;
import com.growgate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.generateToken(user.getId());

        String role = user.getRole() != null ? user.getRole().name() : null;
        String companyId = user.getCompany() != null ? user.getCompany().getId() : null;

        return new LoginResponse(accessToken, user.getId(), user.getUserType().name(), role, companyId);
    }

    public SignupResponse signupPersonal(SignupPersonalRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .userType(UserType.personal)
                .role(UserRole.admin)
                .build();

        userRepository.save(user);

        return SignupResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType().name())
                .build();
    }

    public SignupResponse signupCompany(SignupCompanyRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        if (companyRepository.findByBusinessNumber(request.getBusinessNumber()).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 등록된 사업자번호입니다.");
        }

        Company company = Company.builder()
                .name(request.getCompanyName())
                .businessNumber(request.getBusinessNumber())
                .industry(request.getIndustry())
                .build();

        companyRepository.save(company);

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .userType(UserType.company)
                .role(UserRole.owner)
                .company(company)
                .build();

        userRepository.save(user);

        return SignupResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType().name())
                .companyId(company.getId())
                .build();
    }
}
