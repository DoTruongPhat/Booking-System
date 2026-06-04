package com.booking.domain.exception;


import org.springframework.http.HttpStatus;

/**
 * Exception cho các lỗi xác thực
 * Prefix: AUTH_xxx
 */
public class AuthException extends DomainException {

    public AuthException(String errorCode, String message) {
        super(errorCode, message,
                HttpStatus.UNAUTHORIZED.value());
    }
}
