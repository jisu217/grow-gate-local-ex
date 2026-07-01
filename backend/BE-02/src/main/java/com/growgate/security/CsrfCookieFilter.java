package com.growgate.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    public static final String ACCESS_TOKEN_COOKIE = "gg_access_token";
    public static final String CSRF_COOKIE = "gg_csrf_token";
    public static final String CSRF_HEADER = "X-GG-CSRF";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean hasAccessCookie = StringUtils.hasText(cookieValue(request, ACCESS_TOKEN_COOKIE));
        if (hasAccessCookie) {
            ensureCsrfCookie(request, response);
        }

        if (hasAccessCookie && !SAFE_METHODS.contains(request.getMethod())) {
            String cookieToken = cookieValue(request, CSRF_COOKIE);
            String headerToken = request.getHeader(CSRF_HEADER);
            if (!StringUtils.hasText(cookieToken) || !cookieToken.equals(headerToken)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"CSRF token mismatch\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void ensureCsrfCookie(HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.hasText(cookieValue(request, CSRF_COOKIE))) return;
        response.addHeader(HttpHeaders.SET_COOKIE, csrfCookie(newToken()).toString());
    }

    public ResponseCookie csrfCookie(String token) {
        return ResponseCookie.from(CSRF_COOKIE, token)
                .httpOnly(false)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(24 * 60 * 60)
                .build();
    }

    public ResponseCookie clearCsrfCookie() {
        return ResponseCookie.from(CSRF_COOKIE, "")
                .httpOnly(false)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    public static String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String cookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
