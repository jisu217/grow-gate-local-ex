package com.growgate.auth.service;

import com.growgate.exception.BusinessException;
import com.growgate.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptRateLimiter {

    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();

    @Value("${app.security.login-rate-limit.max-failures:5}")
    private int maxFailures;

    @Value("${app.security.login-rate-limit.block-minutes:10}")
    private long blockMinutes;

    public void assertAllowed(String email) {
        AttemptState state = attempts.get(key(email));
        if (state == null || state.blockedUntil == null) return;
        if (Instant.now(clock).isBefore(state.blockedUntil)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "로그인 실패가 반복되어 잠시 후 다시 시도해주세요.");
        }
    }

    public void recordFailure(String email) {
        attempts.compute(key(email), (ignored, state) -> {
            AttemptState current = state == null ? new AttemptState() : state;
            current.failures++;
            if (current.failures >= maxFailures) {
                current.blockedUntil = Instant.now(clock).plusSeconds(blockMinutes * 60);
            }
            return current;
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static class AttemptState {
        private int failures;
        private Instant blockedUntil;
    }
}
