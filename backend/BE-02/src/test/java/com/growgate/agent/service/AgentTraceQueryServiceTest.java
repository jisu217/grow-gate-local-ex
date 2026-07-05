package com.growgate.agent.service;

import com.growgate.agent.repository.AgentTraceEventRepository;
import com.growgate.company.entity.Company;
import com.growgate.diagnosis.entity.DiagnosisRun;
import com.growgate.diagnosis.enums.DiagnosisStatus;
import com.growgate.diagnosis.repository.DiagnosisRepository;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.membership.entity.Membership;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.user.entity.User;
import com.growgate.user.enums.UserRole;
import com.growgate.user.enums.UserType;
import com.growgate.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentTraceQueryServiceTest {

    private final AgentTraceEventRepository traceRepository = mock(AgentTraceEventRepository.class);
    private final DiagnosisRepository diagnosisRepository = mock(DiagnosisRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MembershipRepository membershipRepository = mock(MembershipRepository.class);
    private final AgentTraceQueryService service = new AgentTraceQueryService(
            traceRepository,
            diagnosisRepository,
            userRepository,
            membershipRepository
    );

    @Test
    void getTraceAllowsWatermarkedSyntheticDemoDiagnosisOutsideCurrentCompany() {
        User user = user("admin-user", UserType.company);
        Company currentCompany = Company.builder().id("current-company").build();
        Membership membership = Membership.builder().role(UserRole.admin).company(currentCompany).build();
        DiagnosisRun run = DiagnosisRun.builder()
                .id("synthetic-demo-diag")
                .user(user("scenario-owner", UserType.company))
                .inputData("{}")
                .knowledgeSnapshot("{}")
                .result("{\"demoWatermark\":\"synthetic_demo_not_real_customer\"}")
                .status(DiagnosisStatus.done)
                .targetCountry("EU")
                .build();

        when(userRepository.findById("admin-user")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("admin-user")).thenReturn(Optional.of(membership));
        when(diagnosisRepository.findByIdAndCompanyId("synthetic-demo-diag", "current-company"))
                .thenReturn(Optional.empty());
        when(diagnosisRepository.findByIdAndUserId("synthetic-demo-diag", "admin-user"))
                .thenReturn(Optional.empty());
        when(diagnosisRepository.findById("synthetic-demo-diag")).thenReturn(Optional.of(run));
        when(traceRepository.findByDiagnosisIdOrderByStepNoAscCreatedAtAsc("synthetic-demo-diag"))
                .thenReturn(List.of());

        service.getTrace("admin-user", "synthetic-demo-diag");

        verify(traceRepository).findByDiagnosisIdOrderByStepNoAscCreatedAtAsc("synthetic-demo-diag");
    }

    @Test
    void getTraceKeepsRealDiagnosisOutsideCurrentCompanyHidden() {
        User user = user("admin-user", UserType.company);
        Company currentCompany = Company.builder().id("current-company").build();
        Membership membership = Membership.builder().role(UserRole.admin).company(currentCompany).build();
        DiagnosisRun run = DiagnosisRun.builder()
                .id("real-other-company-diag")
                .user(user("other-owner", UserType.company))
                .inputData("{}")
                .knowledgeSnapshot("{}")
                .result("{\"summary\":\"real customer run\"}")
                .status(DiagnosisStatus.done)
                .targetCountry("EU")
                .build();

        when(userRepository.findById("admin-user")).thenReturn(Optional.of(user));
        when(membershipRepository.findFirstByUserId("admin-user")).thenReturn(Optional.of(membership));
        when(diagnosisRepository.findByIdAndCompanyId("real-other-company-diag", "current-company"))
                .thenReturn(Optional.empty());
        when(diagnosisRepository.findByIdAndUserId("real-other-company-diag", "admin-user"))
                .thenReturn(Optional.empty());
        when(diagnosisRepository.findById("real-other-company-diag")).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> service.getTrace("admin-user", "real-other-company-diag"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    private User user(String id, UserType type) {
        return User.builder()
                .id(id)
                .email(id + "@growgate.local")
                .passwordHash("encoded")
                .name("테스트 사용자")
                .userType(type)
                .build();
    }
}
