package com.booking.domain.exception;

/**
 * Base exception cho toàn bộ hệ thống
 * Tất cả exception đều kế thừa class này
 */
public class BaseException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public BaseException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String  getErrorCode() {
        return errorCode;
    }
    public int getHttpStatus() {
        return httpStatus;
    }
}
