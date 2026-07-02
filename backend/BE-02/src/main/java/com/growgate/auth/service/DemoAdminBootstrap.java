package com.growgate.auth.service;

import com.growgate.auth.entity.UserConsentRecord;
import com.growgate.auth.repository.UserConsentRecordRepository;
import com.growgate.company.entity.Company;
import com.growgate.company.repository.CompanyRepository;
import com.growgate.membership.entity.Membership;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.user.entity.User;
import com.growgate.user.enums.UserRole;
import com.growgate.user.enums.UserType;
import com.growgate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoAdminBootstrap implements ApplicationRunner {

    private static final String COMPANY_ID = "11111111-1111-1111-1111-111111111111";
    private static final String USER_ID = "22222222-2222-2222-2222-222222222223";
    private static final String MEMBERSHIP_ID = "44444444-4444-4444-4444-444444444440";
    private static final String CONSENT_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1";
    private static final String BUSINESS_NUMBER = "615-81-2026";

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final MembershipRepository membershipRepository;
    private final UserConsentRecordRepository consentRecordRepository;

    @Value("${app.demo-admin.enabled:true}")
    private boolean enabled;

    @Value("${app.demo-admin.email:admin@growgate.com}")
    private String email;

    @Value("${app.demo-admin.name:김지용}")
    private String name;

    @Value("${app.demo-admin.password-hash:$2y$10$5BlFP36IjiTq20fBl/kkP.tbeQDqStrGae.empkMuMzCmfWDiXQlW}")
    private String passwordHash;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        if (email == null || email.isBlank() || passwordHash == null || passwordHash.isBlank()) {
            log.warn("Demo admin bootstrap skipped because email/password hash is blank.");
            return;
        }

        Company company = companyRepository.findByBusinessNumber(BUSINESS_NUMBER)
                .orElseGet(() -> companyRepository.save(Company.builder()
                        .id(COMPANY_ID)
                        .name("경남 배터리테크")
                        .businessNumber(BUSINESS_NUMBER)
                        .industry("전기전자")
                        .build()));

        User user = userRepository.findByEmail(email)
                .map(existing -> {
                    existing.updateDemoCredentials(name, passwordHash);
                    return existing;
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .id(USER_ID)
                        .email(email)
                        .passwordHash(passwordHash)
                        .name(name)
                        .userType(UserType.company)
                        .build()));

        boolean hasMembership = membershipRepository.findByUserId(user.getId()).stream()
                .anyMatch(membership -> membership.getCompany().getId().equals(company.getId()));
        if (!hasMembership) {
            membershipRepository.save(Membership.builder()
                    .id(MEMBERSHIP_ID)
                    .user(user)
                    .company(company)
                    .role(UserRole.owner)
                    .build());
        }

        consentRecordRepository.save(UserConsentRecord.builder()
                .id(CONSENT_ID)
                .user(user)
                .policyVersion("2026-07-privacy-v1")
                .termsAccepted(true)
                .privacyAccepted(true)
                .marketingAccepted(false)
                .source("seed-demo-not-live")
                .build());
    }
}
