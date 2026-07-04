package com.growgate.agent.repository;

import com.growgate.agent.entity.AgentTraceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentTraceEventRepository extends JpaRepository<AgentTraceEvent, Long> {
    List<AgentTraceEvent> findByDiagnosisIdOrderByStepNoAscCreatedAtAsc(String diagnosisId);
}
