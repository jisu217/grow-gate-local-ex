package com.growgate.health.controller;

// 서버 연결 상태 확인용 헬스체크 엔드포인트
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "GROW-GATE BE-02");
    }
}
