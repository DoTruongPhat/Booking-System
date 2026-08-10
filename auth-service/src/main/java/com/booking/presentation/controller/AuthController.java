package com.booking.presentation.controller;

import com.booking.application.port.in.*;
import com.booking.application.service.DecryptPasswordService;
import com.booking.application.service.JwtService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Log4j2
public class AuthController {

 private final LoginUseCase loginUseCase;
 private final LogoutUseCase logoutUseCase;
 private final ForgotPasswordUseCase forgotPasswordUseCase;
 private final ResetPasswordUseCase resetPasswordUseCase;
 private final RegisterUseCase registerUseCase;
 private final Verify2faUseCase verify2faUseCase;
 private final Manage2faUseCase manage2faUseCase;
 private final ExchangeKeycloakCodeUseCase exchangeKeycloakCodeUseCase;
 private final GetPublicKeyUseCase getPublicKeyUseCase;
 private final RefreshTokenUseCase refreshTokenUseCase;
 private final SetPasswordUseCase setPasswordUseCase;

 private final JwtService jwtService;
 private final DecryptPasswordService decryptPasswordService;
 private final com.booking.application.port.out.UserRepositoryPort userRepositoryPort;

 private final StringRedisTemplate redisTemplate;

 private static final String STATE_PREFIX = "oauth:state:";
 private static final Duration STATE_TTL = Duration.ofMinutes(5);

 @Value("${app.bff.fe-redirect-url:http://localhost:4200/dashboard}")
 private String feRedirectUrl;

 @Value("${app.bff.fe-error-url:http://localhost:4200/login?error=sso_failed}")
 private String feErrorUrl;

 // ═══════════════════════════════════════════════════════════
 // BFF SSO
 // ═══════════════════════════════════════════════════════════

 @GetMapping("/sso/login")
 public ResponseEntity<Void> ssoLogin(
         @RequestParam(required = false) String provider) {
  String state = UUID.randomUUID().toString();
  redisTemplate.opsForValue().set(STATE_PREFIX + state, "PENDING", STATE_TTL);

  String authUrl = exchangeKeycloakCodeUseCase.buildAuthorizationUrl(state, provider);
  log.info("[BFF] Redirecting to KC login, state={}", state);

  return ResponseEntity.status(HttpStatus.FOUND)
          .location(URI.create(authUrl))
          .build();
 }

 @GetMapping("/sso/callback")
 public ResponseEntity<Void> ssoCallback(
         @RequestParam("code") String code,
         @RequestParam("state") String state,
         HttpServletRequest httpServletRequest) {

  log.info("[BFF] Callback received, state={}", state);

  Boolean deleted = redisTemplate.delete(STATE_PREFIX + state);
  if (!Boolean.TRUE.equals(deleted)) {
   log.warn("[BFF] Invalid or expired state: {}", state);
   return ResponseEntity.status(HttpStatus.FOUND)
           .location(URI.create(feErrorUrl))
           .build();
  }

  try {
   String ipAddress = getClientIp(httpServletRequest);
   String userAgent = httpServletRequest.getHeader("User-Agent");

   LoginResponse response = exchangeKeycloakCodeUseCase.handleCallback(
           code, ipAddress, userAgent);

   String redirectUrl;
   if (response.isPasswordRequired()) {
    redirectUrl = "http://localhost:4200/auth/complete-profile";
   } else if (response.getRoles() != null && response.getRoles().stream()
           .anyMatch(r -> List.of("ADMIN_ALL", "ADMIN", "HOST").contains(r))) {
    redirectUrl = "http://localhost:4200/admin/dashboard";
   } else {
    redirectUrl = feRedirectUrl;
   }

   log.info("[BFF] Login success, redirecting to: {}", redirectUrl);

   return ResponseEntity.status(HttpStatus.FOUND)
           .header(HttpHeaders.SET_COOKIE,
                   buildAccessCookie(response.getToken()).toString(),
                   buildRefreshCookie(response.getRefreshToken()).toString())
           .location(URI.create(redirectUrl))
           .build();

  } catch (Exception e) {
   log.error("[BFF] Callback failed: {}", e.getMessage(), e);
   return ResponseEntity.status(HttpStatus.FOUND)
           .location(URI.create(feErrorUrl))
           .build();
  }
 }

