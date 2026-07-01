package com.growgate.security;

import com.growgate.security.entity.RevokedToken;
import com.growgate.security.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;

    @Transactional
    public void revoke(String token, long expiresAtEpochMillis) {
        if (token == null || token.isBlank()) return;
        cleanup();
        revokedTokenRepository.save(RevokedToken.builder()
                .tokenHash(hash(token))
                .expiresAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresAtEpochMillis), ZoneOffset.UTC))
                .build());
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String token) {
        if (token == null || token.isBlank()) return false;
        return revokedTokenRepository.existsById(hash(token));
    }

    @Transactional
    private void cleanup() {
        revokedTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for token revocation", e);
        }
    }
}
