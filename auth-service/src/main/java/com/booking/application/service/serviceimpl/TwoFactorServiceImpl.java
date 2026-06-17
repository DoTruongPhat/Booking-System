package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.Manage2faUseCase;
import com.booking.application.port.in.Verify2faUseCase;
import com.booking.application.port.out.TokenRepositoryPort;
import com.booking.application.port.out.UserRepositoryPort;
import com.booking.application.service.JwtService;
import com.booking.application.service.SessionService;
import com.booking.application.service.TokenService;
import com.booking.application.service.TwoFactorService;
import com.booking.domain.exception.AuthException;
import com.booking.domain.exception.ErrorCode;
import com.booking.domain.exception.TokenException;
import com.booking.domain.exception.UserException;
import com.booking.domain.model.User;
import com.booking.infrastructure.external.cache.TokenCacheService;
import com.booking.presentation.request.TwoFactorRequest;
import com.booking.presentation.response.LoginResponse;
import com.booking.presentation.response.TwoFactorResponse;
import com.booking.shared.util.MaskUtil;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
@RequiredArgsConstructor
@Log4j2
public class TwoFactorServiceImpl implements
        TwoFactorService,
        Manage2faUseCase,
        Verify2faUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final TokenRepositoryPort tokenRepositoryPort;
    private final TokenService tokenService;
    private final TokenCacheService tokenCacheService;
    private final SessionService sessionService;
    private final JwtService jwtService;

    @Override
    public TwoFactorResponse setup(String username) {
        log.info("[2FA] Setup for user: {}", username);

        // Bước 1: Tạo secret key
        SecretGenerator generator = new DefaultSecretGenerator();
        String secret = generator.generate();

        // Bước 2: Tạo QR code data
        QrData qr = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer("Booking-System")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        // Bước 3: Generate QR code image
        String qrCodeUrl;
        try {
            QrGenerator qrGenerator = new ZxingPngQrGenerator();
            byte[] imageData = qrGenerator.generate(qr);
            qrCodeUrl = getDataUriForImage(
                    imageData,
                    qrGenerator.getImageMimeType()
            );
        }catch (Exception e){
            throw new RuntimeException("Failed to generate QR code", e);
        }

        // Bước 4: Lưu secret tạm vào user
        // (chưa enable, chờ user xác nhận OTP)
        User user = userRepositoryPort
                .findByUsername(username)
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001,
                        ErrorCode.USR_001_MSG
                ));
        user.setTotpSecret(secret);
        userRepositoryPort.save(user);

        log.info("[2FA] QR code generated for: {}", username);

        return TwoFactorResponse.builder()
                .qrCodeUrl(qrCodeUrl)
                .secret(secret)
                .message("Scan QR code with Google Authenticator")
                .build();
    }

    @Override
    @Transactional
    public void enable(String username, String otp) {
        log.info("[2FA] Enable for user: {}", username);

        User user = userRepositoryPort
                .findByUsername(username)
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001,
                        ErrorCode.USR_001_MSG
                ));
        // Verify OTP trước khi enable
        if(!verifyOtp(user.getTotpSecret(), otp)){
            throw new AuthException(
                    ErrorCode.AUTH_001,
                    "Invalid OTP");
        }
        user.setTwoFactorEnabled(true);
        userRepositoryPort.save(user);

        log.info("[2FA] Enabled for user: {}", username);

    }

    @Override
    @Transactional
    public void disable(String username) {
        log.info("[2FA] Disable for user: {}", username);

        User user = userRepositoryPort
                .findByUsername(username)
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001,
                        ErrorCode.USR_001_MSG
                ));

        user.setTwoFactorEnabled(false);
        user.setTotpSecret(null);
        userRepositoryPort.save(user);
        log.info("[2FA] Disabled for user: {}", username);
    }

    @Override
    public boolean verifyOtp(String otp, String secret) {

        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator =
                new DefaultCodeGenerator();
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(
                codeGenerator, timeProvider);

        // Cho phép lệch 1 chu kỳ (30 giây trước/sau)
        verifier.setAllowedTimePeriodDiscrepancy(1);

        return verifier.isValidCode(otp, secret);

    }

    @Override
    public String getSecret(String username) {
        User user = userRepositoryPort
                .findByUsername(username)
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001,
                        ErrorCode.USR_001_MSG
                ));
        return user.getTotpSecret();
    }

    @Override
    public LoginResponse verify2fa(TwoFactorRequest request,
                                    String ipAddress,
                                    String userAgent) {
        log.info("[2FA] Verify request");

        // Bước 1: Lấy userId từ MFA session token (Redis)
        String userId = tokenCacheService.getMfaSession(request.getSessionToken());
        if (userId == null) {
            throw new TokenException(ErrorCode.TKN_001, ErrorCode.TKN_001_MSG);
        }

        // Bước 2: Tìm user
        User user = userRepositoryPort
                .findByIdWithRoles(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001, ErrorCode.USR_001_MSG
                ));

        // Bước 3: Verify OTP từ Google Authenticator
        if (!verifyOtp(user.getTotpSecret(), request.getOtp())) {
            throw new AuthException(ErrorCode.AUTH_001, "Invalid OTP");
        }

        // Bước 4: Xóa MFA session
        tokenCacheService.deleteMfaSession(request.getSessionToken());

        // Bước 5: Single Session - kill old sessions
        int killed = sessionService.killOldSessions(
                user.getId(),
                com.booking.domain.model.UserSession.REASON_NEW_LOGIN
        );
        log.info("[2FA] Killed {} old session(s) for user {}",
                killed, MaskUtil.maskUsername(user.getUsername()));

        // Bước 6: Tạo session mới
        String jti = java.util.UUID.randomUUID().toString();
        int ttl = 3600;
        sessionService.createSession(
                user, jti,
                com.booking.domain.model.UserSession.SOURCE_LOCAL,
                "2FA / Desktop", ipAddress, userAgent, ttl
        );

        // Bước 7: Tạo JWT với jti match session
        String jwt = jwtService.generateToken(user, jti);

        log.info("[2FA] Verify successful: {} (jti: {})",
                MaskUtil.maskUsername(user.getUsername()), jti);

        return LoginResponse.builder()
                .token(jwt)
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(r -> r.getCode())
                        .toList())
                .timezone(user.getTimezone())
                .twoFactorRequired(false)
                .build();
    }

}
