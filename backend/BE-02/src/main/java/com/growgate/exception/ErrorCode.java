package com.growgate.exception;

// 에러 코드 및 HTTP 상태 정의
import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값 형식 오류 또는 필수 필드 누락"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증 토큰 없음 또는 만료"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한 없음 (본인 리소스가 아닌 경우)"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "해당 리소스 없음"),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "이미 존재하는 리소스"),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", "서버 내부 오류");

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
}
