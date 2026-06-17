package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.ForgotPasswordUseCase;
import com.booking.application.port.out.UserRepositoryPort;
import com.booking.application.service.MailService;
import com.booking.application.service.OtpService;
import com.booking.domain.exception.ErrorCode;
import com.booking.domain.exception.UserException;
import com.booking.domain.model.User;
import com.booking.presentation.request.ForgotPasswordRequest;
import com.booking.shared.util.MaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class ForgotPasswordServiceImpl implements ForgotPasswordUseCase {

    private final UserRepositoryPort userRepo;
    private final OtpService otpService;
    private final MailService mailService;

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        // Bước 1: Lấy email từ request
        String email = request.getEmail();
        log.info("[ForgotPassword] Request: {}", MaskUtil.maskEmail(email));

        // Bước 2: Tìm user theo email
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[ForgotPassword] Email not found: {}", MaskUtil.maskEmail(email));
                    return new UserException(ErrorCode.USR_001, ErrorCode.USR_001_MSG);
                });

        // Bước 3: Generate OTP
        String otp = otpService.generateAndStore(email, "FORGOT_PASSWORD");
        log.debug("[ForgotPassword] Generated OTP for {}", MaskUtil.maskEmail(email));

        // Bước 4: Gửi mail
        mailService.sendOtp(email, otp, "FORGOT_PASSWORD");
        log.info("[ForgotPassword] OTP sent to {}", MaskUtil.maskEmail(email));

    }
}
