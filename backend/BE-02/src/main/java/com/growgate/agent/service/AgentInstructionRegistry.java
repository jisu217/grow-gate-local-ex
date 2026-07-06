package com.growgate.agent.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AgentInstructionRegistry {

    private final List<AgentInstruction> instructions = List.of(
            new AgentInstruction("DATA_COLLECTION", "DATA_STATUS_NOTE", "근거 데이터, 리포트, 운영 대시보드",
                    "live/cache/fallback/seed 상태를 분리해 근거 수집 상태를 설명합니다."),
            new AgentInstruction("NORMALIZATION", "INPUT_NORMALIZATION_NOTE", "다음 진단 입력 보정",
                    "제품명, HS Code, 대상국 등 입력 보정 필요성을 설명합니다."),
            new AgentInstruction("MARKET", "MARKET_SIGNAL_NOTE", "시장성 판단, 리포트",
                    "시장 신호를 긍정/보완 후보로 설명하되 수출 가능 단정 표현을 금지합니다."),
            new AgentInstruction("REGULATION_CERTIFICATION", "REGULATION_CHECKLIST", "규제·인증, 실행보드, 리포트",
                    "인증 적용 가능성과 필요 문서를 검토 후보로 정리합니다."),
            new AgentInstruction("RECALL_SAFETY", "RECALL_CHECKLIST", "리콜 위험, 품질 체크리스트",
                    "유사 리콜 신호와 별도 제품안전 점검 필요성을 분리합니다."),
            new AgentInstruction("FTO_PATENT", "FTO_SEARCH_MEMO", "FTO·지원, 실행보드",
                    "공개 특허 기준 위험 후보 탐색과 최종 FTO 검토 필요성을 설명합니다."),
            new AgentInstruction("SUPPORT", "SUPPORT_PROGRAM_CANDIDATE", "지원사업 후보, 리포트",
                    "조건 유사 지원사업 후보와 기관 심사 한계를 함께 표시합니다."),
            new AgentInstruction("EXEC_BOARD", "EXECUTION_TASK_TEMPLATE", "업무 보드",
                    "Agent 판단을 담당자 검토용 업무 템플릿 후보로 변환합니다."),
            new AgentInstruction("REPORT", "BUYER_REPLY_DRAFT", "Markdown 리포트, 바이어 회신",
                    "바이어 회신 초안과 리포트 문장을 담당자 검토 전 초안으로 제한합니다."),
            new AgentInstruction("VERIFICATION", "REVIEW_REQUIRED_NOTE", "담당자 검토 큐",
                    "출처 누락, fallback, 금지 표현 제거 결과를 검토 큐로 정리합니다.")
    );

    private final Map<String, AgentInstruction> byAgent = instructions.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(AgentInstruction::agent, item -> item));

    public List<AgentInstruction> all() {
        return instructions;
    }

    public Optional<AgentInstruction> find(String agent) {
        return Optional.ofNullable(byAgent.get(agent));
    }

    public AgentInstruction require(String agent) {
        return find(agent).orElseThrow(() -> new IllegalArgumentException("Unknown agent: " + agent));
    }

    public record AgentInstruction(String agent, String knowledgeType, String reuseLocation, String promptInstruction) {
    }
}
