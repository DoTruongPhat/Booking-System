package com.booking.infrastructure.security.filter;

import com.booking.application.port.out.TokenRepositoryPort;
import com.booking.application.service.JwtService;
import com.booking.application.service.SessionService;
import com.booking.domain.exception.ErrorCode;
import com.booking.domain.model.UserSession;
import com.booking.infrastructure.external.cache.TokenCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
@Order(2)
public class TokenAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenRepositoryPort tokenRepositoryPort;
    private final TokenCacheService tokenCacheService;
    private final SessionService sessionService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Bước 1: Lấy JWT từ Authorization header
        // → Format: "Bearer eyJhbGci..."
        // → Không có header hoặc không đúng format → cho đi tiếp
        //   (sẽ bị chặn bởi Spring Security nếu endpoint cần auth)
        String jwt = extractToken(request);

        if (jwt == null || jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Bước 2: Verify JWT signature
            // → Đảm bảo token không bị giả mạo
            // → Sai signature → 401 ngay, không check tiếp
            if (!jwtService.validateToken(jwt)) {
                log.warn("[Token] Invalid JWT signature from IP: {}",
                        request.getRemoteAddr());
                sendUnauthorized(response);
                return;
            }

            // Bước 3: Extract thông tin từ JWT
            // → username: để log và set SecurityContext
            // → userId: để tìm cache Redis
            // → jti: để check blacklist
            String username = jwtService.extractUsername(jwt);
            String userId = jwtService.extractUserId(jwt);
            String jti = jwtService.extractJti(jwt);

            // Bước 3.5: Check session còn active trong DB (Single Session)
            // → Nếu session đã bị kill (login ở nơi khác) → 401
            // → Đây là điểm khác biệt của single session:
            //   JWT signature vẫn valid NHƯNG session đã bị invalidate
            var activeSession = sessionService.verifyActive(jti);
            if (activeSession.isEmpty()) {
                log.info("[Token] Session not active for jti: {}, user: {} (probably killed by new login)",
                    jti, username);
                sendSessionKilled(response);
                return;
            }

            // Bước 4: Check blacklist trước (quan trọng!)
            // → Admin revoke → jti bị blacklist trong Redis
            // → Check Redis: O(1) rất nhanh
            // → Có trong blacklist → 401 ngay lập tức
            // → Không check DB → tiết kiệm resource
            if (tokenCacheService.isBlacklisted(jti)) {
                log.warn("[Token] Token blacklisted, jti: {}, user: {}",
                        jti, username);
                sendUnauthorized(response);
                return;
            }

            // Bước 5: Check token có hợp lệ không
            // → Ưu tiên Redis cache (nhanh)
            // → Cache miss → check DB (chậm hơn nhưng chính xác)
            boolean tokenValid = false;

            String cachedJwt = tokenCacheService.getToken(userId);
            if (cachedJwt != null && cachedJwt.equals(jwt)) {
                // Cache hit → token hợp lệ
                // → Không cần query DB → nhanh hơn
                tokenValid = true;
                log.debug("[Token] Cache hit for user: {}", username);
            } else {
                // Cache miss → query DB
                // → Hash token rồi tìm trong DB
                // → isPresent() = token tồn tại và active
                String tokenHash = jwtService.hashToken(jwt);
                tokenValid = tokenRepositoryPort
                        .findByTokenHash(tokenHash)
                        .isPresent();
                if (tokenValid) {
                    log.debug("[Token] DB hit for user: {}", username);
                }
            }

            // Bước 6: Token không hợp lệ → 401
            if (!tokenValid) {
                log.warn("[Token] Token revoked for user: {}", username);
                sendUnauthorized(response);
                return;
            }

            // Bước 7: Build danh sách authorities
            // → authorities = roles + permissions
            // → Spring Security dùng authorities để check @PreAuthorize
            List<GrantedAuthority> authorities = new ArrayList<>();

            // 7a: Thêm roles với prefix "ROLE_"
            // → "ADMIN" → "ROLE_ADMIN"
            // → hasRole('ADMIN') sẽ tìm "ROLE_ADMIN" trong authorities
            List<String> roles = jwtService.extractRoles(jwt);
            roles.stream()
                    .map(role -> new SimpleGrantedAuthority(
                            "ROLE_" + role.trim()))
                    .forEach(authorities::add);

            // 7b: Thêm permissions KHÔNG có prefix
            // → "ADMIN_ALL" → "ADMIN_ALL" (giữ nguyên)
            // → hasAuthority('ADMIN_ALL') tìm chính xác "ADMIN_ALL"
            // → Phân biệt với role: hasRole dùng prefix ROLE_
            //                       hasAuthority dùng chính xác
            List<String> permissions = jwtService.extractPermissions(jwt);
            permissions.stream()
                    .map(permission -> new SimpleGrantedAuthority(
                            permission.trim()))
                    .forEach(authorities::add);

            log.debug("[Token] User: {} has authorities: {}",
                    username, authorities);

            // Bước 8: Set SecurityContext
            // → Spring Security biết request này là của ai
            // → Có quyền gì (roles + permissions)
            // → @PreAuthorize sẽ check authorities này
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("[Token] Authenticated user: {}", username);

            // Bước 9: Cho request đi tiếp vào Controller
            filterChain.doFilter(request, response);

        } finally {
            // Bước 10: Xóa SecurityContext sau khi request xử lý xong
            // → Tránh leak thông tin giữa các request
            // → Mỗi request phải authenticate lại từ đầu
            SecurityContextHolder.clearContext();
        }
    }

    private void sendUnauthorized(HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false," +
                        "\"errorCode\":\"" + ErrorCode.AUTH_003 + "\"," +
                        "\"message\":\"" + ErrorCode.AUTH_003_MSG + "\"}");
    }

    /**
     * 401 cho trường hợp session bị kill (login ở nơi khác)
     * FE sẽ hiển thị toast warning
     */
    private void sendSessionKilled(HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false," +
                        "\"errorCode\":\"SESSION_KILLED\"," +
                        "\"message\":\"Your session was ended because you logged in on another device\"}");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/exchange")
                || path.equals("/api/auth/register")
                || path.equals("/auth/login")
                || path.equals("/auth/register")
                || path.startsWith("/api/internal/")
                || path.startsWith("/internal/")
                || path.equals("/actuator/health");
    }
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}