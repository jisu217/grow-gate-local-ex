package com.growgate.security;

// 인가 실패(권한 없음) 시 contract 에러 포맷으로 403 응답
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.common.dto.ErrorResponse;
import com.growgate.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        ErrorCode errorCode = ErrorCode.FORBIDDEN;
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ErrorResponse.of(errorCode.getCode(), errorCode.getDefaultMessage()))
        );
    }
}
