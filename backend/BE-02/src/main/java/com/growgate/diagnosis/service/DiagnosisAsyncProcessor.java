package com.growgate.diagnosis.service;

// 비동기 진단 실행 처리 (@Async)
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.diagnosis.entity.DiagnosisRun;
import com.growgate.diagnosis.enums.DiagnosisStatus;
import com.growgate.diagnosis.repository.DiagnosisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisAsyncProcessor {

    private final DiagnosisRepository diagnosisRepository;
    private final ObjectMapper objectMapper;

    @Async
    @Transactional
    public void process(String diagnosisId) {
        DiagnosisRun run = diagnosisRepository.findById(diagnosisId).orElse(null);
        if (run == null) return;

        try {
            run.setStatus(DiagnosisStatus.running);
            diagnosisRepository.save(run);

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("diagnosisScore", 72);
            resultMap.put("summary", "전반적인 수출 준비도는 양호하나 FTO 리스크 보완 필요");
            resultMap.put("evidenceSources", List.of("관세청", "KOTRA", "KTL", "SafetyKorea", "KIPRIS", "기업마당", "EU RAPEX"));
            resultMap.put("riskItems", List.of(
                    Map.of("level", "high", "label", "FTO 리스크 보완 필요"),
                    Map.of("level", "medium", "label", "인증서 만료 임박 항목 있음")
            ));
            resultMap.put("actionRoadmap", Map.of("d30", 4, "d60", 4, "d90", 4));
            resultMap.put("sources", List.of(
                    Map.of("field", "certifications", "documentName", "KC 인증서",
                            "url", "https://www.safetykorea.kr/", "retrievedAt", "2026-05-20"),
                    Map.of("field", "regulations", "documentName", "EU CE 규정 가이드라인",
                            "url", "https://ec.europa.eu/", "retrievedAt", "2026-05-21")
            ));

            run.setResult(objectMapper.writeValueAsString(resultMap));
            run.setStatus(DiagnosisStatus.done);
            diagnosisRepository.save(run);
        } catch (Exception e) {
            log.error("Diagnosis processing failed for id: {}", diagnosisId, e);
            run.setStatus(DiagnosisStatus.failed);
            diagnosisRepository.save(run);
        }
    }
}
