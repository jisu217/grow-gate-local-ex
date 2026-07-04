package com.growgate.ml.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
public class MlModelController {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private final ObjectMapper objectMapper;

    @GetMapping("/models/latest")
    public ResponseEntity<Map<String, Object>> latestModel() {
        Path artifact = Path.of("..", "..", "ml", "artifacts", "growgate-gate-risk-v0.1.0").normalize();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("modelName", "growgate-gate-risk");
        result.put("modelVersion", "0.1.0");
        result.put("artifactPath", artifact.toString());
        result.put("mlRole", "ML advisory signal");
        result.put("finalGateOwner", "deterministic rule scoring + public-data evidence ledger");
        result.put("syntheticTraining", true);
        result.put("notRealCustomerOutcome", true);
        result.put("limitations", java.util.List.of(
                "실제 고객 outcome label이 아닌 synthetic/rule-assisted label입니다.",
                "수출 성공 확률을 예측하지 않습니다.",
                "최종 Gate를 override하지 않습니다."
        ));
        result.put("metrics", readJson(artifact.resolve("metrics.json")));
        result.put("trainManifest", readJson(artifact.resolve("train_manifest.json")));
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> readJson(Path path) {
        if (!Files.exists(path)) {
            return Map.of("status", "MISSING", "path", path.toString(),
                    "message", "Run python3 ml/pipelines/train_all.py to generate the artifact.");
        }
        try {
            return objectMapper.readValue(Files.readString(path), MAP_TYPE);
        } catch (Exception e) {
            return Map.of("status", "INVALID", "path", path.toString());
        }
    }
}
