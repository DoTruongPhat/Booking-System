package com.booking.domain.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BaseException {
    public ValidationException(String errorCode, String message) {

        super(errorCode, message,
                HttpStatus.BAD_REQUEST.value());
    }
}
