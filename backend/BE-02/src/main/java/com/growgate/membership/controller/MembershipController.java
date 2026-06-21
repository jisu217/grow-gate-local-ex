package com.growgate.membership.controller;

// 내 회사 팀원 목록 API 엔드포인트
import com.growgate.membership.dto.MemberListResponse;
import com.growgate.membership.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @GetMapping("/me/members")
    public ResponseEntity<MemberListResponse> getMyCompanyMembers(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(membershipService.getMyCompanyMembers(userDetails.getUsername()));
    }
}
