package com.growgate.auth.service;

import com.growgate.auth.dto.LoginRequest;
import com.growgate.auth.dto.LoginResponse;
import com.growgate.auth.dto.SignupRequest;
import com.growgate.auth.dto.SignupResponse;
import com.growgate.company.entity.Company;
import com.growgate.company.repository.CompanyRepository;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.membership.entity.Membership;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.security.JwtTokenProvider;
import com.growgate.security.TokenRevocationService;
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
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationService tokenRevocationService;
    private final LoginAttemptRateLimiter loginAttemptRateLimiter;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        loginAttemptRateLimiter.assertAllowed(request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    loginAttemptRateLimiter.recordFailure(request.getEmail());
                    return new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptRateLimiter.recordFailure(request.getEmail());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        loginAttemptRateLimiter.recordSuccess(request.getEmail());

        String accessToken = jwtTokenProvider.generateToken(user.getId());

        Membership membership = membershipRepository.findFirstByUserId(user.getId()).orElse(null);
        String role = membership != null ? membership.getRole().name() : null;
        String companyId = membership != null ? membership.getCompany().getId() : null;

        return new LoginResponse(accessToken, user.getId(), user.getUserType().name(), role, companyId);
    }

    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        if ("company".equals(request.getType())) {
            return signupCompany(request);
        }
        if (!"personal".equals(request.getType())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "가입 유형은 personal 또는 company만 허용됩니다.");
        }
        return signupPersonal(request);
    }

    public void logout(String authorizationHeader, String cookieToken) {
        String token = resolveBearerToken(authorizationHeader);
        if (token == null && cookieToken != null && !cookieToken.isBlank()) token = cookieToken;
        if (token == null || !jwtTokenProvider.validateToken(token)) return;
        tokenRevocationService.revoke(token, jwtTokenProvider.getExpirationEpochMillis(token));
    }

    private SignupResponse signupPersonal(SignupRequest request) {
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .userType(UserType.personal)
                .build();

        userRepository.save(user);

        return SignupResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType().name())
                .build();
    }

    private SignupResponse signupCompany(SignupRequest request) {
        if (isBlank(request.getCompanyName()) || isBlank(request.getBusinessNumber()) || isBlank(request.getIndustry())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "기업 가입은 회사명, 사업자번호, 업종이 필수입니다.");
        }
        if (!request.getBusinessNumber().matches("^\\d{10}$")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "사업자번호는 숫자 10자리여야 합니다.");
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
                .build();

        userRepository.save(user);

        Membership membership = Membership.builder()
                .user(user)
                .company(company)
                .role(UserRole.owner)
                .build();

        membershipRepository.save(membership);

        return SignupResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType().name())
                .companyId(company.getId())
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolveBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) return null;
        return authorizationHeader.substring(7);
    }
}
