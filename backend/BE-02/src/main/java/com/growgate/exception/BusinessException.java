package com.growgate.exception;

// 비즈니스 로직 예외
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String message;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.message = errorCode.getDefaultMessage();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
    }

    public ErrorCode getErrorCode() { return errorCode; }

    @Override
    public String getMessage() { return message; }
}
