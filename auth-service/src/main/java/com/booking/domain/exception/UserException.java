package com.booking.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception cho các lỗi liên quan đến user
 * Prefix: USR_xxx
 */
public class UserException extends DomainException{

    public UserException(String errorCode, String message) {
        super(errorCode, message,
                HttpStatus.BAD_REQUEST.value());
    }
}
