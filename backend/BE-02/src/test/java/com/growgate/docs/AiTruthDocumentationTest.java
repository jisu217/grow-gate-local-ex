package com.growgate.docs;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiTruthDocumentationTest {

    private final Path root = Path.of("..", "..").normalize();

    @Test
    void contestDefenseAndAgentInstructionsUseAllowedAiClaimsOnly() throws Exception {
        List<Path> requiredDocs = List.of(
                root.resolve("README.md"),
                root.resolve("AGENTS.md"),
                root.resolve("docs/contest-defense.md"),
                root.resolve("docs/ai-claim-truth-table.md"),
                root.resolve("docs/engineering/failure-log.md")
        );

        for (Path doc : requiredDocs) {
            assertThat(Files.exists(doc)).as("required AI truth doc exists: " + doc).isTrue();
        }

        String combined = String.join("\n", requiredDocs.stream()
                .map(this::read)
                .toList());

        assertThat(combined).doesNotContain(
                "production " + "XGBoost",
                "Production " + "XGBoost",
                "ML-based risk " + "prediction",
                "AI-determined final " + "Gate",
                "open-ended autonomous " + "Agent",
                "vector " + "RAG",
                "Vector " + "RAG",
                "embedding-based " + "retrieval"
        );
        assertThat(combined).contains(
                "Rule-based Gate",
                "LLM assisted report",
                "Source-bound evidence retrieval",
                "Claim verification",
                "bounded workflow agent loop"
        );
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception e) {
            throw new IllegalStateException("failed to read " + path, e);
        }
    }
}
