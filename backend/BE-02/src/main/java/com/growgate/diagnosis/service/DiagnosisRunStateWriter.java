package com.growgate.diagnosis.service;

import com.growgate.diagnosis.enums.DiagnosisStatus;
import com.growgate.diagnosis.repository.DiagnosisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiagnosisRunStateWriter {

    private final DiagnosisRepository diagnosisRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(String diagnosisId, DiagnosisStatus status) {
        diagnosisRepository.findById(diagnosisId).ifPresent(run -> {
            run.setStatus(status);
            diagnosisRepository.save(run);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String diagnosisId, String resultJson, DiagnosisStatus status) {
        diagnosisRepository.findById(diagnosisId).ifPresent(run -> {
            run.setResult(resultJson);
            run.setStatus(status);
            diagnosisRepository.save(run);
        });
    }
}
