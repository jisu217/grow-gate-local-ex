package com.growgate.common.dto;

// 공통 에러 응답 포맷
public class ErrorResponse {

    private final ErrorDetail error;

    private ErrorResponse(String code, String message) {
        this.error = new ErrorDetail(code, message);
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }

    public ErrorDetail getError() { return error; }

    public static class ErrorDetail {
        private final String code;
        private final String message;

        ErrorDetail(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
}
