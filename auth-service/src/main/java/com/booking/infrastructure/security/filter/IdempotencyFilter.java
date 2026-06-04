package com.booking.infrastructure.security.filter;

import com.booking.infrastructure.external.cache.IdempotencyCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Log4j2
@Order(3)
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyCacheService idempotencyCacheService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        String idempotencyKey = request
                .getHeader("Idempotency-Key");

        // Không có key → cho qua bình thường
        if(idempotencyKey == null || idempotencyKey.isBlank()){
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("[Idempotency] Key: {}", idempotencyKey);

        // Kiểm tra key đã tồn tại trong Redis chưa
        String cachedResponse = idempotencyCacheService
                .get(idempotencyKey);

        if(cachedResponse != null){
            // Trả response cũ → không xử lý lại
            log.info("[Idempotency] Duplicate request: {}",
                    idempotencyKey);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(cachedResponse);
            return;
        }
        // Wrap response để capture nội dung
        ContentCachingResponseWrapper responseWrapper =
                new ContentCachingResponseWrapper(response);
        // Cho request đi tiếp
        filterChain.doFilter(request, responseWrapper);
        // Lưu response vào Redis
        byte[] responseBody =
                responseWrapper.getContentAsByteArray();

        if(responseBody.length > 0){
            String responseStr = new String(responseBody);
            idempotencyCacheService.save(idempotencyKey, responseStr);
            log.debug("[Idempotency] Cached: {}",
                    idempotencyKey);
        }
        // Copy response về client
        responseWrapper.copyBodyToResponse();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        // Chỉ apply cho POST và PUT
        return !method.equals("POST")
                && !method.equals("PUT");
    }
}
