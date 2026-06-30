package com.growgate.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenRevocationService {

    private final Map<String, Long> revokedUntilEpochMillis = new ConcurrentHashMap<>();

    public void revoke(String token, long expiresAtEpochMillis) {
        if (token == null || token.isBlank()) return;
        cleanup();
        revokedUntilEpochMillis.put(token, expiresAtEpochMillis);
    }

    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) return false;
        cleanup();
        return revokedUntilEpochMillis.containsKey(token);
    }

    private void cleanup() {
        long now = Instant.now().toEpochMilli();
        revokedUntilEpochMillis.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
