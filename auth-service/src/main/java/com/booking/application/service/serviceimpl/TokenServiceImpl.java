package com.booking.application.service.serviceimpl;

import com.booking.application.port.out.TokenRepositoryPort;
import com.booking.application.service.JwtService;
import com.booking.application.service.SystemParamService;
import com.booking.application.service.TokenService;
import com.booking.domain.model.Token;
import com.booking.domain.model.User;
import com.booking.infrastructure.external.cache.TokenCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final TokenRepositoryPort tokenRepositoryPort;
    private final TokenCacheService tokenCacheService;
    private final JwtService jwtService;

    // Inject SystemParamService để lấy TTL động
    // thay vì hardcode 720 giờ
    private final SystemParamService systemParamService;

    @Override
    public String createToken(User user,
                              String ipAddress,
                              String userAgent) {
        // Bước 1: Tạo JWT
        String jwt = jwtService.generateToken(user);

        // Bước 2: Hash JWT để lưu DB
        // → không lưu raw JWT vào DB vì lý do bảo mật
        // → nếu DB bị hack, attacker không lấy được token thật
        String tokenHash = jwtService.hashToken(jwt);

        // Bước 3: Lấy jti từ JWT
        // → jti = UUID duy nhất nhúng trong JWT khi tạo
        // → dùng để blacklist sau này
        String jti = jwtService.extractJti(jwt);

        // Bước 4: Lưu vào DB
        Token tokenEntity = new Token();
        tokenEntity.setUser(user);
        tokenEntity.setTokenHash(tokenHash);
        tokenEntity.setJti(jti);
        tokenEntity.setIpAddress(ipAddress);
        tokenEntity.setUserAgent(userAgent);
        tokenRepositoryPort.save(tokenEntity);

        log.info("[Token] Create token for user: {}", user.getUsername());

        // Bước 5: Lấy TTL từ SystemParam (dynamic)
        // → nếu key không tồn tại thì dùng default 720
        // → admin có thể update TOKEN_TTL_HOURS qua API
        //   mà không cần restart app
        long ttlHours = systemParamService
                .getIntValue("TOKEN_TTL_HOURS", 720);

        // Bước 6: Lưu vào Redis với TTL động
        // Key: "token:{userId}" → Value: raw JWT
        tokenCacheService.saveToken(
                user.getId().toString(),
                jwt,
                ttlHours
        );

        // Bước 7: Trả JWT cho FE
        return jwt;
    }

    @Override
    public String hashToken(String token) {
        return jwtService.hashToken(token);
    }

    @Override
    public boolean validateToken(String token) {

        // Bước 1: Verify JWT signature
        // → kiểm tra token có bị giả mạo không
        if (!jwtService.validateToken(token)) {
            log.warn("[Token] Invalid token signature");
            return false;
        }

        // Bước 2: Lấy JTI từ token
        // → JTI = UUID duy nhất của token này
        String jti = jwtService.extractJti(token);

        // Bước 3: Check blacklist trong Redis
        // → nếu jti có trong blacklist → token đã bị revoke
        // → trả về false ngay, không cần check DB
        // → Redis check rất nhanh (O(1)) → không ảnh hưởng performance
        if (tokenCacheService.isBlacklisted(jti)) {
            log.warn("[Token] Token has been blacklisted, jti: {}", jti);
            return false;
        }

        // Bước 4: Check token có trong DB không
        // → đảm bảo token được tạo bởi hệ thống
        // → không bị giả mạo dù signature đúng
        String tokenHash = jwtService.hashToken(token);
        boolean isExist = tokenRepositoryPort
                .findByTokenHash(tokenHash)
                .isPresent();

        if (!isExist) {
            log.warn("[Token] Token not found in DB");
            return false;
        }

        return true;
    }
}