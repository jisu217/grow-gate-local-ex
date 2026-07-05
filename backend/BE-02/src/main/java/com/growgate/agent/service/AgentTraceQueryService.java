package com.growgate.agent.service;

import com.growgate.agent.entity.AgentTraceEvent;
import com.growgate.agent.repository.AgentTraceEventRepository;
import com.growgate.company.entity.Company;
import com.growgate.diagnosis.service.SyntheticDemoAccess;
import com.growgate.diagnosis.repository.DiagnosisRepository;
import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import com.growgate.membership.repository.MembershipRepository;
import com.growgate.user.entity.User;
import com.growgate.user.enums.UserType;
import com.growgate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentTraceQueryService {

    private final AgentTraceEventRepository traceRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getTrace(String userId, String diagnosisId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        ensureDiagnosisVisible(user, diagnosisId);
        List<Map<String, Object>> events = traceRepository.findByDiagnosisIdOrderByStepNoAscCreatedAtAsc(diagnosisId)
                .stream()
                .map(this::toMap)
                .toList();
        return Map.of(
                "diagnosisId", diagnosisId,
                "count", events.size(),
                "events", events
        );
    }

    private void ensureDiagnosisVisible(User user, String diagnosisId) {
        if (user.getUserType() == UserType.company) {
            Company company = membershipRepository.findFirstByUserId(user.getId())
                    .map(membership -> membership.getCompany())
                    .orElse(null);
            if (company != null
                    && diagnosisRepository.findByIdAndCompanyIdAndDeletedAtIsNull(diagnosisId, company.getId()).isPresent()) {
                return;
            }
        }
        if (diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull(diagnosisId, user.getId()).isPresent()) {
            return;
        }
        if (diagnosisRepository.findById(diagnosisId)
                .filter(run -> run.getDeletedAt() == null)
                .filter(SyntheticDemoAccess::isSyntheticDemoRun)
                .isPresent()) {
            return;
        }
        throw new BusinessException(ErrorCode.NOT_FOUND, "진단 결과를 찾을 수 없습니다.");
    }

    private Map<String, Object> toMap(AgentTraceEvent event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("stepNo", event.getStepNo());
        row.put("agentName", event.getAgentName());
        row.put("toolName", event.getToolName());
        row.put("outputSchema", event.getOutputSchema());
        row.put("outputSummary", event.getOutputSummary());
        row.put("sourceType", event.getSourceType());
        row.put("provider", event.getProvider());
        row.put("endpoint", event.getEndpoint());
        row.put("latencyMs", event.getLatencyMs());
        row.put("status", event.getStatus());
        row.put("retryCount", event.getRetryCount());
        row.put("fallbackReason", event.getFallbackReason());
        row.put("evidenceCount", event.getEvidenceCount());
        row.put("createdAt", event.getCreatedAt());
        return row;
    }
}
