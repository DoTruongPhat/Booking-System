package com.booking.domain.exception;

import lombok.Getter;

/**
 * Exception cho vi phạm business rule.
 * Khác với CoreException (technical/validation error).
 */
@Getter
public class BusinessRuleException extends RuntimeException {
    private final String ruleCode;

    public BusinessRuleException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }
}