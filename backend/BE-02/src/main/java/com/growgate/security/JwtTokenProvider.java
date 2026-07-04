package com.growgate.security;

// JWT 토큰 생성·검증
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Set;

@Component
public class JwtTokenProvider {

    public static final String UNSAFE_LOCAL_DEMO_SECRET = "unsafe-local-demo-secret-change-before-prod-32bytes";
    private static final Set<String> PROD_PROFILES = Set.of("prod", "production");

    private final Key key;
    private final long expiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration,
            Environment environment) {
        this(secret, expiration, environment.getActiveProfiles());
    }

    JwtTokenProvider(String secret, long expiration, String[] activeProfiles) {
        byte[] keyBytes = resolveKeyBytes(secret, activeProfiles);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("jwt.secret은 HS256 기준 32 bytes 이상이어야 합니다.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
    }

    public String generateToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public long getExpirationEpochMillis(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration().getTime();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] resolveKeyBytes(String secret, String[] activeProfiles) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("jwt.secret 설정이 필요합니다.");
        }
        if (UNSAFE_LOCAL_DEMO_SECRET.equals(secret) && isProd(activeProfiles)) {
            throw new IllegalArgumentException("prod profile에서는 JWT_SECRET을 운영용 32 bytes 이상 secret으로 설정해야 합니다.");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            if (decoded.length >= 32) return decoded;
        } catch (IllegalArgumentException ignored) {
            // Plain text secrets are accepted below when they are strong enough.
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private boolean isProd(String[] activeProfiles) {
        if (activeProfiles == null) return false;
        for (String profile : activeProfiles) {
            if (profile != null && PROD_PROFILES.contains(profile.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
