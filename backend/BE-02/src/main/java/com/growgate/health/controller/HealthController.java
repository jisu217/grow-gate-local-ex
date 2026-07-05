package com.growgate.health.controller;

// 서버 연결 상태 확인용 헬스체크 엔드포인트
import com.growgate.publicdata.service.PublicDataOperationalStatusService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final DataSource dataSource;
    private final Environment environment;
    private final PublicDataOperationalStatusService publicDataOperationalStatusService;
    private final String jwtSecret;
    private final boolean agentEnabled;
    private final boolean fastapiEnabled;
    private final String appName;

    public HealthController(
            DataSource dataSource,
            Environment environment,
            PublicDataOperationalStatusService publicDataOperationalStatusService,
            @Value("${jwt.secret:}") String jwtSecret,
            @Value("${app.agent.enabled:false}") boolean agentEnabled,
            @Value("${app.fastapi.enabled:false}") boolean fastapiEnabled,
            @Value("${spring.application.name:grow-gate-local-ex}") String appName) {
        this.dataSource = dataSource;
        this.environment = environment;
        this.publicDataOperationalStatusService = publicDataOperationalStatusService;
        this.jwtSecret = jwtSecret;
        this.agentEnabled = agentEnabled;
        this.fastapiEnabled = fastapiEnabled;
        this.appName = appName;
    }

    @GetMapping({"/health", "/api/health"})
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        response.put("app", appName);
        response.put("service", "GROW-GATE BE-02");
        response.put("profile", activeProfile());
        response.put("version", environment.getProperty("APP_VERSION",
                environment.getProperty("GIT_SHA", "local-dev")));
        response.put("database", databaseStatus());
        response.put("migration", "ddl-auto");
        response.put("auth", Map.of(
                "jwtSecretConfigured", jwtSecret != null && !jwtSecret.isBlank(),
                "csrf", "disabled-spring-default-custom-double-submit-cookie-filter"
        ));
        response.put("agent", Map.of(
                "enabled", agentEnabled,
                "fastapiEnabled", fastapiEnabled,
                "mode", agentEnabled || fastapiEnabled ? "bounded-workflow-assisted" : "rule-assisted",
                "reason", agentEnabled || fastapiEnabled ? "APP_AGENT_ENABLED or APP_FASTAPI_ENABLED is true"
                        : "APP_AGENT_ENABLED and APP_FASTAPI_ENABLED are false"
        ));
        response.put("publicData", publicDataOperationalStatusService.healthSummary());
        response.put("checkedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        return response;
    }

    private String activeProfile() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) return "local";
        if (Arrays.asList(profiles).contains("docker")) return "docker";
        if (Arrays.asList(profiles).contains("prod")) return "prod";
        return String.join(",", profiles);
    }

    private String databaseStatus() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(1) ? "ok" : "error";
        } catch (Exception e) {
            return "error";
        }
    }
}
