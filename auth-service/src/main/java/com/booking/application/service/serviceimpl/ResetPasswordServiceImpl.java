package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.ResetPasswordUseCase;
import com.booking.application.port.out.KeycloakAdminPort;
import com.booking.application.port.out.TokenRepositoryPort;
import com.booking.application.port.out.UserKcLinkRepositoryPort;
import com.booking.application.port.out.UserRepositoryPort;
import com.booking.application.service.OtpService;
import com.booking.application.service.PasswordService;
import com.booking.domain.enums.DeactivationReason;
import com.booking.domain.exception.AuthException;
import com.booking.domain.exception.ErrorCode;
import com.booking.domain.model.User;
import com.booking.presentation.request.ResetPasswordRequest;
import com.booking.shared.util.MaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class ResetPasswordServiceImpl implements ResetPasswordUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordService passwordService;
    private final OtpService otpService;
    private final TokenRepositoryPort tokenRepositoryPort;
    private final KeycloakAdminPort kcAdminClient;         // ← MỚI
    private final UserKcLinkRepositoryPort kcLinkRepo;     // ← MỚI

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        String otp = request.getOtp();
        String newPassword = request.getNewPassword();

        log.info("[ResetPassword] Attempt for {}", MaskUtil.maskEmail(email));

        if (!otpService.verify(email, "FORGOT_PASSWORD", otp)) {
            log.warn("[ResetPassword] Invalid OTP for {}", MaskUtil.maskEmail(email));
            throw new AuthException(ErrorCode.AUTH_005, ErrorCode.AUTH_005_MSG);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(
                        ErrorCode.USR_001, ErrorCode.USR_001_MSG));

        var hashedPassword = passwordService.hash(
                newPassword, user.getUsername(), user.getPasswordSalt()
        );

        user.setPasswordHash(hashedPassword.hash());
        user.setPasswordSalt(hashedPassword.salt());
        userRepository.save(user);

        // Kill all refresh tokens
        int killed = tokenRepositoryPort.deactivateAllByUserId(
                user.getId(),
                DeactivationReason.PASSWORD_CHANGE.name()
        );
        log.info("[ResetPassword] Deactivated {} token(s) for {}",
                killed, MaskUtil.maskEmail(email));

        // ═══ MỚI — Sync KC (non-blocking) ═══
        kcLinkRepo.findByUserId(user.getId()).ifPresent(link -> {
            try {
                kcAdminClient.resetPassword(link.getKcUserId(), newPassword);
                log.info("[ResetPassword] KC password synced: kcUserId={}", link.getKcUserId());
            } catch (Exception e) {
                log.warn("[ResetPassword] KC sync failed (non-blocking): {}", e.getMessage());
            }
        });

        log.info("[ResetPassword] Success for {}", MaskUtil.maskEmail(email));
    }
}