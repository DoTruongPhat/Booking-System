package com.booking.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception cho các lỗi phân quyền
 * Prefix: RBC_xxx
 */
public class PermissionException extends DomainException {
    public PermissionException(String errorCode, String message) {
        super(errorCode, message,
                HttpStatus.FORBIDDEN.value());
    }
}
