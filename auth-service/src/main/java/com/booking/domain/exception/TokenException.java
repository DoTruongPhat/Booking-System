package com.booking.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception cho các lỗi liên quan đến token
 * Prefix: TKN_xxx
 */
public class TokenException extends DomainException {

    public TokenException(String errorCode, String message) {
        super(errorCode, message,
                HttpStatus.UNAUTHORIZED.value());
    }
}
