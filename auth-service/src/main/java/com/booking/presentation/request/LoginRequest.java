package com.booking.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * LoginRequest
 * ─────────────────────────────────────────────────────────
 * FE có 2 cách gửi password:
 *
 * 1. Plain (legacy/dev) — không khuyến nghị:
 *    { "username": "admin", "password": "Admin@2024" }
 *
 * 2. RSA-OAEP-256 (khuyến nghị):
 *    { "username": "admin", "encryptedPassword": "base64..." }
 *
 * Nếu cả 2 đều có → ưu tiên encryptedPassword.
 * Nếu cả 2 đều null → 400 Bad Request.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 100)
    private String username;

    /**
     * Plain password — dùng cho dev/test
     * Trong prod nên để null, FE sẽ gửi encryptedPassword.
     */
    private String password;

    /**
     * Base64 của RSA-OAEP-256 ciphertext (FE encrypt bằng public key).
     * Server sẽ decrypt trước khi gọi LoginUseCase.
     */
    private String encryptedPassword;

    private String totpCode; // Optional for 2FA
}
