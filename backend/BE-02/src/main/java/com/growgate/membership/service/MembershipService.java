package com.growgate.membership.service;

import com.growgate.common.DateTimeUtils;
import com.growgate.company.entity.Company;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.membership.dto.MemberListResponse;
import com.growgate.membership.dto.MemberListResponse.MemberSummary;
import com.growgate.membership.entity.Membership;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.user.entity.User;
import com.growgate.user.enums.UserType;
import com.growgate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public MemberListResponse getMyCompanyMembers(String userId) {
        Company company = getCompany(userId);

        List<MemberSummary> members = membershipRepository.findByCompanyId(company.getId()).stream()
                .map(membership -> MemberSummary.builder()
                        .membershipId(membership.getId())
                        .userId(membership.getUser().getId())
                        .name(membership.getUser().getName())
                        .email(membership.getUser().getEmail())
                        .role(membership.getRole().name())
                        .createdAt(DateTimeUtils.formatUtc(membership.getCreatedAt()))
                        .build())
                .toList();

        return MemberListResponse.builder().members(members).build();
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
