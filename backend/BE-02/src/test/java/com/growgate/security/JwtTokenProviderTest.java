package com.growgate.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    @Test
    void prodRejectsUnsafeLocalDemoSecret() {
        assertThatThrownBy(() -> new JwtTokenProvider(
                "unsafe-local-demo-secret-change-before-prod-32bytes",
                86400000L,
                new String[]{"prod"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prod");
    }

    @Test
    void shortSecretFailsFastWithoutPrintingSecret() {
        assertThatThrownBy(() -> new JwtTokenProvider("short", 86400000L, new String[]{"local"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("short");
    }
}
