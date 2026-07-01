package com.growgate.company.service;

import com.growgate.common.DateTimeUtils;
import com.growgate.company.dto.CompanyMeResponse;
import com.growgate.company.dto.UpdateCompanyRequest;
import com.growgate.company.dto.UpdateCompanyResponse;
import com.growgate.company.entity.Company;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.membership.entity.Membership;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.user.entity.User;
import com.growgate.user.enums.UserType;
import com.growgate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public CompanyMeResponse getMyCompany(String userId) {
        Company company = getCompany(userId);
        return CompanyMeResponse.builder()
                .companyId(company.getId())
                .name(company.getName())
                .businessNumber(company.getBusinessNumber())
                .industry(company.getIndustry())
                .createdAt(DateTimeUtils.formatUtc(company.getCreatedAt()))
                .build();
    }

    public UpdateCompanyResponse updateMyCompany(String userId, UpdateCompanyRequest request) {
        Company company = getCompany(userId);
        company.updateIndustry(request.getIndustry());
        return UpdateCompanyResponse.builder()
                .companyId(company.getId())
                .industry(company.getIndustry())
                .updatedAt(DateTimeUtils.formatUtc(company.getUpdatedAt()))
                .build();
    }

    private Company getCompany(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        if (user.getUserType() != UserType.company) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "기업 계정만 접근할 수 있습니다.");
        }
        return membershipRepository.findFirstByUserId(userId)
                .map(Membership::getCompany)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "기업 계정만 접근할 수 있습니다."));
    }
}
