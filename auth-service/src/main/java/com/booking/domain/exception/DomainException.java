package com.booking.domain.exception;

/**
 * Base exception cho tất cả lỗi business logic
 * Tất cả domain exception kế thừa class này
 */
public class DomainException extends BaseException {

    public DomainException(String errorCode, String message, int httpStatus) {
        super(errorCode, message, httpStatus);
    }


}
