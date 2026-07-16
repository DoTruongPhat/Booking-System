package com.booking.application.port.out;



import com.booking.domain.model.Token;

import java.util.Optional;
import java.util.UUID;

public interface TokenRepositoryPort {
    Token save(Token token);
    Optional<Token> findByTokenHash(String tokenHash);
    int deactivateAllByUserId(UUID userId, String reason);

    /**
     * Tìm token đang active của user
     * Dùng để lấy JTI khi admin muốn revoke
     *
     * @param userId ID của user cần revoke
     * @return Optional<Token> token đang active
     *         → có JTI để blacklist
     *         → empty nếu user chưa login hoặc đã logout
     */

    Optional<Token> findActiveTokenByUserId(UUID userId);

    /**
     * Tìm token theo JTI
     * JTI = JWT ID, UUID duy nhất nhúng trong JWT khi tạo
     *
     * Dùng khi: admin revoke 1 session cụ thể theo JTI
     * → Tìm được token → lấy userId → xóa cache Redis
     * → Deactivate trong DB → lưu lịch sử
     *
     * @param jti JWT ID cần tìm
     * @return Optional<Token> → empty nếu không tìm thấy
     */
    Optional<Token> findByJti(String jti);

 /**
 * Deactivate 1 token theo JTI (logout, revoke)
 * @return true nếu deactivate thành công
 */
 boolean deactivateByJti(String jti, String reason);
}