 // ═══════════════════════════════════════════════════════════
 // LOCAL LOGIN + AUTH
 // ═══════════════════════════════════════════════════════════

 @GetMapping("/public-key")
 public ResponseEntity<JweCryptoService.PublicKeyInfo> getPublicKey() {
  log.info("[CONTROLLER] Public key requested");
  return ResponseEntity.ok(getPublicKeyUseCase.get());
 }

 @PostMapping("/login")
 public ResponseEntity<LoginResponse> login(
         @Valid @RequestBody LoginRequest request,
         HttpServletRequest httpServletRequest) {

  String plainPassword = resolvePassword(request);
  if (plainPassword == null || plainPassword.isBlank()) {
   throw new IllegalArgumentException("Password must not be blank");
  }
  request.setPassword(plainPassword);
  request.setEncryptedPassword(null);

  String ipAddress = getClientIp(httpServletRequest);
  String userAgent = httpServletRequest.getHeader("User-Agent");

  log.info("[CONTROLLER] Login attempt: {}", MaskUtil.maskUsername(request.getUsername()));

  LoginResponse response = loginUseCase.login(request, ipAddress, userAgent);

  if (response.isTwoFactorRequired()) {
   return ResponseEntity.ok(response);
  }

  return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE,
                  buildAccessCookie(response.getToken()).toString(),
                  buildRefreshCookie(response.getRefreshToken()).toString())
          .body(response);
 }

 @PostMapping("/exchange")
 public ResponseEntity<LoginResponse> exchange(
         @Valid @RequestBody ExchangeCodeRequest request,
         HttpServletRequest httpServletRequest) {

  String ipAddress = getClientIp(httpServletRequest);
  String userAgent = httpServletRequest.getHeader("User-Agent");

  log.info("[CONTROLLER] Keycloak exchange from IP: {}", ipAddress);

  LoginResponse response = exchangeKeycloakCodeUseCase.exchange(
          request, ipAddress, userAgent);

  return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE,
                  buildAccessCookie(response.getToken()).toString(),
                  buildRefreshCookie(response.getRefreshToken()).toString())
          .body(response);
 }

 @PostMapping("/refresh")
 public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
  String refreshToken = extractCookie(request, "refresh_token");

  if (refreshToken == null || refreshToken.isBlank()) {
   log.warn("[CONTROLLER] Refresh failed: no refresh_token cookie");
   return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }

  String ipAddress = getClientIp(request);
  String userAgent = request.getHeader("User-Agent");

  LoginResponse response = refreshTokenUseCase.refresh(refreshToken, ipAddress, userAgent);

  return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE,
                  buildAccessCookie(response.getToken()).toString(),
                  buildRefreshCookie(response.getRefreshToken()).toString())
          .body(response);
 }

 @PostMapping("/logout")
 public ResponseEntity<Void> logout(HttpServletRequest request) {
  String rawToken = extractToken(request);

  if (rawToken != null) {
   logoutUseCase.logout(rawToken);
  }

  return ResponseEntity.noContent()
          .header(HttpHeaders.SET_COOKIE,
                  deleteCookie("access_token").toString(),
                  deleteCookie("refresh_token").toString())
          .build();
 }

 @PostMapping("/register")
 public ResponseEntity<RegisterResponse> register(
         @Valid @RequestBody RegisterRequest request) {
  log.info("[CONTROLLER] Register attempt: {}", MaskUtil.maskUsername(request.getUsername()));
  RegisterResponse response = registerUseCase.register(request);
  return ResponseEntity.status(HttpStatus.CREATED).body(response);
 }

 @PostMapping("/forgot-password")
 public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
  log.info("[CONTROLLER] Forgot password: {}", MaskUtil.maskEmail(request.getEmail()));
  forgotPasswordUseCase.forgotPassword(request);
  return ResponseEntity.ok().build();
 }

 @PostMapping("/reset-password")
 public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
  log.info("[CONTROLLER] Reset password request");
  resetPasswordUseCase.resetPassword(request);
  return ResponseEntity.ok().build();
 }

 // ═══════════════════════════════════════════════════════════
 // 2FA + COMPLETE PROFILE
 // ═══════════════════════════════════════════════════════════

 @PostMapping("/2fa/setup")
 public ResponseEntity<TwoFactorResponse> setup2fa(HttpServletRequest request) {
  String username = jwtService.extractUsername(extractToken(request));
  TwoFactorResponse response = manage2faUseCase.setup(username);
  return ResponseEntity.ok(response);
 }

 @PostMapping("/2fa/enable")
 public ResponseEntity<Void> enable2fa(
         @Valid @RequestBody TwoFactorRequest request,
         HttpServletRequest http) {
  String username = jwtService.extractUsername(extractToken(http));
  manage2faUseCase.enable(username, request.getOtp());
  return ResponseEntity.ok().build();
 }

 @PostMapping("/2fa/disable")
 public ResponseEntity<Void> disable2fa(HttpServletRequest http) {
  String username = jwtService.extractUsername(extractToken(http));
  manage2faUseCase.disable(username);
  return ResponseEntity.ok().build();
 }

 @PostMapping("/2fa/verify")
 public ResponseEntity<LoginResponse> verify2fa(
         @Valid @RequestBody TwoFactorRequest request,
         HttpServletRequest httpServletRequest) {

  String ipAddress = getClientIp(httpServletRequest);
  String userAgent = httpServletRequest.getHeader("User-Agent");

  LoginResponse response = verify2faUseCase.verify2fa(request, ipAddress, userAgent);

  return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE,
                  buildAccessCookie(response.getToken()).toString(),
                  buildRefreshCookie(response.getRefreshToken()).toString())
          .body(response);
 }

 @PostMapping("/complete-profile")
 public ResponseEntity<Void> completeProfile(
         @Valid @RequestBody SetPasswordRequest request,
         HttpServletRequest httpServletRequest) {
  String token = extractToken(httpServletRequest);
  String username = jwtService.extractUsername(token);
  log.info("[CONTROLLER] Complete profile: {}", MaskUtil.maskUsername(username));
  setPasswordUseCase.setPassword(username, request.getUsername(), request.getNewPassword());
  return ResponseEntity.ok().build();
 }

 // ═══════════════════════════════════════════════════════════
 // HELPER METHODS
 // ═══════════════════════════════════════════════════════════

 private ResponseCookie buildAccessCookie(String token) {
  return ResponseCookie.from("access_token", token)
          .httpOnly(true)
          .secure(false)
          .path("/")
          .maxAge(Duration.ofHours(1))
          .build();
 }

 private ResponseCookie buildRefreshCookie(String token) {
  return ResponseCookie.from("refresh_token", token)
          .httpOnly(true)
          .secure(false)
          .path("/")
          .maxAge(Duration.ofDays(7))
          .build();
 }

 private ResponseCookie deleteCookie(String name) {
  return ResponseCookie.from(name, "")
          .httpOnly(true)
          .secure(false)
          .path("/")
          .maxAge(0)
          .build();
 }

 private String extractCookie(HttpServletRequest request, String name) {
  Cookie[] cookies = request.getCookies();
  if (cookies != null) {
   for (Cookie c : cookies) {
    if (name.equals(c.getName())) {
     return c.getValue();
    }
   }
  }
  return null;
 }

 private String extractToken(HttpServletRequest request) {
  String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
  if (authHeader != null && authHeader.startsWith("Bearer ")) {
   return authHeader.substring(7);
  }
  return extractCookie(request, "access_token");
 }

 private String getClientIp(HttpServletRequest request) {
  String xff = request.getHeader("X-Forwarded-For");
  if (xff != null && !xff.isBlank()) {
   return xff.split(",")[0].trim();
  }
  return request.getRemoteAddr();
 }

 private String resolvePassword(LoginRequest request) {
  if (request.getEncryptedPassword() != null
          && !request.getEncryptedPassword().isBlank()) {
   return decryptPasswordService.decrypt(request.getEncryptedPassword());
  }
  return request.getPassword();
 }
}
