package com.growgate.auth.service;

import com.growgate.auth.dto.SignupRequest;
import com.growgate.auth.entity.UserConsentRecord;
import com.growgate.auth.repository.UserConsentRecordRepository;
import com.growgate.company.repository.CompanyRepository;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.security.JwtTokenProvider;
import com.growgate.security.TokenRevocationService;
import com.growgate.user.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceConsentTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void signupRequestRequiresTermsAndPrivacyConsent() {
        SignupRequest request = personalSignupRequest(false, false);

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("termsAccepted", "privacyAccepted");
    }

    @Test
    void signupPersonalStoresConsentRecord() {
        UserRepository userRepository = mock(UserRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        MembershipRepository membershipRepository = mock(MembershipRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
        LoginAttemptRateLimiter loginAttemptRateLimiter = mock(LoginAttemptRateLimiter.class);
        UserConsentRecordRepository consentRepository = mock(UserConsentRecordRepository.class);
        AuthService authService = new AuthService(
                userRepository,
                companyRepository,
                membershipRepository,
                passwordEncoder,
                jwtTokenProvider,
                tokenRevocationService,
                loginAttemptRateLimiter,
                consentRepository
        );
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authService.signup(personalSignupRequest(true, true));

        ArgumentCaptor<UserConsentRecord> captor = ArgumentCaptor.forClass(UserConsentRecord.class);
        verify(consentRepository).save(captor.capture());
        UserConsentRecord record = captor.getValue();
        assertThat(record.isTermsAccepted()).isTrue();
        assertThat(record.isPrivacyAccepted()).isTrue();
        assertThat(record.isMarketingAccepted()).isFalse();
        assertThat(record.getPolicyVersion()).isEqualTo("2026-07-privacy-v1");
        assertThat(record.getSource()).isEqualTo("signup");
    }

    private SignupRequest personalSignupRequest(boolean terms, boolean privacy) {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "type", "personal");
        ReflectionTestUtils.setField(request, "email", "privacy-user@growgate.local");
        ReflectionTestUtils.setField(request, "password", "password123");
        ReflectionTestUtils.setField(request, "name", "privacy user");
        ReflectionTestUtils.setField(request, "termsAccepted", terms);
        ReflectionTestUtils.setField(request, "privacyAccepted", privacy);
        ReflectionTestUtils.setField(request, "marketingAccepted", false);
        ReflectionTestUtils.setField(request, "consentPolicyVersion", "2026-07-privacy-v1");
        return request;
    }
}
