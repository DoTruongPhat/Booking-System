package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.RevokeAllSessionsUseCase;
import com.booking.application.port.in.RevokeSessionUseCase;
import com.booking.application.port.out.TokenRepositoryPort;
import com.booking.application.service.JwtService;
import com.booking.application.service.SessionService;
import com.booking.domain.exception.ErrorCode;
import com.booking.domain.exception.TokenException;
import com.booking.domain.model.Token;
import com.booking.infrastructure.external.cache.TokenCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * SessionServiceImpl = xử lý revoke token
 *
 * Luồng dữ liệu khi revoke:
 * 1. Tìm token active của user trong DB → lấy JTI
 * 2. Blacklist JTI trong Redis → token bị chặn ngay lập tức
 * 3. Deactivate token trong DB → đánh dấu đã revoke
 * 4. Xóa cache Redis (token:{userId}) → user phải login lại
 *
 * Tại sao cần cả Redis VÀ DB?
 * → Redis: chặn nhanh (mỗi request check Redis trước)
 * → DB: lưu lịch sử (audit log, biết token bị revoke lúc nào)
 */

@Service
@RequiredArgsConstructor
@Log4j2
public class SessionServiceImpl implements
        SessionService,
        RevokeAllSessionsUseCase,
        RevokeSessionUseCase {

    // Cần TokenRepositoryPort để:
    // → Tìm token active của user (lấy JTI)
    // → Deactivate token trong DB
    private final TokenRepositoryPort tokenRepositoryPort;

    // Cần TokenCacheService để:
    // → Blacklist JTI trong Redis
    // → Xóa cache token của user
    private final TokenCacheService tokenCacheService;

    // Cần JwtService để:
    // → Tính TTL còn lại của token
    // → Dùng làm TTL của blacklist entry
    //   (blacklist không cần giữ lâu hơn token)
    private final JwtService jwtService;

    @Override
    @Transactional
    public void revokeAllSessions(UUID userId) {

        log.info("[Session] Revoking all sessions for user: {}", userId);

        // Bước 1: Tìm token active mới nhất của user trong DB
        // → Cần JTI để blacklist
        // → Nếu không có token active → user chưa login → bỏ qua
        tokenRepositoryPort
                .findActiveTokenByUserId(userId)
                .ifPresent(token -> {
                    // Bước 2: Lấy JTI từ token
                    // → JTI = UUID duy nhất nhúng trong JWT
                    // → Dùng để blacklist chính xác token này
                    String jti = token.getJti();


                    // Bước 3: Blacklist JTI trong Redis
                    // → TTL = 720 giờ (thời gian sống của token)
                    // → Khi token hết hạn tự nhiên thì
                    //   blacklist cũng tự xóa → Redis không bị đầy
                    tokenCacheService.blacklistToken(jti, 720);

                    log.info("[Session] Blacklisted jti: {}", jti);

                });

        // Bước 4: Deactivate tất cả token trong DB
        // → Đánh dấu isActive = false
        // → Lưu lý do = "ADMIN_REVOKE" để audit sau này
        int count =
                tokenRepositoryPort.deactivateAllByUserId(
                        userId,
                        "ADMIN_REVOKE"
                );

        log.info("[Session] Deactivated {} tokens in DB for user: {}",
                count, userId);

        // Bước 5: Xóa cache token trong Redis
        // → Key: "token:{userId}"
        // → Xóa để user không thể dùng cached token
        // → Lần sau gọi API → TokenAuthFilter không thấy cache
        //   → check DB → token đã deactivated → 401
        tokenCacheService.deleteToken(userId.toString());

        log.info("[Session] Cleared token cache for user: {}", userId);


    }

    @Override
    @Transactional
    public void revokeSession(String jti) {
        log.info("[Session] Revoking session with jti: {}", jti);

        // Bước 1: Tìm token theo JTI trong DB
        // → Cần lấy userId để xóa cache Redis
        // → Nếu không tìm thấy → JTI không hợp lệ → throw exception
        Token token = tokenRepositoryPort.findByJti(jti)
                .orElseThrow(() -> {
                    log.warn("[Session] JTI not found: {}", jti);
                    return new TokenException(
                            ErrorCode.TKN_003,
                            ErrorCode.TKN_003_MSG
                    );
                });

        // Bước 2: Blacklist JTI trong Redis
        // → Chặn token ngay lập tức kể từ request tiếp theo
        // → TTL = 720 giờ
        tokenCacheService.blacklistToken(jti, 720);

        log.info("[Session] Blacklisted jti: {}", jti);

        // Bước 3: Deactivate token trong DB
        // → Đánh dấu isActive = false
        // → Lưu lý do để audit sau này
        token.deactivate("ADMIN_REVOKE");
        tokenRepositoryPort.save(token);
        log.info("[Session] Deactivated token in DB, jti: {}", jti);

        // Bước 4: Xóa cache Redis của user
        // → Key: "token:{userId}"
        // → User phải login lại để lấy token mới
        tokenCacheService.deleteToken(token.getUser().getId().toString());
        log.info("[Session] Cleared token cache for user: {}",
                token.getUser().getId());
    }
}
