package com.growgate.auth.controller;

// 로그인·회원가입 API 엔드포인트
import com.growgate.auth.dto.LoginRequest;
import com.growgate.auth.dto.LoginResponse;
import com.growgate.auth.dto.SignupRequest;
import com.growgate.auth.dto.SignupResponse;
import com.growgate.auth.service.AuthService;
import com.growgate.security.CsrfCookieFilter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String ACCESS_TOKEN_COOKIE = "gg_access_token";

    private final AuthService authService;
    private final CsrfCookieFilter csrfCookieFilter;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie(response.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookieFilter.csrfCookie(CsrfCookieFilter.newToken()).toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @CookieValue(value = ACCESS_TOKEN_COOKIE, required = false) String cookieToken) {
        authService.logout(authorization, cookieToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookieFilter.clearCsrfCookie().toString())
                .body(Map.of("message", "로그아웃 되었습니다."));
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    private ResponseCookie accessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(24 * 60 * 60)
                .build();
    }

    private ResponseCookie clearAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
}
