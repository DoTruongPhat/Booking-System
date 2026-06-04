package com.booking.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Log4j2
@Order(4)
public class TraceFilter  extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String REQUEST_ID = "requestId";
    private static final String X_REQUEST_ID = "X-Request-Id";
    private static final String X_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try{
            // traceId: BE tự sinh mỗi request
            String traceId = UUID.randomUUID().toString()
                    .replace(".", "").substring(0, 16);

            // requestId: FE gửi lên hoặc BE tự sinh
            String requestId = request.getHeader(X_REQUEST_ID);
            if(requestId == null || requestId.isBlank()){
                requestId = UUID.randomUUID().toString()
                        .replace(".", "").substring(0, 16);
            }

            // Lưu vào MDC → Log4j2 tự lấy ra
            ThreadContext.put(TRACE_ID, traceId);
            ThreadContext.put(REQUEST_ID, requestId);

            // Trả về header cho FE biết
            response.setHeader(X_TRACE_ID, traceId);
            response.setHeader(X_REQUEST_ID, requestId);

            filterChain.doFilter(request, response);

        }finally {

            // Xóa MDC sau khi xử lý xong
            // Tránh leak sang request khác
            ThreadContext.clearAll();

        }

    }
}
