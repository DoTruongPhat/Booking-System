package com.booking.presentation.advice;

import com.booking.domain.exception.*;
import com.booking.presentation.response.ErrorResponse;
import lombok.extern.log4j.Log4j2;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    /**
     * Bắt tất cả DomainException và subclass
     * AuthException, UserException, TokenException, PermissionException
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(
            DomainException ex) {
        log.warn("[Exception] Domain: code={} message={}",
                ex.getErrorCode(), ex.getMessage());

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(buildError(
                        ex.getErrorCode(),
                        ex.getMessage()
                ));
    }

    /**
     * Bắt lỗi validate input (@Valid fail)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            fieldErrors.put(field, message);
        });

        log.warn("[Exception] Validation: {}", fieldErrors);

        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .errorCode(ErrorCode.CMN_002)
                .message(ErrorCode.CMN_002_MSG)
                .timestamp(ZonedDateTime.now())
                .requestId(UUID.randomUUID().toString())
                .details(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Bắt InfrastructureException
     */
    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<ErrorResponse> handleInfrastructure(
            InfrastructureException ex) {
        log.error("[Exception] Infrastructure: code={} message={}",
                ex.getErrorCode(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(
                        ex.getErrorCode(),
                        ex.getMessage()
                ));
    }

    /**
     * Bắt tất cả exception không xử lý
     * Không trả stack trace cho FE
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex) {
        log.error("[Exception] Unhandled: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(
                        ErrorCode.CMN_001,
                        ErrorCode.CMN_001_MSG
                ));
    }

    private ErrorResponse buildError(String errorCode,
                                     String message) {
        return ErrorResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .timestamp(ZonedDateTime.now())
                .requestId(UUID.randomUUID().toString())
                .traceId(MDC.get("traceId"))
                .build();
    }

    /**
     * Bắt lỗi phân quyền (@PreAuthorize fail)
     */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(Exception ex) {
        log.warn("[Exception] Access Denied: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(buildError(
                        ErrorCode.CMN_003,
                        ErrorCode.CMN_003_MSG
                ));
    }
}