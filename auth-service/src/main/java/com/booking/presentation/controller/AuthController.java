package com.booking.presentation.controller;

import com.booking.application.port.in.*;
import com.booking.application.service.JwtService;
import com.booking.application.service.TwoFactorService;
import com.booking.shared.util.MaskUtil;
import com.booking.presentation.request.*;
import com.booking.presentation.response.LoginResponse;
import com.booking.presentation.response.RegisterResponse;
import com.booking.presentation.response.TwoFactorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Log4j2
public class AuthController {
    // ── Inject Use Case thay vì AuthService ───────────────────────
    // Tại sao?
    // → Controller chỉ biết đúng Use Case nó cần
    // → Không phụ thuộc vào toàn bộ AuthService
    // → Đúng Interface Segregation Principle (chữ I trong SOLID)
    // → Dễ test: mock từng Use Case riêng lẻ
    // → Dễ thay implementation: đổi bean implement LoginUseCase
    //   mà không cần sửa Controller
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final RegisterUseCase registerUseCase;
    private final Verify2faUseCase verify2faUseCase;
    private final Manage2faUseCase manage2faUseCase;

    // TwoFactorService và JwtService giữ nguyên
    // → Chưa có Use Case riêng cho 2FA setup/enable/disable
    private final JwtService jwtService;

    /**
     * POST /api/auth/login
     * FE gửi username + password
     * BE trả token + thông tin user
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login
    (@Valid @RequestBody LoginRequest request,
     HttpServletRequest httpServletRequest) {
        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");

        log.info("[CONTROLLER] Login attempt: {}",
                MaskUtil.maskUsername(request.getUsername()));

        LoginResponse response = loginUseCase.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);

    }
    /**
     * POST /api/auth/logout
     * FE gửi token trong Authorization header
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader) {

        String rawToken = authHeader.substring(7);
        log.info("[CONTROLLER] Logout request");
        logoutUseCase.logout(rawToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity register(
            @Valid @RequestBody RegisterRequest request){
        log.info("[CONTROLLER] Register attempt: {}",
                MaskUtil.maskUsername(request.getUsername()));
        RegisterResponse response = registerUseCase.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);

    }


    /**
     * POST /api/auth/forgot-password
     * FE gửi email → BE gửi link reset qua email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity <Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ){
        log.info("[CONTROLLER] Forgot password: {}",
                MaskUtil.maskEmail(request.getEmail()));

        forgotPasswordUseCase.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/auth/reset-password
     * FE gửi token + password mới → BE đổi password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ){
        log.info("[CONTROLLER] Reset password request");

        resetPasswordUseCase.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Lấy IP thật của user
     * Xử lý trường hợp đứng sau proxy
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * POST /api/auth/2fa/setup
     * Tạo QR code để user quét
     */
    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFactorResponse> setup2fa(
            @RequestHeader("Authorization") String authHeader){
        String jwt = authHeader.substring(7);
        String username = jwtService.extractUsername(jwt);

        log.info("[CONTROLLER] 2FA setup: {}",
                MaskUtil.maskUsername(username));

        TwoFactorResponse response =
                manage2faUseCase.setup(username);

        return  ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/2fa/enable
     * Xác nhận OTP và bật 2FA
     */
    @PostMapping("/2fa/enable")
    public ResponseEntity<Void> enable2fa(
            @RequestHeader("Authorization") String authHeader,
            @Valid@RequestBody TwoFactorRequest request){

        String jwt = authHeader.substring(7);
        String username = jwtService.extractUsername(jwt);

        log.info("[CONTROLLER] 2FA enable: {}",
                MaskUtil.maskUsername(username));

        manage2faUseCase.enable(username, request.getOtp());

        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/auth/2fa/disable
     * Tắt 2FA
     */
    @PostMapping("/2fa/disable")
    public ResponseEntity<Void> disable2fa(
            @RequestHeader("Authorization") String authHeader) {
        String jwt = authHeader.substring(7);
        String username = jwtService.extractUsername(jwt);

        log.info("[CONTROLLER] 2FA disable: {}",
                MaskUtil.maskUsername(username));

        manage2faUseCase.disable(username);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/auth/2fa/verify
     * Verify OTP sau khi login
     */
    @PostMapping("/2fa/verify")
    public ResponseEntity<LoginResponse> verify2fa(
            @Valid @RequestBody TwoFactorRequest request,
            HttpServletRequest httpServletRequest) {

        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");

        log.info("[CONTROLLER] 2FA verify request");

        LoginResponse response = verify2faUseCase.verify2fa
                (request, ipAddress, userAgent);
        return  ResponseEntity.ok(response);
    }




}
