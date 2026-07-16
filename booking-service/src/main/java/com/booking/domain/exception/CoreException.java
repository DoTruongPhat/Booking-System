package com.booking.domain.exception;

import lombok.Getter;

@Getter
public class CoreException extends RuntimeException {

    private final CoreErrorCode errorCode;

    public CoreException(CoreErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CoreException(CoreErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}