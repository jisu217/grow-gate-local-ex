package com.growgate.membership.dto;

// 내 회사 팀원 목록 조회 응답 DTO
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MemberListResponse {

    private List<MemberSummary> members;

    @Getter
    @Builder
    public static class MemberSummary {
        private String membershipId;
        private String userId;
        private String name;
        private String email;
        private String role;
        private String createdAt;
    }
}
