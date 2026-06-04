package com.booking.domain.exception;


import org.springframework.http.HttpStatus;

/**
 * Exception cho các lỗi hạ tầng (DB, Cache, External)
 * Prefix: CMN_xxx
 */
public class InfrastructureException extends BaseException {

    public InfrastructureException(String errorCode, String message) {
        super(errorCode, message,
                HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
