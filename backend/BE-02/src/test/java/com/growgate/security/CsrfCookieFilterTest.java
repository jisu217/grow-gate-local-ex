package com.growgate.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CsrfCookieFilterTest {

    private final CsrfCookieFilter filter = new CsrfCookieFilter();

    @Test
    void unsafeCookieAuthenticatedRequestRequiresMatchingCsrfHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/diagnosis");
        request.setCookies(new Cookie(CsrfCookieFilter.ACCESS_TOKEN_COOKIE, "access-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("CSRF token mismatch");
        verifyNoInteractions(chain);
    }

    @Test
    void unsafeCookieAuthenticatedRequestPassesWithMatchingCsrfHeader() throws Exception {
        String csrf = CsrfCookieFilter.newToken();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/diagnosis");
        request.setCookies(
                new Cookie(CsrfCookieFilter.ACCESS_TOKEN_COOKIE, "access-token"),
                new Cookie(CsrfCookieFilter.CSRF_COOKIE, csrf)
        );
        request.addHeader(CsrfCookieFilter.CSRF_HEADER, csrf);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void safeCookieAuthenticatedRequestGetsReadableCsrfCookie() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.setCookies(new Cookie(CsrfCookieFilter.ACCESS_TOKEN_COOKIE, "access-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> {
                    assertThat(cookie).contains(CsrfCookieFilter.CSRF_COOKIE + "=");
                    assertThat(cookie).doesNotContain("HttpOnly");
                    assertThat(cookie).contains("SameSite=Lax");
                });
        verify(chain).doFilter(request, response);
    }
}
