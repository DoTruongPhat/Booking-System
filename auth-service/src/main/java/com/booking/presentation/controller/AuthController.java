package com.booking.presentation.controller;

import com.booking.application.port.in.*;
import com.booking.application.service.DecryptPasswordService;
import com.booking.application.service.JwtService;
import com.booking.application.service.TwoFactorService;
import com.booking.infrastructure.crypto.JweCryptoService;
import com.booking.shared.util.MaskUtil;
import com.booking.presentation.request.*;
import com.booking.presentation.response.LoginResponse;
import com.booking.presentation.response.RegisterResponse;
import com.booking.presentation.response.TwoFactorResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

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
    private final ExchangeKeycloakCodeUseCase exchangeKeycloakCodeUseCase;
    private final GetPublicKeyUseCase getPublicKeyUseCase;

    // TwoFactorService và JwtService giữ nguyên
    // → Chưa có Use Case riêng cho 2FA setup/enable/disable
    private final JwtService jwtService;
    private final DecryptPasswordService decryptPasswordService;

    /**
     * GET /api/auth/public-key
     * FE lấy public key để encrypt password trước khi gửi.
     * Public route — không cần auth.
     */
    @GetMapping("/public-key")
    public ResponseEntity<JweCryptoService.PublicKeyInfo> getPublicKey() {
        log.info("[CONTROLLER] Public key requested");
        return ResponseEntity.ok(getPublicKeyUseCase.get());
    }

    /**
     * POST /api/auth/login
     * FE gửi username + (encryptedPassword HOẶC password)
     * BE decrypt → trả cookie HttpOnly (body không chứa token)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login
    (@Valid @RequestBody LoginRequest request,
     HttpServletRequest httpServletRequest) {

        // ── Step 1: Decrypt JWE password ──
        String plainPassword = resolvePassword(request);

        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "Password must not be blank");
        }

        // Ghi đè request bằng password đã decrypt
        request.setPassword(plainPassword);
        request.setEncryptedPassword(null);

        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");

        log.info("[CONTROLLER] Login attempt: {}",
                MaskUtil.maskUsername(request.getUsername()));

        LoginResponse response = loginUseCase.login(request, ipAddress, userAgent);

        if (response.isTwoFactorRequired()) {
            return ResponseEntity.ok(response);
        }

        // ── Step 2: Set HttpOnly cookie, KHÔNG trả token trong body ──
        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", response.getToken())
                .httpOnly(true)
                .secure(false) // dev localhost
                .path("/")
                .maxAge(Duration.ofHours(1))
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .body(response);
    }

    /**
     * Resolve password từ request:
     * - Ưu tiên encryptedPassword (JWE) — decrypt bằng private key
     * - Fallback password plain (dev/test)
     */
    private String resolvePassword(LoginRequest request) {
        if (request.getEncryptedPassword() != null
                && !request.getEncryptedPassword().isBlank()) {
            log.debug("[CONTROLLER] Decrypting JWE password");
            return decryptPasswordService.decrypt(
                    request.getEncryptedPassword());
        }
        return request.getPassword();
    }

    /**
     * POST /api/auth/exchange
     * Form B (Keycloak) login:
     * FE gửi code + code_verifier (PKCE) → BE exchange với KC → cấp JWT nội bộ
     * Public route — không cần auth (là bước đầu của login flow)
     */
    @PostMapping("/exchange")
    public ResponseEntity<LoginResponse> exchange(
            @Valid @RequestBody ExchangeCodeRequest request,
            HttpServletRequest httpServletRequest) {

        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");

        log.info("[CONTROLLER] Keycloak exchange from IP: {}", ipAddress);

        LoginResponse response = exchangeKeycloakCodeUseCase.exchange(
            request, ipAddress, userAgent
        );

        // Set HttpOnly cookie giống Form A
        ResponseCookie accessTokenCookie = ResponseCookie.from(
                "access_token", response.getToken())
            .httpOnly(true)
            .secure(false)  // dev localhost
            .path("/")
            .maxAge(Duration.ofHours(1))
            .sameSite("Lax")
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
            .body(response);
    }
    /**
     * POST /api/auth/logout
     * FE gửi token trong Authorization header
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String rawToken = extractToken(request);

        if (rawToken != null) {
            logoutUseCase.logout(rawToken);
        }

        ResponseCookie deleteCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
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

        LoginResponse response =
                verify2faUseCase.verify2fa(request, ipAddress, userAgent);

        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", response.getToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofHours(1))
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .body(response);
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
