package com.booking.infrastructure.security.filter;


import com.booking.domain.exception.ErrorCode;
import com.booking.infrastructure.config.AppProperties;
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

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Log4j2
@Order(1)
public class ApiKeyFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;

    @Override
    protected void doFilterInternal
            (HttpServletRequest request,
             HttpServletResponse response,
             FilterChain filterChain)
            throws ServletException, IOException {

        // Bước 1: Kiểm tra API key
        String apiKey = request.getHeader("X-API-KEY");
        String expectedKey = appProperties.getSecurity().getApiKey();

        if (expectedKey == null ||
                !expectedKey.equals(apiKey)) {
            log.warn("[ApiKey] Invalid key from IP: {}",
                    request.getRemoteAddr());
            sendUnauthorized(response);
            return;
        }


        // Bước 2: Kiểm tra Referrer/Origin
        // Chặn request không từ FE của mình
        if(!isAllowedOrigin(request)){
            log.warn("[ApiKey] Invalid origin from IP: {}",
                    request.getRemoteAddr());
            sendUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);

    }

    /**
     * Kiểm tra request có từ FE hợp lệ không
     * Dùng Origin hoặc Referer header
     */
    private boolean isAllowedOrigin(HttpServletRequest request) {
        String allowedOrigins = appProperties
                .getSecurity().getAllowedOrigins();

        // Nếu không config → cho qua (dev mode)
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return true;
        }

        // Lấy Origin hoặc Referer từ request
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");


        // Postman/internal tool không có Origin → cho qua dev
        // Production nên bỏ dòng này
        if (origin == null && referer == null) {
            log.debug("[ApiKey] No origin header - allowing (dev mode)");
            return true;
        }

        // Kiểm tra từng allowed origin
        String [] allowed = allowedOrigins.split(",");
        for(String allowedOrigin : allowed) {
            String trimmed = allowedOrigin.trim();
            if(origin != null && origin.startsWith(trimmed)) {
                return true;
            }
            if(referer != null && referer.startsWith(trimmed)) {
                return true;
            }
        }
        return  false;
    }


    private void sendUnauthorized(HttpServletResponse response)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.getWriter().write(
                "{\"success\":false," +
                        "\"errorCode\":\"" + ErrorCode.AUTH_001 + "\"," +
                        "\"message\":\"Invalid API key\"}"
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();
        return path.startsWith("/internal/")
                || path.equals("/actuator/health")
                // Public auth endpoints - không cần X-API-KEY
                // vì browser/FE không có key này
                || path.equals("/auth/login")
                || path.equals("/auth/exchange")     // V8: Keycloak exchange
                || path.equals("/auth/public-key")
                || path.equals("/auth/register")
                || path.equals("/auth/forgot-password")
                || path.equals("/auth/reset-password")
                || path.equals("/auth/logout")
                || path.equals("/auth/2fa/verify");
    }
}



