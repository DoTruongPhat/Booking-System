package com.booking.presentation.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
 private String username;
 private String email;
 private List<String> roles;

 /**
 * Access token - chỉ dùng để set cookie HttpOnly, KHÔNG trả trong body
 */
 @JsonIgnore
 private String token;

 /**
 * Refresh token - chỉ dùng để set cookie HttpOnly, KHÔNG trả trong body
 */
 @JsonIgnore
 private String refreshToken;

 private String timezone;
 private boolean twoFactorRequired;
 private String mfaSessionToken;

 /**
 * Phase 7: true khi user cần bổ sung phone (vd user mới sync từ Keycloak).
 */
 private boolean phoneRequired;
 private boolean passwordRequired;

 private String firstName;
 private String lastName;
}