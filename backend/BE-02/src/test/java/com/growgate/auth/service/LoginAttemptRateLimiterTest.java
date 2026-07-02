package com.growgate.auth.service;

import com.growgate.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptRateLimiterTest {

    @Test
    void blocksAfterConfiguredFailuresAndClearsAfterSuccess() {
        LoginAttemptRateLimiter limiter = new LoginAttemptRateLimiter();
        ReflectionTestUtils.setField(limiter, "maxFailures", 2);
        ReflectionTestUtils.setField(limiter, "blockMinutes", 10L);

        limiter.recordFailure("USER@GrowGate.local");
        assertThatCode(() -> limiter.assertAllowed("user@growgate.local")).doesNotThrowAnyException();

        limiter.recordFailure("user@growgate.local");
        assertThatThrownBy(() -> limiter.assertAllowed(" user@growgate.local "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("로그인 실패");

        limiter.recordSuccess("user@growgate.local");
        assertThatCode(() -> limiter.assertAllowed("user@growgate.local")).doesNotThrowAnyException();
    }
}
