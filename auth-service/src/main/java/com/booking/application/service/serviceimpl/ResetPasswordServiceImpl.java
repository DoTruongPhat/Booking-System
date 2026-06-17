package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.ResetPasswordUseCase;
import com.booking.application.port.out.UserRepositoryPort;
import com.booking.application.service.MailService;
import com.booking.application.service.OtpService;
import com.booking.application.service.PasswordService;
import com.booking.application.service.SessionService;
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
    private final SessionService sessionService;

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Bước 1: Lấy email, otp, newPassword từ request
        String email = request.getEmail();
        String otp = request.getOtp();
        String newPassword = request.getNewPassword();

        log.info("[ResetPassword] Attempt for {}", MaskUtil.maskEmail(email));

        // Bước 2: Verify OTP
        if(!otpService.verify(email, "FORGOT_PASSWORD", otp)) {
            log.warn("[ResetPassword] Invalid OTP for {}", MaskUtil.maskEmail(email));
            throw new AuthException(
                    ErrorCode.AUTH_005, ErrorCode.AUTH_005_MSG
            );
        }

        // Bước 3: Tìm user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(
                        ErrorCode.USR_001, ErrorCode.USR_001_MSG
                ));

        // Bước 4: Hash newPassword bằng Bcrypt trả về object chứa hash và salt
        var hashedPassword = passwordService.hash(
                newPassword, user.getUsername(), user.getPasswordSalt()
        );

        // Bước 5: Update password trong DB
        user.setPasswordHash(hashedPassword.hash());
        user.setPasswordSalt(hashedPassword.salt());
        userRepository.save(user);

        // Bước 6: Kill all sessions (bảo mật)
        sessionService.revokeAllSessions(user.getId());

        log.info("[ResetPassword] Success for {}", MaskUtil.maskEmail(email));
    }
}
