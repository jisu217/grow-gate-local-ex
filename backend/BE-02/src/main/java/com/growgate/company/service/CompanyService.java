package com.growgate.company.service;

import com.growgate.company.dto.CompanyMeResponse;
import com.growgate.company.dto.UpdateCompanyRequest;
import com.growgate.company.dto.UpdateCompanyResponse;
import com.growgate.company.entity.Company;
import com.growgate.company.repository.CompanyRepository;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.user.entity.User;
import com.growgate.user.enums.UserType;
import com.growgate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public CompanyMeResponse getMyCompany(String userId) {
        User user = getCompanyUser(userId);
        Company company = user.getCompany();
        return CompanyMeResponse.builder()
                .companyId(company.getId())
                .name(company.getName())
                .businessNumber(company.getBusinessNumber())
                .industry(company.getIndustry())
                .createdAt(company.getCreatedAt().format(ISO_FMT))
                .build();
    }

    public UpdateCompanyResponse updateMyCompany(String userId, UpdateCompanyRequest request) {
        User user = getCompanyUser(userId);
        Company company = user.getCompany();
        company.updateIndustry(request.getIndustry());
        return UpdateCompanyResponse.builder()
                .companyId(company.getId())
                .industry(company.getIndustry())
                .updatedAt(company.getUpdatedAt().format(ISO_FMT))
                .build();
    }

    private User getCompanyUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        if (user.getUserType() != UserType.company || user.getCompany() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "기업 계정만 접근할 수 있습니다.");
        }
        return user;
    }
}
