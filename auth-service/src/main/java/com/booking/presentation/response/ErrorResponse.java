package com.booking.presentation.response;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class ErrorResponse {

    // Luôn false khi có lỗi
    private boolean success;

    // Mã lỗi theo prefix: AUTH_xxx, USR_xxx...
    private String errorCode;

    // Mô tả lỗi
    private String message;

    // Thời điểm xảy ra lỗi
    private ZonedDateTime timestamp;

    // UUID unique cho mỗi request - dùng để trace log
    private String requestId;

    // TraceId từ OpenTelemetry (sẽ thêm sau)
    private String traceId;

    // Chi tiết lỗi validation
    private Object details;
}